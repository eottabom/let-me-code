# timeout-settings

connection timeout, socket/read timeout, write timeout, keep-alive, max-lifetime 을 개념으로만 이해하지 않고, 실제로 그 상황을 만들어서 예외/로그로 확인하기 위한 예제입니다.

블로그: [Timeout 설정 헷갈리지 않기: Tomcat, HTTP Client, HikariCP 기준 정리](https://eottabom.github.io/post/timeout-settings-guide/)

## 필요 환경

- JDK 17+
- Docker 데몬 (Testcontainers 로 MySQL 을 띄우는 테스트/데모가 있습니다)

## 실행

### 자동화 테스트

```bash
./gradlew :timeout-settings:test
```

| 테스트 | 확인하는 것 |
|---|---|
| `ConnectTimeoutTests` | 외부 blackhole IP 에 의존하지 않고, Apache HttpClient5 `ConnectionConfig` 에 connect/read/TTL 값이 설정되는지 |
| `ReadTimeoutTests` | TCP 는 맺혔는데 응답이 늦을 때, read timeout 값 근처에서 실패하는지 (백엔드 지연까지 기다리지 않는지) |
| `ConnectionReuseTests` | 순차 호출이 같은 물리 커넥션(remote port)을 재사용하는지, connection time-to-live 를 넘기면 새 커넥션을 여는지 |
| `TomcatKeepAliveTimeoutTests` | Tomcat 의 `keep-alive-timeout` 이 지난 커넥션을 서버가 실제로 닫는지 (raw socket 으로 직접 확인) |
| `NettyServerIdleTimeoutTests` | WebFlux(Reactor Netty) 서버의 `server.netty.idle-timeout` 이 지난 커넥션을 닫는지 (raw socket 으로 직접 확인) |
| `WebClientTimeoutTests` | WebClient(Reactor Netty)는 connect timeout(채널 옵션)과 read timeout(핸들러)이 서로 다른 API 이며, 둘 다 별도로 설정해야 한다는 것 |
| `PoolMaintenanceTests` | Apache HttpClient5 풀의 `closeIdle()` 이 유휴 커넥션을 한 번 정리하는지 |
| `IdleConnectionEvictorTests` | `IdleConnectionEvictor` 가 `closeIdle()` 호출을 백그라운드에서 주기적으로 대신 수행하는지 |
| `ValidateAfterInactivityTests` | 오래 유휴 상태였던 커넥션을 재사용 전에 검증하면 죽은 커넥션을 새 커넥션으로 교체하는지 |
| `DeadConnectionAfterWaitTimeoutTests` | MySQL 의 `wait_timeout` 을 넘긴 커넥션을 재사용하면 실제 `CommunicationsException` 이 나는지 (Docker 필요) |

`DeadConnectionAfterWaitTimeoutTests` 는 Docker 환경을 찾지 못하면 skip 됩니다. Testcontainers 가 Docker 에 접근할 수 있는 환경에서만 MySQL wait_timeout 시나리오를 검증합니다.

### 수동 데모 - HikariCP max-lifetime 회전

```bash
./gradlew :timeout-settings:bootRun --args="hikari-lifetime"
# args: hikari-lifetime [waitTimeoutSeconds=35] [borrowCount=5] [intervalMs=10000]
```

Docker 로 MySQL 을 띄우고, 새 DB 세션마다 `wait_timeout` 을 넉넉하게(기본 35초) 잡은 뒤 HikariCP `max-lifetime`(30초, HikariCP 가 클램핑 없이 그대로 받아들이는 최소값)이 그보다 먼저 커넥션을 회전시켜서 에러 없이 넘어가는 것을 `CONNECTION_ID()` 로그로 확인합니다.

HikariCP 는 `max-lifetime` 을 30000ms 미만으로 주면 30000ms 로 올리는 게 아니라 기본값인 30분으로 되돌려버립니다. 그래서 이 데모는 그 클램핑을 피하기 위해 `max-lifetime` 을 정확히 30000ms 로 맞춰 쓰고, 실행에는 30초 이상 걸립니다.

## 볼 것

- `TimeoutHttpClientFactory` - connect timeout, read timeout, connection time-to-live(keep-alive 로 재사용 가능한 최대 수명)을 각각 독립적으로 조절
- `TimeoutWebClientFactory` - WebClient 는 connect timeout 을 Netty 채널 옵션으로, read/write timeout 을 파이프라인 핸들러로 각각 설정한다
- `BackendController` - 요청을 처리한 remote port 를 기록해서 커넥션 재사용 여부를 눈으로 확인
- `HikariLifetimeDemo` / `DeadConnectionAfterWaitTimeoutTests` - HikariCP max-lifetime 과 MySQL wait_timeout 의 관계
- `TomcatKeepAliveTimeoutTests` - Tomcat Poller 의 주기적 sweep 때문에 keep-alive-timeout 이 "정확히 그 시간 뒤"가 아니라 "다음 sweep 때" 적용된다는 점
- `PoolMaintenanceTests` / `IdleConnectionEvictorTests` / `ValidateAfterInactivityTests` - Apache HttpClient5 풀의 유휴 커넥션 정리와 재사용 전 검증 동작

## 참고 문서

- [Spring Boot - Common Application Properties (server.tomcat.*)](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.server)
- [HikariCP - README (Configuration)](https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby)
- [HikariCP Wiki - About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- [Apache HttpClient 5 - Connection Management](https://hc.apache.org/httpcomponents-client-5.4.x/current/tutorial/html/connmgmt.html)
- [Project Reactor Netty - Timeout Configurations](https://projectreactor.io/docs/netty/release/reference/index.html#_configuring_timeout)
