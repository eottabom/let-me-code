#!/usr/bin/env bash
# E5. 커넥션 풀이 첫 커넥션을 맺는 시점.
# JDBC 만 쓸 때(기본), JPA 를 켰을 때(jpa 프로파일), JPA 에서 시작할 때 DB 에 연결하는 두 곳을 껐을 때를 비교한다.
# 두 곳은 Spring Boot 의 ddl-auto 기본값 결정(내장 DB 확인)과 Hibernate 의 JDBC 메타데이터 조회다.
# 경우마다 앱을 REPEAT 번 새로 띄워 Hikari 풀 시작 로그가 Started 로그보다 얼마나 앞이나 뒤에 찍히는지,
# 그리고 /users 첫 요청과 반복 요청 시간을 잰다. /hello 를 먼저 한 번 불러 Spring MVC 쪽 첫 요청 비용은 빼 둔다.
source "$(dirname "$0")/lib.sh"

METRICS="$OUT_DIR/05-connection-pool.metrics.tsv"
: >"$METRICS"

discard_run
for case_name in JDBC JPA JPA-ddl-auto-지정-메타데이터-조회-끔; do
	for run in $(seq 1 "$REPEAT"); do
		log "$case_name 실행 $run/$REPEAT"
		case $case_name in
			JDBC) start_app 05-connection-pool ;;
			JPA) SPRING_PROFILES_ACTIVE=jpa start_app 05-connection-pool ;;
			*) SPRING_PROFILES_ACTIVE=jpa,jpa-no-boot-connection start_app 05-connection-pool ;;
		esac
		wait_ready
		sleep 0.5
		curl -s -o /dev/null "$BASE_URL/hello"
		pool_before=$(grep -c "HikariPool-1 - Start completed" "$APP_LOG" || true)
		printf '%s Ready 때 풀 시작 여부 (1 이면 시작됨)\t%s\n' "$case_name" "$pool_before" >>"$METRICS"
		call_repeatedly /users/1 20 >"$OUT_DIR/05-curl.txt"
		# /hello 한 번을 빼고 /users 요청만 센다.
		grep -E '^\[request\] .* path=/users/' "$APP_LOG" >"$OUT_DIR/05-users.txt"
		python3 "$DEMO_DIR/run/analyze.py" requests "$case_name /users" "$OUT_DIR/05-users.txt" "$OUT_DIR/05-curl.txt" >>"$METRICS"
		started_metrics | sed "s/^/$case_name /" >>"$METRICS"
		# 풀이 시작 과정에서 만들어진 경우에만 Started 로그보다 얼마나 앞이었는지 남긴다.
		if [ "$pool_before" = 1 ]; then
			started_line=$(grep "Started StartupApplication" "$APP_LOG")
			pool_line=$(grep "HikariPool-1 - Start completed" "$APP_LOG" | head -1)
			printf '%s 풀 시작이 Started 로그보다 앞선 시간\t%s\n' "$case_name" \
				"$(($(log_time_ms "${started_line%% *}") - $(log_time_ms "${pool_line%% *}")))" >>"$METRICS"
		fi
		if [ "$run" = 1 ]; then
			log "$case_name 첫 실행: 풀 시작, Started, 첫 /users 요청 로그 순서"
			grep -E "HikariPool-1 - (Starting|Start completed)|Started StartupApplication|^\[request\] seq=2 " "$APP_LOG" | head -6
			cp "$APP_LOG" "$OUT_DIR/05-connection-pool.$case_name.first.app.txt"
			sanitize "$OUT_DIR/05-connection-pool.$case_name.first.app.txt"
		fi
		stop_app
	done
done
rm -f "$OUT_DIR/05-curl.txt" "$OUT_DIR/05-users.txt" "$OUT_DIR/05-connection-pool.app.txt"

log "실행 $REPEAT 번씩 요약 (ms)"
summarize "$METRICS"
