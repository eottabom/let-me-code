#!/usr/bin/env bash
# E8. 시작하고 첫 요청을 받을 때 JVM 안에서 누가 CPU 를 쓰는지.
# JFR 로 프로세스 CPU, 스레드별 CPU, JIT 컴파일을 100ms 간격으로 기록하면서,
# Ready 뒤 3초 쉬었다가 k6 로 VU 4개가 /users/1 을 10초 동안 계속 호출한다. 다른 서버가 새로 뜬 이 서버를 부르기 시작하는 상황이다.
# 구간(시작 중, Ready 후 대기, 요청 0~1초, 1~3초, 3~10초)마다 CPU 와 컴파일 수, 처리량, 응답 시간을 낸다.
# 요청마다 남기는 [request] 로그는 끈다. 초당 수천 줄을 쓰는 비용이 CPU 에 섞이지 않게 하려는 것이다.
source "$(dirname "$0")/lib.sh"

METRICS="$OUT_DIR/08-cpu-jit.metrics.tsv"
: >"$METRICS"
JFR_FILE="$OUT_DIR/08-cpu-jit.jfr"
JFR_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}jfr"
# k6 는 맥에 직접 설치한 것을 쓴다. Docker 로 띄우면 컨테이너에서 맥으로 가는 네트워크 경로가 흔들려서 첫 몇 초를 비교할 수 없었다.
echo "k6: $(k6 version | head -1)"
JFR_OPTION="-XX:StartFlightRecording:filename=$JFR_FILE,dumponexit=true,jdk.CPULoad#period=100ms,jdk.ThreadCPULoad#period=100ms,jdk.Compilation#threshold=0ms"

discard_run
for run in $(seq 1 "$REPEAT"); do
	log "실행 $run/$REPEAT"
	rm -f "$JFR_FILE"
	LOG_REQUESTS=false start_app 08-cpu-jit "$JFR_OPTION"
	wait_ready
	sleep 3
	rm -f "$OUT_DIR/k6-summary.json"
	SUMMARY_FILE="$OUT_DIR/k6-summary.json" k6 run --quiet "$DEMO_DIR/run/load.js" >/dev/null
	# k6 결과를 부하 시작 시각(epoch 초) 한 줄과 metric<TAB>value 줄로 바꾼다.
	python3 "$DEMO_DIR/run/analyze.py" k6-summary "$OUT_DIR/k6-summary.json" >"$OUT_DIR/08-load.txt"
	sleep 1
	stop_app
	"$JFR_BIN" print --json --events jdk.CPULoad,jdk.ThreadCPULoad,jdk.Compilation,jdk.JVMInformation "$JFR_FILE" >"$OUT_DIR/08-jfr.json"
	# 첫 실행만 100ms 단위 시간표를 남긴다. 빈 배열은 macOS 기본 bash 3.2 에서 set -u 에 걸려서 위처럼 펼친다.
	timeline_option=()
	if [ "$run" = 1 ]; then
		timeline_option=(--timeline "$OUT_DIR/08-cpu-jit.first.timeline.tsv")
	fi
	python3 "$DEMO_DIR/run/analyze.py" jfr-phases "$OUT_DIR/08-jfr.json" "$APP_LOG" "$(head -1 "$OUT_DIR/08-load.txt")" ${timeline_option[@]+"${timeline_option[@]}"} >>"$METRICS"
	tail -n +2 "$OUT_DIR/08-load.txt" >>"$METRICS"
done
rm -f "$JFR_FILE" "$OUT_DIR/08-jfr.json" "$OUT_DIR/08-load.txt" "$OUT_DIR/k6-summary.json" "$OUT_DIR/08-cpu-jit.app.txt"

log "실행 $REPEAT 번 요약 (CPU 는 코어 몇 개를 썼는지, 이 장비는 $(python3 -c 'import os; print(os.cpu_count())')코어)"
summarize "$METRICS"
