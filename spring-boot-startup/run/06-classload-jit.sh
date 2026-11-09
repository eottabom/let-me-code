#!/usr/bin/env bash
# E6. 클래스 로딩과 JIT 컴파일.
# -Xlog:class+load 와 -Xlog:jit+compilation 을 켜고 /hello 를 200 번 부른 뒤,
# Ready 전, Ready 후 1번째 요청 전, 1번째 요청 처리 중, 2~10번째, 11~200번째 구간마다 새로 로딩된 클래스와 컴파일된 메서드를 센다.
# Ready 를 readiness 요청으로 확인한 경우와, 요청 없이 로그로만 확인한 경우를 비교한다.
# 앱이 받는 첫 HTTP 요청이 공통 경로의 클래스를 로딩하므로, 두 경우는 /hello 가 첫 요청인지 아닌지가 다르다.
# 경우마다 앱을 REPEAT 번 새로 띄워 구간별 개수의 중앙값을 내고, 원본 로그는 첫 실행 것만 남긴다.
source "$(dirname "$0")/lib.sh"

METRICS="$OUT_DIR/06-classload-jit.metrics.tsv"
: >"$METRICS"
CLASS_LOG="$OUT_DIR/06-class-load.txt"

discard_run
for case_name in readiness-확인-후 readiness-확인-없이; do
for run in $(seq 1 "$REPEAT"); do
	log "$case_name 실행 $run/$REPEAT"
	rm -f "$CLASS_LOG"
	# -XX:+PrintCompilation 은 표준 출력에 앱 로그와 섞여 찍히므로, 같은 내용을 통합 로깅으로 파일에 따로 받는다.
	rm -f "$OUT_DIR/06-compile.txt"
	start_app 06-classload-jit "-Xlog:class+load:file=$CLASS_LOG:uptimemillis" \
		"-Xlog:jit+compilation=debug:file=$OUT_DIR/06-compile.txt:uptimemillis"
	if [ "$case_name" = readiness-확인-후 ]; then
		wait_ready
	else
		wait_ready_log
	fi
	sleep 0.5
	call_repeatedly /hello 200 >/dev/null
	sleep 0.5
	python3 "$DEMO_DIR/run/analyze.py" jvm-windows "$APP_LOG" "$CLASS_LOG" "$OUT_DIR/06-compile.txt" | sed "s/^/$case_name /" >>"$METRICS"
	if [ "$run" = 1 ]; then
		cp "$CLASS_LOG" "$OUT_DIR/06-classload-jit.$case_name.first.class-load.txt"
		cp "$OUT_DIR/06-compile.txt" "$OUT_DIR/06-classload-jit.$case_name.first.compile.txt"
		sanitize "$OUT_DIR/06-classload-jit.$case_name.first.class-load.txt"
		log "$case_name 첫 실행: 1번째 요청 처리 중 로딩된 클래스와 C1 에서 C2 로 다시 컴파일된 메서드"
		python3 "$DEMO_DIR/run/analyze.py" jvm-windows "$APP_LOG" "$CLASS_LOG" "$OUT_DIR/06-compile.txt" --detail | sed -n '/^$/,$p'
	fi
	stop_app
done
done
rm -f "$CLASS_LOG" "$OUT_DIR/06-compile.txt" "$OUT_DIR/06-classload-jit.app.txt"

log "실행 $REPEAT 번 요약 (개수)"
summarize "$METRICS"
