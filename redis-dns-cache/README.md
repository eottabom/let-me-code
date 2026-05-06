# redis-dns-cache

Redis 연결에서 `UnknownHostException` 이 매번이 아니라 간헐적으로 발생하는 상황을 확인하기 위한 작은 예제입니다.

Redis 서버 자체를 재현하기보다, 기존 연결은 그대로 쓰다가 새 연결 또는 재연결이 필요한 순간에만 hostname lookup 이 다시 발생한다는 점을 확인하는 데 초점을 둡니다.

## 실행

DNS cache 설정과 hostname 해석 결과를 확인합니다.

```bash
./gradlew :redis-dns-cache:bootRun --args="inspect redis-host"
```

Lettuce 로 Redis 연결을 시도하고, 실패 원인을 출력합니다.

```bash
./gradlew :redis-dns-cache:bootRun --args="connect redis-host 6379"
```

실패한 hostname 을 넣으면 `UnknownHostException` 이 cause chain 에 포함되는지 확인할 수 있습니다.

```bash
./gradlew :redis-dns-cache:bootRun --args="connect redis-host.invalid 6379"
```

이 명령은 간헐성을 그대로 재현한다기보다, 간헐적으로 문제가 터지는 시점에 실제로 보이는 hostname lookup 실패를 작게 확인하는 용도입니다.

## 볼 것

- `networkaddress.cache.ttl`
- `networkaddress.cache.negative.ttl`
- `InetAddress.getAllByName(...)` 결과
- Lettuce 연결 실패 예외의 cause chain
