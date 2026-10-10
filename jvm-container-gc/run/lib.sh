#!/usr/bin/env bash
# 공통 변수와 함수. 다른 스크립트에서 source 한다.
set -euo pipefail
DEMO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KIND_CLUSTER="${KIND_CLUSTER:-jvm-gc}"
KCTL="kubectl --context kind-${KIND_CLUSTER} -n jvm-gc"
IMAGE="jvm-gc-demo:1"

log() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

# deploy_app <name> <cpu-limit> <memory-limit> [jvm-gc-opts] : 적용 후 Ready 까지 기다린다.
deploy_app() {
	APP_NAME="$1" CPU="$2" MEMORY="$3" JVM_GC_OPTS="${4:-}" \
		envsubst '${APP_NAME} ${CPU} ${MEMORY} ${JVM_GC_OPTS}' < "$DEMO_DIR/k8s/app.yaml" | $KCTL apply -f - >/dev/null
	$KCTL rollout status "deploy/$1" --timeout=180s >/dev/null
}

# app_pod <name>
app_pod() {
	$KCTL get pod -l "app=$1" --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}'
}

# 실험 대상 Deployment 를 모두 지워 다른 조건과 동시에 실행되지 않게 한다.
clear_apps() {
	$KCTL delete deploy,svc -l role=gc-demo --ignore-not-found >/dev/null
	$KCTL wait --for=delete pod -l role=gc-demo --timeout=120s >/dev/null 2>&1 || true
}

# start_load <job-name> <target-service> <rate> <warmup> <measure> : k6 Pod 가 Running 이 될 때까지 기다린다.
start_load() {
	local job="$1"
	$KCTL delete job "$job" --ignore-not-found >/dev/null
	JOB_NAME="$job" TARGET="http://$2:8080" RATE="$3" WARMUP="$4" MEASURE="$5" \
		envsubst '${JOB_NAME} ${TARGET} ${RATE} ${WARMUP} ${MEASURE}' < "$DEMO_DIR/k8s/load/k6-job.yaml" | $KCTL apply -f - >/dev/null
	$KCTL wait --for=condition=Ready pod -l "job-name=$job" --timeout=120s >/dev/null
}

# wait_load <job-name> : 완료 후 k6 summary JSON 한 줄을 출력한다.
wait_load() {
	$KCTL wait --for=condition=complete "job/$1" --timeout=3600s >/dev/null
	$KCTL logs "job/$1" | grep '^K6_SUMMARY ' | sed 's/^K6_SUMMARY //'
}
