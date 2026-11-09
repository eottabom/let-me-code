#!/usr/bin/env bash
# E4. Spring 이 첫 요청으로 미루는 초기화.
# 기본값, DispatcherServlet 을 시작 때 초기화(load-on-startup=1), 빈 생성을 미룸(lazy-initialization) 세 경우를 비교한다.
# 경우마다 앱을 REPEAT 번 새로 띄워 Started 시간, DispatcherServlet 초기화 시점, Ready 뒤 /hello 첫 요청과 반복 요청 시간을 잰다.
# readiness 확인 요청도 DispatcherServlet 을 거치므로, 초기화가 어떤 요청 때 일어났는지도 남긴다.
source "$(dirname "$0")/lib.sh"

METRICS="$OUT_DIR/04-deferred-init.metrics.tsv"
: >"$METRICS"

discard_run
for case_name in 기본값 load-on-startup lazy; do
	for run in $(seq 1 "$REPEAT"); do
		log "$case_name 실행 $run/$REPEAT"
		case $case_name in
			기본값) start_app 04-deferred-init ;;
			load-on-startup) SPRING_MVC_SERVLET_LOAD_ON_STARTUP=1 start_app 04-deferred-init ;;
			lazy) SPRING_PROFILES_ACTIVE=lazy start_app 04-deferred-init ;;
		esac
		wait_ready
		ready_ms=$(now_ms)
		sleep 0.5
		call_repeatedly /hello 20 >"$OUT_DIR/04-curl.txt"
		started_metrics | sed "s/^/$case_name /" >>"$METRICS"
		python3 "$DEMO_DIR/run/analyze.py" requests "$case_name /hello" "$APP_LOG" "$OUT_DIR/04-curl.txt" >>"$METRICS"
		# DispatcherServlet 초기화 로그: "Completed initialization in N ms"
		init_line=$(grep -E "Completed initialization in [0-9]+ ms" "$APP_LOG" | head -1 || true)
		if [ -n "$init_line" ]; then
			printf '%s DispatcherServlet 초기화 시간\t%s\n' "$case_name" "$(sed -nE 's/.*Completed initialization in ([0-9]+) ms.*/\1/p' <<<"$init_line")" >>"$METRICS"
			init_ms=$(log_time_ms "${init_line%% *}")
			printf '%s DispatcherServlet 초기화 시점 (Ready 확인 기준)\t%s\n' "$case_name" "$((init_ms - ready_ms))" >>"$METRICS"
		fi
		if [ "$run" = 1 ]; then
			log "$case_name 첫 실행: DispatcherServlet 초기화와 Started, Ready 로그 순서"
			grep -E "Initializing Servlet|Completed initialization|Started StartupApplication|ApplicationReadyEvent|^\[request\] seq=[12] " "$APP_LOG"
			cp "$APP_LOG" "$OUT_DIR/04-deferred-init.$case_name.first.app.txt"
			sanitize "$OUT_DIR/04-deferred-init.$case_name.first.app.txt"
		fi
		stop_app
	done
done
rm -f "$OUT_DIR/04-curl.txt" "$OUT_DIR/04-deferred-init.app.txt"

log "실행 $REPEAT 번씩 요약 (ms). 초기화 시점이 음수면 readiness 가 200 이 되기 전에 초기화된 것이다"
summarize "$METRICS"
