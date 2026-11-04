#!/usr/bin/env bash
# 공통 함수. 각 시나리오 스크립트가 source 해서 쓴다.
set -euo pipefail
CLUSTER="${CLUSTER:-probes-lab}"
NS=probes
DEMO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# 시나리오 출력은 화면과 results/latest/<스크립트 이름>.log 에 함께 남긴다.
if [[ "$(basename "$0")" =~ ^0[1-7]- ]]; then
	mkdir -p "$DEMO_DIR/results/latest"
	exec > >(tee "$DEMO_DIR/results/latest/$(basename "$0" .sh)${1:+-$1}.log") 2>&1
fi

k() { kubectl --context "kind-$CLUSTER" -n "$NS" "$@"; }
log() { printf '\n[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

# 클러스터 안에서 요청을 보내는 curl Pod
ensure_client() {
	if ! k get pod client >/dev/null 2>&1; then
		k run client --image=curlimages/curl:8.11.1 --restart=Never --command -- sleep infinity >/dev/null
	fi
	k wait --for=condition=Ready pod/client --timeout=120s >/dev/null
}

# 종료 중인 이전 Pod 가 모두 사라져서 Pod 수가 replicas 와 같아질 때까지 기다린다.
wait_replicas() {
	local want
	want=$(k get deploy/probes-app -o jsonpath='{.spec.replicas}')
	for _ in $(seq 1 90); do
		[ "$(k get pods -l app=probes-app --no-headers 2>/dev/null | wc -l | tr -d ' ')" -eq "$want" ] && return 0
		sleep 2
	done
	echo "Pod 수가 $want 개로 줄지 않았다" >&2
	return 1
}

# 특정 Pod 에 직접 요청 (Pod IP 로)
call_pod() {
	local pod=$1 method=$2 path=$3
	local ip
	ip=$(k get pod "$pod" -o jsonpath='{.status.podIP}')
	k exec client -- curl -s -X "$method" "http://$ip:8080$path"
	echo
}

# 초마다 Pod 상태와 Service 엔드포인트(ready 인 IP)를 한 줄로 기록한다.
observe() {
	local seconds=$1 label=${2:-app=probes-app} start
	start=$(date +%s)
	for _ in $(seq 1 "$seconds"); do
		local pods eps
		pods=$(k get pods -l "$label" --no-headers -o custom-columns='N:.metadata.name,R:.status.containerStatuses[0].ready,C:.status.containerStatuses[0].restartCount,S:.status.containerStatuses[0].state' 2>/dev/null \
			| awk '{st=$4; sub(/map\[/,"",st); sub(/:.*/,"",st); printf "%s ready=%s restarts=%s %s | ", substr($1,length($1)-4), $2, $3, st}')
		eps=$(k get endpointslices -l kubernetes.io/service-name=probes-app -o jsonpath='{range .items[*].endpoints[?(@.conditions.ready==true)]}{.addresses[0]} {end}' 2>/dev/null)
		printf '+%3ss  %s endpoints[%s]\n' "$(( $(date +%s) - start ))" "$pods" "$eps"
		sleep 1
	done
}
