#!/usr/bin/env bash
# E7. 첫 요청과 반복 요청의 응답 시간.
# API 하나마다 앱을 새로 띄워, Ready 뒤 같은 연결로 200 번 부른다. 다른 API 의 첫 요청 비용이 섞이지 않게 하려는 것이다.
# 1번째, 2~10번째, 11~200번째로 나눠 실행마다 중앙값을 내고, 다시 REPEAT 번의 중앙값, 최솟값, 최댓값으로 묶는다.
source "$(dirname "$0")/lib.sh"

METRICS="$OUT_DIR/07-first-vs-repeat.metrics.tsv"
# 그래프용으로 요청 하나하나의 서버 시간도 남긴다 (path, 실행 번호, 요청 순번, ms).
SERIES="$OUT_DIR/07-first-vs-repeat.series.tsv"
: >"$METRICS"
printf 'path\trun\tseq\tserver_ms\n' >"$SERIES"

discard_run
for path in /hello /users/1 /external; do
	for run in $(seq 1 "$REPEAT"); do
		log "$path 실행 $run/$REPEAT"
		start_app 07-first-vs-repeat
		wait_ready
		sleep 0.5
		call_repeatedly "$path" 200 >"$OUT_DIR/07-curl.txt"
		python3 "$DEMO_DIR/run/analyze.py" requests "$path" "$APP_LOG" "$OUT_DIR/07-curl.txt" >>"$METRICS"
		sed -nE 's/^\[request\] seq=([0-9]+) .* server=([0-9]+)us$/\1 \2/p' "$APP_LOG" \
			| awk -v path="$path" -v run="$run" '{ printf "%s\t%s\t%s\t%.3f\n", path, run, $1, $2 / 1000 }' >>"$SERIES"
		stop_app
	done
done
rm -f "$OUT_DIR/07-curl.txt" "$OUT_DIR/07-first-vs-repeat.app.txt"

log "실행 $REPEAT 번씩 요약 (ms, 실행마다 구간 중앙값을 낸 뒤 다시 묶은 값)"
summarize "$METRICS"
