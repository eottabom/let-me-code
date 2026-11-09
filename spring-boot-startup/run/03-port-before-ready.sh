#!/usr/bin/env bash
# E3. 포트는 열렸지만 아직 Ready 가 아닌 구간.
# Runner 를 5초 걸리게 두고 100ms 마다 /hello 와 liveness, readiness 를 불러 응답 코드가 바뀌는 시점을 남긴다.
# 앱을 REPEAT 번 새로 띄워 시점별 중앙값을 내고, 응답 코드 변화는 첫 실행 것만 남긴다.
source "$(dirname "$0")/lib.sh"

METRICS="$OUT_DIR/03-port-before-ready.metrics.tsv"
: >"$METRICS"

discard_run
for run in $(seq 1 "$REPEAT"); do
	log "실행 $run/$REPEAT (Runner 5초)"
	RUNNER_DELAY_SECONDS=5 start_app 03-port-before-ready
	[ "$run" = 1 ] && printf '%8s  %5s  %8s  %9s\n' "경과" "hello" "liveness" "readiness"
	previous=""
	port_open=""
	ready=""
	while true; do
		elapsed=$(($(now_ms) - APP_STARTED_MS))
		current="$(http_code /hello) $(http_code /actuator/health/liveness) $(http_code /actuator/health/readiness)"
		if [ "$current" != "$previous" ]; then
			read -r hello liveness readiness <<<"$current"
			[ "$run" = 1 ] && printf '%6sms  %5s  %8s  %9s\n' "$elapsed" "$hello" "$liveness" "$readiness"
			[ -z "$port_open" ] && [ "$hello" = 200 ] && port_open=$elapsed
			[ -z "$ready" ] && [ "$readiness" = 200 ] && ready=$elapsed
			previous=$current
		fi
		if [ -n "$ready" ] && [ $((elapsed - ready)) -ge 1000 ]; then
			break
		fi
		[ "$elapsed" -gt 60000 ] && break
		sleep 0.1
	done
	printf '/hello 첫 200 (실행 기준)\t%s\nreadiness 첫 200 (실행 기준)\t%s\n200 인데 Ready 아닌 구간\t%s\n' \
		"$port_open" "$ready" "$((ready - port_open))" >>"$METRICS"
	if [ "$run" = 1 ]; then
		log "첫 실행: 같은 시간대의 앱 로그"
		grep -E 'Tomcat started on port|Started StartupApplication|Runner 지연|AvailabilityChangeEvent' "$APP_LOG"
		cp "$APP_LOG" "$OUT_DIR/03-port-before-ready.first.app.txt"
	fi
	stop_app
done
sanitize "$OUT_DIR/03-port-before-ready.first.app.txt"
rm -f "$OUT_DIR/03-port-before-ready.app.txt"

log "실행 $REPEAT 번 요약 (ms, 폴링 간격 100ms 에 curl 세 번이 더해져 오차가 있다)"
summarize "$METRICS"
