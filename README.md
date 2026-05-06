## let-me-code

작게 실험 가능한 예제를 모아두는 Gradle 멀티모듈 저장소입니다.
각 모듈은 한 가지 주제에 집중해서 동작 원리나 성능 차이, 프레임워크 사용법을 확인하는 용도로 구성되어 있습니다.

### 프로젝트 구성

| 모듈 | 기술/주제 | 설명 |
| --- | --- | --- |
| `jpa-entity` | Spring Boot, Spring Data JPA, H2 | `Persistable` 기반 엔티티 저장 흐름과 `isNew()` 판단 방식을 테스트로 확인하는 예제 |
| `jpa-y2k38` | Spring Boot, Spring Data JPA, H2 | MySQL `TIMESTAMP` 2038 문제와 `DATETIME` 저장 동작을 검증하는 날짜/시간 예제 |
| `redis-dns-cache` | Spring Boot, Spring Data Redis, Lettuce | Redis 연결의 `UnknownHostException` 과 JVM DNS cache 설정을 확인하는 예제 |
| `mongodb-pagination` | Spring Data MongoDB | `skip/limit` 방식과 커서 기반 페이지네이션 저장소 구현을 비교하는 예제 |
| `grpc-example` | Protobuf, gRPC Java | `person.proto`를 기반으로 gRPC 서버/클라이언트와 메시지 생성을 다루는 예제 |
| `playwright` | Playwright for Java, JUnit | 브라우저 설치용 `installBrowsers`, 코드 생성용 `codegen`, 테스트 샘플을 포함한 예제 |
| `jmh-final-keyword` | JMH | `final` 메서드 여부에 따른 호출 성능 차이를 측정하는 예제 |
| `jmh-logger` | JMH, Logback, SLF4J | 로깅 방식과 설정 차이를 측정하는 예제 |
| `jmh-serialization` | Spring Boot, JMH, Protobuf, Jackson, OkHttp | 직렬화 방식에 따른 성능 차이를 비교하는 예제 |

### 공통 설정

- 루트 `build.gradle`에서 하위 모듈 공통 Java 설정을 관리합니다.
- 현재 toolchain은 Java 25 기준입니다.
- 라이브러리 버전은 `gradle/libs.versions.toml`에서 관리합니다.

### 자주 쓰는 명령

```bash
./gradlew build
./gradlew test
./gradlew :playwright:installBrowsers
./gradlew :playwright:codegen
```

### 참고

- 일부 모듈은 Spring Boot 애플리케이션 형태이고, 일부는 단순 라이브러리/JMH 실행 예제입니다.
- 실험 성격의 저장소라서 모듈마다 의존성과 실행 방식이 다릅니다.
