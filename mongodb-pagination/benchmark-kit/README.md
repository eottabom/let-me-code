# Benchmark Kit

## 목적

- DB 내부 실행 기준(`mongosh explain`) 비교
- 애플리케이션 실행 기준(Spring Boot) 비교

## 버전

- Spring Boot: `4.0.3`
- Gradle: `9.1.0` (Docker build stage)

## 1) DB 내부 비교 (AS-IS vs TO-BE)

```bash
cd benchmark-kit
./run-benchmark.sh
```

대용량 예시:

```bash
TOTAL_DOCS=1000000 PAGE_SIZE=500 PAGES=0,1,10,100,300,1000 ./run-benchmark.sh
```

## 2) Spring Boot 애플리케이션 비교 (Gradle + Docker)

```bash
cd benchmark-kit
SEED=1 TOTAL_DOCS=1000000 PAGE_SIZE=500 PAGES=0,1,10,100,300,1000 ./run-spring-benchmark.sh
```

주의:

- `SEED=1`이면 데이터 재생성
- 로컬 Maven/Gradle 설치 불필요 (Docker로 빌드/실행)
- `PAGES`는 쉼표 구분 (`0,1,10,...`)

## 출력 해석

- `AS-IS`: `skip + limit`
- `TO-BE step`: cursor 기반 단일 페이지 조회 시간
- `TO-BE cumulative`: 0페이지부터 해당 페이지까지 누적 cursor 조회 시간
