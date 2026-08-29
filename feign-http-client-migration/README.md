# feign-http-client-migration

OpenFeign 만 추가하고 별도 HTTP 클라이언트를 지정하지 않으면 `feign.Client.Default` 가 쓰인다. 이 클래스는 내부적으로 `java.net.HttpURLConnection` 을 사용하는데, `PoolingHttpClientConnectionManager` 나 `okhttp3.ConnectionPool` 같은 **명시적으로 설정/관측 가능한 커넥션 풀이 없다**. idle 커넥션 캐시 크기가 `http.maxConnections` 시스템 프로퍼티(기본값 5)로 정해질 뿐, 동시 요청 수를 제한하거나 대기(backpressure)시키는 풀 추상화 자체가 없다.

이 모듈은 같은 백엔드(`/api/hello`)를 네 가지 클라이언트로 호출해서 그 차이를 눈으로 확인한다.

- `default` — `feign.Client.Default` (HttpURLConnection, 풀 없음)
- `hc5` — `feign-hc5` + `PoolingHttpClientConnectionManager`
- `okhttp` — `feign-okhttp` + `okhttp3.ConnectionPool`
- `http-interface` — Feign 대신 Spring 6 `RestClient` + `HttpServiceProxyFactory` (hc5와 동일한 풀 설정), 마이그레이션 목표 지점

## 실행

```bash
./gradlew :feign-http-client-migration:bootRun --args="<mode> [concurrency=20] [delayMs=100]"

./gradlew :feign-http-client-migration:bootRun --args="default 20 200"
./gradlew :feign-http-client-migration:bootRun --args="hc5 20 200"
./gradlew :feign-http-client-migration:bootRun --args="okhttp 20 200"
./gradlew :feign-http-client-migration:bootRun --args="http-interface 20 200"
```

같은 프로세스 안에서 백엔드(`BackendController`, 8090 포트)와 클라이언트가 함께 뜬다. `concurrency` 개 스레드가 동시에 `/api/hello?delayMs=<delayMs>` 를 호출하고, 전체 소요 시간과 클라이언트별 풀 통계를 로그로 남긴다.

## 볼 것

### 1. default 모드가 실제로 HttpURLConnection 을 쓰는지

`LoggingHttpUrlConnectionClient` 가 `feign.Client.Default#getConnection(URL)` 을 오버라이드해서 매 요청마다 실제 커넥션 구현체 클래스를 로그로 찍는다.

```
[default] connection impl=sun.net.www.protocol.http.HttpURLConnection http.maxConnections=5 (default)
```

`hc5`/`okhttp` 모드에서는 대신 `ApacheHttp5Client`/`OkHttpClient` 로그가 찍히고, 실행 종료 후 풀 통계가 출력된다.

```
[hc5] pool stats = [leased: 0; pending: 0; available: 20; max: 50]
[okhttp] connectionCount=20 idleConnectionCount=20
[http-interface] pool stats = [leased: 0; pending: 0; available: 20; max: 50]
```

`default` 모드는 이런 통계 자체가 없다. `printPoolStats` 가 "코드로 조회 가능한 풀 통계 없음"이라고 로그를 남기는 이유다.

### 2. 커넥션이 재사용되는지 (서버 로그)

`BackendController` 는 요청마다 `request.getRemotePort()` 를 로그로 남긴다. 같은 remote port 가 반복되면 커넥션이 재사용된 것이고, 매번 새 port 면 매 요청마다 새 TCP 커넥션이 열린 것이다.

**순차 호출(`seq`)** 은 두 클라이언트 모두 재사용된다. JDK 의 `HttpURLConnection` 도 요청 사이 간격이 짧으면 JVM 전역 `sun.net.www.http.KeepAliveCache` 로 재사용하기 때문이다.

```bash
./gradlew :feign-http-client-migration:bootRun --args="default 20 10 seq" 2>&1 | grep -oE "remotePort=[0-9]+" | sort -u | wc -l
# → 1 (20번 호출 모두 같은 remote port)
```

진짜 차이는 **동시 버스트를 연달아 두 번(`burst2`) 쐈을 때** 드러난다. 실측 결과(concurrency=20):

