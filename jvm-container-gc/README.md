## jvm-container-gc

Pod의 CPU, 메모리 limit이 바뀌면 JVM ergonomics에 의해 기본 GC, GC worker 수, Heap 크기가 어떻게 바뀌는지 실제 Kubernetes Pod에서 확인하는 예제입니다.
OpenTelemetry Java agent → OTel Collector → Prometheus → Grafana로 GC 변화를 지표로 확인합니다.

- 블로그: [Pod 리소스를 줄였더니 GC가 바뀌었다](https://eottabom.github.io/post/kubernetes-resources-jvm-gc-ergonomics/)

### 실행 환경

| 항목 | 값 |
| --- | --- |
| 클러스터 | kind v0.32 (Kubernetes v1.36), 단일 노드 |
| 호스트 | Docker Desktop, Linux/arm64, cgroup v2, 10 CPU / 8GiB |
| JDK | Temurin 21.0.10+7 (`eclipse-temurin:21-jre`) |
| 애플리케이션 | Spring Boot 4 게시글 피드 API |
| 부하 | k6 0.49.0, constant-arrival-rate |
| 관측 | OTel Java agent 2.31.1, Collector 0.123.0, Prometheus 3.2.1, Grafana 10.4.3 |

필요한 도구: Docker, kind, kubectl, envsubst, JDK 25(빌드용), Python 3

### 구성

```
jvm-container-gc/
├── app/                        ← Spring Boot 게시글 피드 API (Java 21 바이트코드, 이미지에 OTel agent 포함)
├── k8s/
│   ├── observability.yaml      ← OTel Collector, Prometheus, Grafana
│   ├── grafana/                ← jvm-resize.json(리소스 변경 전후), jvm-matrix.json(조건별 비교, case 변수)
│   ├── app.yaml                ← 앱 Deployment/Service 템플릿
│   └── load/                   ← k6 부하: k6.js(부하 스크립트), k6-job.yaml(Job 템플릿)
├── run/
│   ├── cluster.sh              ← up | forward | down
│   ├── resize.sh               ← 부하를 유지한 채 resources 만 바꿔 Pod 재생성
│   ├── matrix.sh               ← 리소스 조건별 순차 측정
│   ├── probe.sh                ← limits 경계, JDK 버전, -server 옵션별 기본 GC 선택 확인
│   ├── summarize.py            ← (Python) matrix 결과 → summary.csv 집계
│   ├── export-metrics.py       ← (Python) 시나리오 구간의 Prometheus 쿼리 결과 → JSON 저장
│   └── lib.sh
└── results/
```

### 애플리케이션

`GET /feed`는 요청마다 게시글 200개짜리 피드를 만들어 JSON으로 응답하고, 최근 피드 2,000건을 메모리에 캐시합니다.
Young 영역에는 요청마다 생기는 단기 객체가, Old 영역에는 캐시된 피드가 쌓이도록 만든 부하입니다.
`DEMO_FEED_POSTS`, `DEMO_FEED_RETAINED` 환경 변수로 크기를 바꿀 수 있습니다.

`GET /runtime`은 JVM이 인식한 CPU 수, 최대 Heap, GC 이름을 돌려줍니다.

### 1. 준비

```bash
bash jvm-container-gc/run/cluster.sh up
bash jvm-container-gc/run/cluster.sh forward   # Grafana http://localhost:13000/d/jvm-resize
```

### 2. 리소스 변경 시나리오

같은 Deployment에 고정 RPS 부하를 유지한 채, 일정 시간 뒤 `kubectl set resources`로 limit만 8 CPU / 4Gi → 1 CPU / 1Gi로 바꿉니다.

```bash
bash jvm-container-gc/run/resize.sh                                          # 300 RPS → results/resize
RATE=400 bash jvm-container-gc/run/resize.sh jvm-container-gc/results/resize-high
python3 jvm-container-gc/run/export-metrics.py jvm-container-gc/results/resize   # cluster.sh forward 실행 중
```

`BEFORE_SECONDS`, `AFTER_SECONDS`(기본 360초)로 변경 전후 시간을 바꿀 수 있습니다.

| 패널 | PromQL |
| --- | --- |
| JVM이 인식한 CPU 수 | `max by(host_name) (jvm_cpu_count{service_name="app"})` |
| 최대 Heap | `sum by(host_name) (jvm_memory_limit_bytes{service_name="app",jvm_memory_type="heap"})` |
| GC 종류별 이벤트 수 | `sum by(host_name, jvm_gc_name) (rate(jvm_gc_duration_seconds_count{service_name="app"}[1m]))` |
| GC에 쓴 시간 비율 | `100 * sum by(host_name) (rate(jvm_gc_duration_seconds_sum{service_name="app",jvm_gc_name!="G1 Concurrent GC"}[1m]))` |
| HTTP 응답 시간 p99.9 | `histogram_quantile(0.999, sum by(le, host_name) (rate(http_server_request_duration_seconds_bucket{service_name="app",http_route="/feed"}[2m])))` |
| Heap 사용량 | `sum by(host_name) (jvm_memory_used_bytes{service_name="app",jvm_memory_type="heap"})` |

OTel Collector의 Prometheus exporter는 `metric_expiration`을 15초로 줄였습니다.
기본값(5분)이면 Pod 교체 후에도 이전 Pod의 마지막 값이 계속 노출돼 변경 전후 값이 겹쳐 보입니다.

Prometheus는 클러스터 안에만 있어 `cluster.sh down` 하면 데이터도 사라집니다. 시나리오가 끝나면 `export-metrics.py`로 원본을 남겨둡니다.

### 3. 조건별 측정

조건마다 Pod 하나만 띄우고 고정 RPS로 부하를 줍니다. warmup 구간은 집계에서 뺍니다.

```bash
bash jvm-container-gc/run/matrix.sh                 # RATE=300 WARMUP_SECONDS=60 MEASURE_SECONDS=180 REPEAT=3
CASES='g1-small g1-small-apc2' bash jvm-container-gc/run/matrix.sh   # 일부 조건만
```

| 조건 | CPU / Memory limit | GC 옵션 |
| --- | --- | --- |
| default-large | 8 / 4Gi | 미지정 (G1) |
| default-small | 1 / 1Gi | 미지정 (Serial) |
| g1-cpu-small | 1 / 4Gi | `-XX:+UseG1GC` |
| g1-small | 1 / 1Gi | `-XX:+UseG1GC` |
| g1-small-apc2 | 1 / 1Gi | `-XX:+UseG1GC -XX:ActiveProcessorCount=2` |

| 지표 | 출처 |
| --- | --- |
| 응답 시간 p50/p95/p99/max, drop 수 | k6 `measure` 시나리오 |
| GC pause 횟수/p99/max/합계, Full GC 횟수 | GC 로그 중 measure 구간 (Concurrent 단계 제외) |
| CPU 사용량, throttling 비율/시간 | measure 시작, 종료 시점 cgroup `cpu.stat` 차이 |

CPU request는 모든 조건에서 500m입니다. JVM은 limit만 보므로 결과에는 영향이 없고, 단일 노드에 8 CPU 조건과 k6가 함께 스케줄되도록 낮췄습니다.

Grafana의 `jvm-matrix` 대시보드에서 `case` 변수를 바꾸면 조건별 GC 종류, GC 시간 비율, Heap 사용량, CPU 사용량을 볼 수 있습니다. (http://localhost:13000/d/jvm-matrix)

### 4. GC 선택 경계

```bash
bash jvm-container-gc/run/probe.sh
```

limits를 바꾼 Pod에서 `java -version`만 실행해 기본 GC 선택을 확인합니다. 결과는 `results/`에 파일 3개로 남습니다.

| 확인 | 이미지 | 조건 |
| --- | --- | --- |
| limits 경계 | 앱 이미지 (JDK 21) | cpu 1, 2, 8 / mem 1Gi, 1791Mi, 1792Mi, 4Gi, `-XX:ActiveProcessorCount=2` |
| JDK 버전 | `eclipse-temurin:{8,11,17,21}-jre` | cpu 1, 2 / mem 1Gi, 1792Mi, 4Gi |
| `-server` 옵션 | `eclipse-temurin:{8,11,17,21}-jre` | cpu 1, mem 1Gi에서 옵션 없음, `-server`, `-client`, `-XX:+AlwaysActAsServerClassMachine` |

### results

| 경로 | 내용 |
| --- | --- |
| `selection-boundary.txt` | limits 경계별 기본 GC, 인식 CPU/메모리, 최대 Heap |
| `jdk-versions.txt` | Temurin 8, 11, 17, 21 이미지별 기본 GC (`PrintFlagsFinal`) |
| `server-option.txt` | limits cpu 1, mem 1Gi에서 `-server`, `-client`, `-XX:+AlwaysActAsServerClassMachine`별 GC |
| `resize/`, `resize-high/` | 300 / 400 RPS 리소스 변경 시나리오. `timeline.txt`(부하 시작, 변경, 종료 시각), k6 결과, 변경 전후 GC 초기화 로그, `prometheus.json`(대시보드 쿼리 결과 원본) |
| `matrix/<case>-flags.txt` | 같은 Pod에서 띄운 JVM의 `os+container` 로그, `gc+init`, `PrintFlagsFinal` |
| `matrix/<case>-run-<n>.*` | k6 summary, GC 로그, cgroup `cpu.stat` |
| `matrix/summary.csv`, `summary-median.csv` | 실행별 / 조건별 중앙값 집계 |

### 주의

- 단일 노드 kind 클러스터라 k6, 관측 스택, 앱이 같은 호스트 CPU를 나눠 씁니다. 앱 Pod의 CPU limit은 cgroup으로 강제되지만 호스트 경합까지 통제하지는 않습니다.
- 고정 RPS 기준의 경향을 보는 실험이며, GC 간 우열을 확정하는 벤치마크가 아닙니다.

### 정리

```bash
bash jvm-container-gc/run/cluster.sh down
```
