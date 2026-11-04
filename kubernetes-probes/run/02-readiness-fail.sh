#!/usr/bin/env bash
# 시나리오 2. 한 Pod 의 readiness 만 REFUSING_TRAFFIC 으로 바꾸면 무슨 일이 일어나는가
source "$(dirname "$0")/lib.sh"
k rollout restart deploy/probes-app >/dev/null; k rollout status deploy/probes-app --timeout=180s >/dev/null
# rollout status 가 끝나도 이전 Pod 가 종료 중으로 남아 있어서, 대상 Pod 로 잘못 고르지 않도록 사라질 때까지 기다린다.
wait_replicas
ensure_client
target=$(k get pods -l app=probes-app -o jsonpath='{.items[0].metadata.name}')
log "대상 Pod: $target, IP $(k get pod "$target" -o jsonpath='{.status.podIP}')"

service_split() {
	k exec client -- sh -c 'for i in $(seq 1 40); do curl -s http://probes-app/hello; echo; done' | sort | uniq -c
}
log "변경 전 Service 로 40번 요청"; service_split
log "readiness REFUSING_TRAFFIC"
call_pod "$target" POST '/chaos/readiness?refuse=true'
observe 25
log "변경 후 Service 로 40번 요청"; service_split
log "다시 ACCEPTING_TRAFFIC"
call_pod "$target" POST '/chaos/readiness?refuse=false'
observe 12
