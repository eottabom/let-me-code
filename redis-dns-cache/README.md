# redis-dns-cache

Redis 연결에서 `UnknownHostException` 이 매번이 아니라 간헐적으로 발생하는 상황을 확인하기 위한 작은 예제입니다.

Redis 서버 자체를 재현하기보다, 기존 연결은 그대로 쓰다가 새 연결 또는 재연결이 필요한 순간에만 hostname lookup 이 다시 발생한다는 점을 확인하는 데 초점을 둡니다.

Lettuce 는 기본적으로 Netty 의 `DnsAddressResolverGroup` 을 사용합니다. 이 resolver 는 JVM 의 `InetAddress` 를 거치지 않고 DNS TTL 을 직접 파싱해서 자체 캐시를 관리합니다. TTL 이 만료된 순간 DNS 가 일시적으로 응답하지 않으면 `UnknownHostException` 이 발생할 수 있습니다.

## 실행

DNS cache 설정과 hostname 해석 결과를 확인합니다.

```bash
./gradlew :redis-dns-cache:bootRun --args="inspect redis-host"
```

Lettuce 기본 설정(Netty resolver)으로 Redis 연결을 시도하고, 실패 원인을 출력합니다.

```bash
./gradlew :redis-dns-cache:bootRun --args="connect redis-host.invalid 6379"
```

JVM `InetAddress` resolver 를 사용하도록 `DefaultAddressResolverGroup` 을 지정해서 연결을 시도합니다.

```bash
./gradlew :redis-dns-cache:bootRun --args="connect-jvm redis-host.invalid 6379"
```

두 커맨드를 각각 실행하면 resolver 종류에 따라 예외 cause chain 이 어떻게 다르게 보이는지 비교할 수 있습니다.

이 예제는 간헐성을 그대로 재현하는 것이 아닙니다. `redis-host.invalid` 는 매번 실패하는 host 입니다. 여기서 보고 싶은 건 실패가 발생하는 순간 Lettuce 예외 안쪽에 `UnknownHostException` 이 어떤 계층에서 들어오는지, 그리고 resolver 를 바꾸면 그 계층이 어떻게 달라지는지입니다.

## 볼 것

- `networkaddress.cache.ttl`
- `networkaddress.cache.negative.ttl`
- `InetAddress.getAllByName(...)` 결과
- Lettuce 연결 실패 예외의 cause chain
- Netty resolver 사용 시와 JVM resolver 사용 시의 cause chain 차이
