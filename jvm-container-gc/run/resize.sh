#!/usr/bin/env bash
# 같은 Deployment 의 resources 만 바꿔 Pod 를 재생성한다. 부하는 처음부터 끝까지 같은 RPS 로 유지한다.
source "$(dirname "$0")/lib.sh"
out="${1:-$DEMO_DIR/results/resize}"
rate="${RATE:-300}" before_s="${BEFORE_SECONDS:-360}" after_s="${AFTER_SECONDS:-360}"
mkdir -p "$out"
clear_apps
deploy_app app 8 4Gi ""
start_load load-app app "$rate" 10s "$((before_s + after_s))s"
echo "LOAD_START $(date +%s)" > "$out/timeline.txt"
sleep "$before_s"
$KCTL logs "$(app_pod app)" | grep -E 'Using (Serial|G1)|Heap Max|Parallel Workers' > "$out/before-gc-init.txt"
log "resources 변경: 8 CPU / 4Gi → 1 CPU / 1Gi"
echo "RESIZE $(date +%s)" >> "$out/timeline.txt"
$KCTL set resources deploy/app --requests=cpu=500m,memory=1Gi --limits=cpu=1,memory=1Gi >/dev/null
$KCTL rollout status deploy/app --timeout=300s
echo "RESIZED $(date +%s)" >> "$out/timeline.txt"
wait_load load-app > "$out/k6.json"
echo "LOAD_END $(date +%s)" >> "$out/timeline.txt"
$KCTL logs "$(app_pod app)" | grep -E 'Using (Serial|G1)|Heap Max|Parallel Workers' > "$out/after-gc-init.txt"
cat "$out/timeline.txt"
