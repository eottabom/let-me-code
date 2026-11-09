#!/usr/bin/env bash
# E2. 시작 이벤트가 발행되는 순서와 그때의 Liveness, Readiness 변화.
# Runner 를 2초 걸리게 두어 Started 와 Ready 사이에 Runner 가 있다는 것이 보이게 한다.
# 앱을 REPEAT 번 새로 띄워 이벤트별 경과 시간의 중앙값을 내고, 상세 순서는 첫 실행 것만 남긴다.
source "$(dirname "$0")/lib.sh"

METRICS="$OUT_DIR/02-events.metrics.tsv"
: >"$METRICS"

discard_run
for run in $(seq 1 "$REPEAT"); do
	log "실행 $run/$REPEAT (Runner 2초)"
	RUNNER_DELAY_SECONDS=2 start_app 02-events
	wait_ready
	sleep 0.5
	# [timeline] +  2403ms main  ServletWebServerInitializedEvent → 이벤트<TAB>2403
	timeline | sed -E 's/^\[timeline\] \+ *([0-9]+)ms +[^ ]+ +(.*)$/\2\t\1/' >>"$METRICS"
	if [ "$run" = 1 ]; then
		log "첫 실행: 이벤트 순서 (JVM 시작 기준 경과 시간, 스레드, 이벤트)"
		grep -E '^\[timeline\]|Tomcat started on port|Started StartupApplication|Runner 지연' "$APP_LOG" \
			| sed -E 's/^[0-9T:.+-]+ +[A-Z]+ +[0-9]+ --- \[[^]]*\] \[ *([^]]*)\] [^ ]+ +: /[log]      \1  /'
		cp "$APP_LOG" "$OUT_DIR/02-events.first.app.txt"
	fi
	stop_app
done
sanitize "$OUT_DIR/02-events.first.app.txt"
rm -f "$OUT_DIR/02-events.app.txt"

log "실행 $REPEAT 번 요약 (JVM 시작 기준 경과 ms)"
summarize "$METRICS"
