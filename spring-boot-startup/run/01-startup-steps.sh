#!/usr/bin/env bash
# E1. 시작 단계별 소요 시간. /actuator/startup 의 단계 기록에서 주요 단계 시간을 뽑는다.
# 앱을 REPEAT 번 새로 띄워 지표별 중앙값을 내고, 상세 단계 목록은 첫 실행 것만 남긴다.
source "$(dirname "$0")/lib.sh"

# 전체 흐름을 나누는 주요 단계. 서로 겹치거나 포함하는 단계도 있다.
MAIN_STEPS='["spring.boot.application.environment-prepared", "spring.context.config-classes.parse",
	"spring.context.beans.post-process", "spring.boot.webserver.create", "spring.context.refresh"]'
METRICS="$OUT_DIR/01-startup-steps.metrics.tsv"
: >"$METRICS"

discard_run
for run in $(seq 1 "$REPEAT"); do
	log "실행 $run/$REPEAT"
	start_app 01-startup-steps
	wait_ready
	curl -s "$BASE_URL/actuator/startup" >"$OUT_DIR/01-startup-steps.json"
	started_metrics >>"$METRICS"
	# duration 은 "PT0.123456S" 형식이다.
	jq -r --argjson steps "$MAIN_STEPS" '
		def ms: sub("^PT"; "") | sub("S$"; "") | tonumber * 1000;
		.timeline.events as $events
		| ($steps[] as $step | $events[] | select(.startupStep.name == $step) | "\($step)\t\(.duration | ms | floor)"),
		  "spring.beans.instantiate (합계)\t\([$events[] | select(.startupStep.name == "spring.beans.instantiate") | .duration | ms] | add | floor)",
		  "spring.beans.instantiate (개수)\t\([$events[] | select(.startupStep.name == "spring.beans.instantiate")] | length)"' \
		"$OUT_DIR/01-startup-steps.json" >>"$METRICS"
	if [ "$run" = 1 ]; then
		cp "$OUT_DIR/01-startup-steps.json" "$OUT_DIR/01-startup-steps.first.json"
		log "첫 실행: 오래 걸린 단계 상위 15개 (ms, 단계 이름, 대상)"
		jq -r '
			def ms: sub("^PT"; "") | sub("S$"; "") | tonumber * 1000 | floor;
			[.timeline.events[] | {ms: (.duration | ms), name: .startupStep.name,
				target: ([.startupStep.tags[]? | select(.key == "beanName" or .key == "postProcessor" or .key == "name") | .value] | first // "" | sub("@[0-9a-f]+$"; ""))}]
			| sort_by(-.ms) | .[:15][] | "\(.ms)\t\(.name)\t\(.target)"' "$OUT_DIR/01-startup-steps.json"
		cp "$APP_LOG" "$OUT_DIR/01-startup-steps.first.app.txt"
	fi
	stop_app
done
rm -f "$OUT_DIR/01-startup-steps.json"
sanitize "$OUT_DIR/01-startup-steps.first.app.txt"
rm -f "$OUT_DIR/01-startup-steps.app.txt"

log "실행 $REPEAT 번 요약 (ms)"
summarize "$METRICS"