```bash
./gradlew :feign-http-client-migration:bootRun --args="default 20 50 burst2"
./gradlew :feign-http-client-migration:bootRun --args="hc5 20 50 burst2"
```

| 클라이언트 | round1 고유 port | round2 고유 port | round1∩round2(재사용된 개수) |
| --- | --- | --- | --- |
| `default` | 20 | 20 | **5** (`http.maxConnections` 기본값과 일치) |
| `hc5` | 20 | 20 | **20** (`maxTotal=50` 풀 안에서 전부 재사용) |

`default` 는 idle 캐시가 5개뿐이라 round1 에서 쓴 20개 커넥션 중 5개만 살아남고, round2 의 나머지 15개는 새 TCP 커넥션을 다시 맺는다. `hc5` 는 풀이 `maxTotal=50` 이라 20개 전부 재사용한다. `DefaultClientConnectionReuseTests` 가 이 동작을 `/api/debug/ports` 엔드포인트로 자동 검증한다.

### 3. 외부에서 TCP 커넥션 수를 직접 세는 방법

프로세스 PID를 먼저 확인한다.

```bash
jps -l | grep FeignHttpClientMigrationApplication
```

부하를 주는 동안 다른 터미널에서 실제 열린 커넥션 수를 스냅샷 뜬다.

```bash
lsof -p <pid> -a -i tcp | grep :8090
# 또는
netstat -an | grep 8090 | grep ESTABLISHED | wc -l
```

`default` 모드는 요청이 몰릴수록 `ESTABLISHED`/`TIME_WAIT` 소켓 수가 요청 수에 비례해서 늘어난다. `hc5`/`okhttp` 는 `maxTotal` 이하로 수렴한다.

### 4. 스레드 덤프로 대기 여부 확인

풀이 꽉 찼을 때 hc5/okhttp 클라이언트 스레드가 커넥션을 기다리며 블로킹되는지는 스레드 덤프로 확인할 수 있다.

```bash
jstack <pid> | grep -A5 "pool-2-thread"
```

`leaseConnection` 류의 프레임이 보이면 풀이 꽉 차서 대기 중이라는 뜻이다. `default` 모드에는 이런 대기 지점 자체가 없다(대신 소켓을 계속 새로 연다).

## 실행 결과 (concurrency=20, delayMs=200)

```
[DEFAULT]        elapsedMs=291 success=20 failure=0   # 풀 통계 없음
[HC5]            elapsedMs=302 success=20 failure=0   # pool stats = [leased: 0; pending: 0; available: 20; max: 50]
[OKHTTP]         elapsedMs=303 success=20 failure=0   # connectionCount=20 idleConnectionCount=20
[HTTP_INTERFACE] elapsedMs=326 success=20 failure=0   # pool stats = [leased: 0; pending: 0; available: 20; max: 50]
```

로컬호스트에서는 handshake 비용이 미미해서 `elapsedMs` 자체는 네 모드 모두 비슷하다. 이 데모에서 실제로 다른 것은 소요 시간이 아니라 **풀 통계 조회 가능 여부**(default 는 없음)와 **`connection impl` 로그로 보이는 구현체**(HttpURLConnection vs ApacheHttp5Client vs OkHttpClient)다.

`./gradlew :feign-http-client-migration:test` 로 실행되는 `FeignClientFactoryTests` 는 네 클라이언트가 모두 정상 응답하는지, hc5/okhttp/http-interface 가 풀 통계를 조회할 수 있는지, `FeignClientFactory` 가 `HTTP_INTERFACE` 모드를 거부하는지를 검증한다. 5개 테스트 전부 통과 확인.

## 마이그레이션 방향

Feign 은 현재 유지보수 단계 프로젝트다. `feign-hc5`/`feign-okhttp` 로 풀을 붙여서 당장의 문제는 해결할 수 있지만, 장기적으로는 Spring 이 표준으로 제공하는 HTTP Interface(`@GetExchange` + `HttpServiceProxyFactory`)로 옮기는 편이 낫다. `http-interface` 모드가 그 목표 지점이며, 커넥션 풀 설정(`PoolingHttpClientConnectionManager`)은 `hc5` 모드와 동일하게 맞춰서 "같은 풀을 Feign 대신 Spring 표준 스택으로 옮겼을 때" 동작이 같다는 것을 보여준다.
