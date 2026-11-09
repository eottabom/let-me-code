#!/usr/bin/env bash
# 공통 함수. 각 실험 스크립트가 source 해서 쓴다.
set -euo pipefail
DEMO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="$DEMO_DIR/app/build/libs/app-0.0.1-SNAPSHOT.jar"
# JAVA_HOME 을 지정하면 그 JDK 로 앱을 띄운다. 블로그 측정은 Azul Zulu 25.0.2 로 했다.
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
BASE_URL=http://localhost:8080
OUT_DIR="$DEMO_DIR/results/latest"
mkdir -p "$OUT_DIR"
# 실험마다 앱을 새로 띄우는 횟수. 첫 실행은 디스크 캐시가 비어 있을 수 있어 따로 한 번 더 띄우고 버린다.
REPEAT="${REPEAT:-10}"

# 실험 출력은 화면과 results/latest/<스크립트 이름>.txt 에 함께 남긴다.
# 저장소 루트 .gitignore 가 *.log 를 제외하므로 결과 파일은 .txt 로 쓴다.
if [[ "$(basename "$0")" =~ ^0[1-8]- ]]; then
	exec > >(tee "$OUT_DIR/$(basename "$0" .sh).txt") 2>&1
	echo "JDK: $("$JAVA_BIN" -version 2>&1 | sed -n 2p)"
fi

log() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

# 현재 시각(ms). macOS date 는 밀리초를 못 내서 python 을 쓴다.
now_ms() { python3 -c 'import time; print(int(time.time() * 1000))'; }

# 앱을 띄운다. 첫 인자는 앱 로그 이름, 나머지는 JVM 옵션. 프로파일과 설정은 환경 변수로 넘긴다.
start_app() {
	local name=$1
	shift
	# 이전 실행이 남아 포트를 잡고 있으면 그 앱이 대신 응답하므로 바로 멈춘다.
	if curl -s -o /dev/null --max-time 1 "$BASE_URL/actuator/health"; then
		echo "8080 포트에 이미 다른 앱이 떠 있다" >&2
		return 1
	fi
	APP_LOG="$OUT_DIR/$name.app.txt"
	APP_STARTED_MS=$(now_ms)
	"$JAVA_BIN" "$@" -jar "$JAR" >"$APP_LOG" 2>&1 &
	APP_PID=$!
}

# 스크립트가 중간에 실패해도 띄운 앱이 남지 않게 한다.
trap 'if [ -n "${APP_PID:-}" ]; then kill "$APP_PID" 2>/dev/null || true; fi' EXIT

http_code() { curl -s -o /dev/null -w '%{http_code}' --max-time 2 "$BASE_URL$1" || true; }

wait_ready() {
	for _ in $(seq 1 600); do
		[ "$(http_code /actuator/health/readiness)" = 200 ] && return 0
		sleep 0.1
	done
	echo "readiness 가 60초 안에 200 이 되지 않았다" >&2
	return 1
}

# readiness 를 부르지 않고 앱 로그의 ApplicationReadyEvent 로 Ready 를 기다린다.
# 앱이 받는 첫 HTTP 요청을 실험 요청으로 만들고 싶을 때 쓴다.
wait_ready_log() {
	for _ in $(seq 1 600); do
		grep -q 'ApplicationReadyEvent' "$APP_LOG" && return 0
		sleep 0.1
	done
	echo "60초 안에 ApplicationReadyEvent 가 찍히지 않았다" >&2
	return 1
}

# 앱을 끄고, 앱 로그에서 개인 경로와 사용자 이름을 지운다.
stop_app() {
	kill "$APP_PID" 2>/dev/null || true
	wait "$APP_PID" 2>/dev/null || true
	sanitize "$APP_LOG"
}

sanitize() {
	local user
	user=$(whoami)
	# 시작 로그의 "with PID ... (경로 started by 사용자 in 경로)" 줄은 통째로 뺀다.
	sed -i.bak -e '/Starting StartupApplication using Java/d' -e "s|$HOME|~|g" -e "s|$user|user|g" "$1"
	rm -f "$1.bak"
}

# 기록하지 않는 실행을 한 번 해서 jar 와 JDK 파일을 디스크 캐시에 올려 둔다.
discard_run() {
	log "버리는 실행 1번"
	start_app discard
	wait_ready
	stop_app
	rm -f "$OUT_DIR/discard.app.txt"
}

# 반복 실행에서 모은 metric<TAB>value 를 지표별 중앙값, 최솟값, 최댓값으로 묶는다.
summarize() { python3 "$DEMO_DIR/run/analyze.py" stats <"$1"; }

# Started 로그의 두 시간(초)을 ms 로 뽑는다.
started_metrics() {
	sed -nE 's/.*Started StartupApplication in ([0-9.]+) seconds \(process running for ([0-9.]+)\).*/\1 \2/p' "$APP_LOG" \
		| awk '{ printf "Started 로그 (애플리케이션)\t%d\nStarted 로그 (JVM 포함)\t%d\n", $1 * 1000, $2 * 1000 }'
}

# 같은 연결로 경로를 횟수만큼 차례로 부르고, 요청마다 curl 이 잰 전체 시간(초)을 한 줄씩 쓴다.
call_repeatedly() {
	local path=$1 count=$2 urls=()
	for _ in $(seq 1 "$count"); do
		# -o 는 URL 하나에만 걸리므로 URL 마다 붙인다.
		urls+=(-o /dev/null "$BASE_URL$path")
	done
	curl -s -w '%{time_total}\n' "${urls[@]}"
}

# 로그 줄 앞의 시각(2026-11-07T09:50:34.960+09:00)을 epoch ms 로 바꾼다.
log_time_ms() {
	python3 -c 'import sys, datetime; print(int(datetime.datetime.fromisoformat(sys.argv[1]).timestamp() * 1000))' "$1"
}

# 앱 로그에서 [timeline], [request] 줄을 뽑는다.
timeline() { grep -E '^\[timeline\]' "$APP_LOG"; }
requests() { grep -E '^\[request\]' "$APP_LOG"; }
