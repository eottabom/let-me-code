## spring-boot-startup

Spring Boot 애플리케이션이 Started, Ready 시점에 무엇이 끝났고, Ready 뒤 첫 요청에 어떤 비용이 남는지 로컬에서 측정하는 예제입니다.
시작 이벤트, DispatcherServlet, lazy 빈, 커넥션 풀, 클래스 로딩, JIT 컴파일을 실험마다 나눠 보고, 실험마다 애플리케이션을 10번 새로 띄워 중앙값을 냅니다.

- 블로그: [Spring Boot 는 Ready 뒤에도 첫 요청 비용이 남는다 - 시작 과정과 JVM 으로 따라가기](https://eottabom.github.io/post/spring-boot-when-ready/)

### 실행 환경

| 항목 | 값 |
| --- | --- |
| JDK | Azul Zulu 25.0.2 (`JAVA_HOME` 으로 지정) |
| 애플리케이션 | Spring Boot 4.1.2 (Spring Framework 7.0.10, Tomcat 11.0.27), Actuator |
| DB | MySQL 8.4 (Docker, localhost:13306), HikariCP 7.0.2, Hibernate 7.4.13 |
| 부하 | k6 v2.3.0 (맥에 직접 설치, 08 에서만 사용) |

필요한 도구: JDK 25, Docker, Python 3, jq, k6

### 구성

```
spring-boot-startup/
├── app/                                  ← Spring Boot 테스트 애플리케이션
│   └── src/main/
│       ├── java/.../startup/
│       │   ├── StartupApplication.java    ← BufferingApplicationStartup, StartupTimeline 등록
│       │   ├── StartupTimeline.java       ← 시작 이벤트를 JVM 시작 기준 경과 시간과 함께 [timeline] 으로 출력
│       │   ├── RequestTimingFilter.java   ← 요청마다 순번과 서버 처리 시간을 [request] 로 출력 (LOG_REQUESTS=false 로 끔)
│       │   ├── SampleController.java      ← /hello, /users/{id}(MySQL), /external(로컬 스텁 서버)
│       │   └── SlowRunner.java            ← RUNNER_DELAY_SECONDS 만큼 걸리는 ApplicationRunner
│       └── resources/
│           ├── application.yml                          ← 기본은 JDBC 만, JPA 자동 설정은 꺼 둠
│           ├── application-jpa.yml                      ← JPA 를 켬
│           ├── application-jpa-no-boot-connection.yml   ← 시작할 때 DB 에 연결하는 두 곳을 끈 JPA
│           └── application-lazy.yml                     ← lazy-initialization 을 켬
├── run/
│   ├── 00-setup.sh               ← MySQL 컨테이너, 스텁 서버, bootJar
│   ├── 01 ~ 08                   ← 실험 스크립트
│   ├── lib.sh                    ← 앱 시작, Ready 대기, 종료, 로그에서 개인 경로 지우기
│   ├── analyze.py                ← 로그와 기록 분석 (아래 하위 명령)
│   ├── load.js                   ← k6 부하 (VU 4개, 10초, 구간별 응답 시간)
│   └── stub/stub.json            ← /external 이 부르는 스텁 응답
└── results/
```

실험 스크립트는 `run/analyze.py` 의 하위 명령으로 결과를 정리합니다.

| 하위 명령 | 하는 일 | 쓰는 실험 |
| --- | --- | --- |
| `stats` | 반복 실행 결과를 지표별 중앙값, 최솟값, 최댓값으로 묶음 | 01 \~ 08 |
| `requests` | 요청 순번 구간(1번째, 2\~10번째, 11번째 이후)별 응답 시간 | 04, 05, 07 |
| `jvm-windows` | 클래스 로딩, JIT 컴파일 로그를 Ready 와 요청 기준 구간으로 나눔 | 06 |
| `jfr-phases` | JFR 기록에서 구간별 CPU, 컴파일러 스레드 CPU, 컴파일 수 | 08 |
| `k6-summary` | k6 요약을 부하 시작 시각과 구간별 처리량, 응답 시간으로 바꿈 | 08 |

### 실행

```bash
export JAVA_HOME=<Zulu 25 경로>

# 준비. MySQL 컨테이너와 스텁 서버를 띄우고 bootJar 를 만든다.
bash spring-boot-startup/run/00-setup.sh

# 실험 실행 (예시). 반복 횟수는 REPEAT 로 바꿀 수 있다 (기본 10, 버리는 실행 1번은 따로).
bash spring-boot-startup/run/02-events.sh
REPEAT=3 bash spring-boot-startup/run/07-first-vs-repeat.sh
```

| 스크립트 | 확인하는 것 | 블로그 섹션 |
| --- | --- | --- |
| `01-startup-steps.sh` | `/actuator/startup` 의 단계별 소요 시간 | 단계별 소요 시간 |
| `02-events.sh` | 시작 이벤트 순서와 Liveness, Readiness 변화 (Runner 2초) | 이벤트 순서 |
| `03-port-before-ready.sh` | 포트는 열렸지만 readiness 가 503 인 구간 (Runner 5초) | readiness 가 503 인데 요청은 왜 처리될까? |
| `04-deferred-init.sh` | 기본값, load-on-startup=1, lazy-initialization 의 DispatcherServlet 초기화와 첫 `/hello` | DispatcherServlet, lazy-initialization |
| `05-connection-pool.sh` | JDBC, JPA 기본 설정, ddl-auto 를 지정하고 메타데이터 조회를 끈 JPA 에서 Hikari 풀이 시작되는 시점과 첫 `/users/1` | 커넥션 풀 |
| `06-classload-jit.sh` | readiness 확인 후, 확인 없이 첫 `/hello` 를 보냈을 때 구간별 클래스 로딩과 컴파일 수 | 클래스 로딩, JIT 컴파일 |
| `07-first-vs-repeat.sh` | API 마다 새로 띄워 1번째, 2\~10번째, 11\~200번째 응답 시간 | 첫 요청과 반복 요청 |
| `08-cpu-jit.sh` | JFR 로 기록한 구간별 애플리케이션 CPU, 컴파일러 스레드 CPU, 컴파일 수와 k6 응답 시간 | Ready 직후 요청이 몰릴 때의 CPU 와 JIT 컴파일 |

### 결과

- `results/2026-11-07-boot-4.1.2-zulu-25.0.2/` 에 블로그에 실은 실행의 결과가 있습니다.
- 실험을 다시 실행하면 출력이 `results/latest/` 에 남습니다. 이 폴더는 git 에 올라가지 않습니다.
- `<실험>.txt` 는 실행 출력과 요약, `<실험>.metrics.tsv` 는 실행마다 모은 값, `<실험>.*.first.*` 는 첫 실행의 앱 로그와 원본 기록입니다.
- 앱 로그와 클래스 로딩 로그에서 개인 경로와 사용자 이름은 지웠고, 경로가 들어간 애플리케이션 시작 줄 하나를 뺐습니다. 나머지는 원본 그대로입니다.

| 블로그의 값 | 결과 파일 |
| --- | --- |
| refresh 1,348ms, 설정 클래스 분석 479ms, 빈 354개 | `01-startup-steps.txt` |
| 이벤트별 JVM 시작 기준 시간, Started 2,030ms, Ready 4,079ms | `02-events.txt` |
| 약 2.4초에 포트 열림, 약 7.3초에 readiness 200 | `03-port-before-ready.txt` |
| DispatcherServlet 이 readiness 200 약 150ms 전에 초기화, 첫 `/hello` 기본값 5.4ms, lazy 109.1ms | `04-deferred-init.txt`, `04-deferred-init.*.first.app.txt` |
| 첫 `/users/1` JDBC 205.9ms, JPA 기본 설정 16.8ms, ddl-auto 지정, 메타데이터 조회 끈 JPA 190.1ms | `05-connection-pool.txt`, `05-connection-pool.*.first.app.txt` |
| 첫 HTTP 요청이 클래스 약 550개 로딩, 첫 `/hello` 6.3ms 와 58.0ms | `06-classload-jit.txt` |
| C1 에서 C2 로 넘어간 Spring 메서드 예시 | `06-classload-jit.readiness-확인-후.first.compile.txt` |
| `/users/1` 첫 요청 206.4ms 와 순번별 값, API 별 1번째, 2\~10번째, 11\~200번째 | `07-first-vs-repeat.txt`, `07-first-vs-repeat.series.tsv` |
| 요청 0~1초 애플리케이션 CPU 코어 3.7개, 컴파일러 스레드 코어 1.6개, 처리량 초당 1,959건 | `08-cpu-jit.txt`, `08-cpu-jit.first.timeline.tsv` |

### 주의

- 같은 설정이어도 Started 시간이 실행마다 1.6초에서 2.1초 사이로 달라집니다. 실험끼리 수치가 조금씩 다른 것은 이 때문입니다.
- 응답 시간은 가장 바깥 서블릿 필터에서 잰 서버 안의 시간이고, 이 측정용 필터를 처음 실행하는 비용도 함께 들어 있습니다. 08 의 응답 시간은 k6 에서 잰 값입니다.
- 08 에서 Docker 로 띄운 k6 는 컨테이너에서 맥으로 가는 네트워크 경로가 흔들려서, 첫 몇 초를 비교할 수 없었습니다. 맥에 직접 설치한 k6 를 씁니다.
- 앱은 8080 포트를 씁니다. 다른 앱이 8080 을 쓰고 있으면 실험이 바로 멈춥니다.

### 정리

```bash
docker rm -f startup-mysql
kill "$(cat spring-boot-startup/run/.stub.pid)"
```
