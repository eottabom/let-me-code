## kubernetes-probes

Liveness, Readiness, Startup Probe 가 실패했을 때 재시작과 트래픽이 실제로 어떻게 바뀌는지 kind 클러스터에서 확인하는 예제입니다.
Spring Boot Actuator 의 liveness, readiness 상태를 일부러 바꾸는 엔드포인트를 두고, 시나리오별 스크립트로 Pod 상태와 EndpointSlice 를 1초마다 기록합니다.

- 블로그: [Kubernetes Probe 이해하기 - Liveness, Readiness, Startup](https://eottabom.github.io/post/kubernetes-probes/)

### 실행 환경

| 항목 | 값 |
| --- | --- |
| 클러스터 | kind v0.32 (Kubernetes v1.36.1), 단일 노드 |
| 애플리케이션 | Spring Boot 4.1.2, Actuator |
| JDK | `eclipse-temurin:21-jre` (빌드는 JDK 25, Java 21 바이트코드) |
| Probe 기본 설정 | periodSeconds 5, failureThreshold 3, timeoutSeconds 1 |

필요한 도구: Docker, kind, kubectl, JDK 25(빌드용)

### 구성

```
kubernetes-probes/
├── app/                          ← Spring Boot 테스트 애플리케이션
│   └── src/main/java/.../probes/
│       ├── ChaosController.java  ← liveness, readiness 상태 변경, exec probe 파일, 느린 응답(/slow)
│       ├── SlowStartup.java      ← 빈 초기화에서 시작 지연 (STARTUP_DELAY_SECONDS)
│       ├── SlowRunner.java       ← ApplicationRunner 에서 시작 지연 (RUNNER_DELAY_SECONDS)
│       └── DependencyHealthIndicator.java ← DB 역할 대상에 TCP 연결만 확인하는 헬스 체크
├── k8s/
│   ├── app.yaml                  ← 기본 Deployment, Service (replicas 2)
│   ├── fake-db.yaml              ← DB 역할을 하는 서버
│   └── probe-types.yaml          ← TCP, Exec, gRPC probe 용 Pod
└── run/
    ├── 00-setup.sh               ← 클러스터 생성, 이미지 빌드, kind load
    ├── 01 ~ 07                   ← 시나리오별 스크립트
    └── lib.sh                    ← 공통 함수 (상태 기록 observe 등)
```

### 실행

```bash
# 준비. 클러스터가 이미 있으면 이미지만 다시 넣는다.
bash kubernetes-probes/run/00-setup.sh

# 시나리오 실행 (예시)
bash kubernetes-probes/run/01-liveness-fail.sh
bash kubernetes-probes/run/04-dependency-in-probe.sh liveness
```

| 스크립트 | 확인하는 것 | 블로그 섹션 |
| --- | --- | --- |
| `01-liveness-fail.sh` | 한 Pod 의 liveness 만 실패시켰을 때 재시작 시점과, 그동안 엔드포인트에 남는지 | Liveness Probe |
| `02-readiness-fail.sh` | 한 Pod 의 readiness 만 실패시켰을 때 재시작 없이 엔드포인트에서만 빠지는지 | Readiness Probe |
| `03-slow-startup.sh` | 시작에 40초 걸리는 앱을 liveness 만으로 띄웠을 때와 startupProbe 를 추가했을 때 | Startup Probe |
| `04-dependency-in-probe.sh liveness \| readiness` | DB 체크를 liveness, readiness 에 넣고 DB 를 내렸다 올렸을 때 | DB 체크를 liveness 에 넣으면 |
| `05-timeout.sh` | 1.5초 걸리는 엔드포인트를 timeoutSeconds 1, 3 으로 liveness 에 걸었을 때 | timeoutSeconds 1초 |
| `06-probe-types.sh` | TCP, Exec, gRPC probe 가 실패하는 방식과 이벤트 메시지 | 검사 방식 |
| `07-rolling-update.sh` | rollout restart 중 요청 실패 건수, preStop 대기 없음과 있음 비교 | readiness 가 있어도 배포 중 요청이 실패한다 |

시작, 종료 과정의 상태 변화는 클러스터 없이 로컬에서도 볼 수 있습니다. `application.yml` 에서 `org.springframework.boot.availability` 로그를 DEBUG 로 켜 두었습니다.

```bash
./gradlew :kubernetes-probes:app:bootJar
RUNNER_DELAY_SECONDS=10 java -jar kubernetes-probes/app/build/libs/app-0.0.1-SNAPSHOT.jar

# 다른 터미널에서. Runner 실행 중에는 liveness 200, readiness 503
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/actuator/health/liveness
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/actuator/health/readiness
```

### 결과

- `results/2026-11-03-boot-4.1.2/` 에 블로그에 실은 실행의 원본 로그가 있습니다.
- 시나리오를 다시 실행하면 출력이 `results/latest/<스크립트 이름>.log` 에도 남습니다. 이 폴더는 git 에 올라가지 않습니다.
- 상태 기록의 `[시:분:초]` 는 실행한 PC 의 시각(KST)이고, kubectl 이벤트의 시각은 UTC 입니다. 예를 들어 21:41:43 KST 는 12:41:43Z 입니다.

| 블로그의 값 | 로그 |
| --- | --- |
| liveness 13초 판정, 18초 재시작, 24초 ready | `01-liveness-fail.txt` (BROKEN 21:41:43, Unhealthy/Killing 이벤트 12:41:56Z) |
| readiness 13초 제외, 5초 복귀, Service 요청 40번 | `02-readiness-fail.txt` |
| liveness 만으로 40초, 83초 재시작, startupProbe 로 43초 ready | `03-slow-startup.txt` |
| DB 체크 liveness: 약 15초 간격 재시작, 59초 CrashLoopBackOff, 복구 후 21초 | `04-dependency-in-probe-liveness.txt` |
| DB 체크 readiness: 15초 제외, 복구 후 5초 | `04-dependency-in-probe-readiness.txt` |
| timeoutSeconds 1 은 재시작 3번, 3 은 0번 | `05-timeout.txt` |
| TCP 통과, Exec, gRPC 재시작과 이벤트 메시지 | `06-probe-types.txt` |
| 배포 중 실패 preStop 없음 3, 3, 5건 / preStop 5초 1, 0, 0건 | `07-rolling-update.txt` |
| Runner 지연 중 liveness 200, readiness 503 | `local-runner-delay.txt` (`RUNNER_DELAY_SECONDS=10`, 포트 18186) |
| 종료 순서와 graceful shutdown 3.5초 | `local-graceful-shutdown.txt` (4초짜리 `/slow` 요청을 보낸 채 SIGTERM, 포트 18187) |

로컬 로그는 경로가 들어간 애플리케이션 시작 줄 하나를 뺐고, 나머지는 원본 그대로입니다.

### 주의

- 기록의 초 단위 값은 실행할 때마다 조금씩 다릅니다. 상태 기록은 kubectl 을 1초마다 호출해서 만들기 때문에 1초 정도 오차가 있습니다.
- 시나리오 스크립트는 같은 Deployment 를 고쳐 쓰기 때문에 한 번에 하나씩 실행합니다. 앞 시나리오가 넣은 env 가 남아 결과가 이상하면 `kubectl --context kind-probes-lab -n probes delete deploy/probes-app` 후 다시 실행합니다.
### 정리

```bash
kind delete cluster --name probes-lab
```
