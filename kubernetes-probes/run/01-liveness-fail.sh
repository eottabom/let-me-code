#!/usr/bin/env bash
# 시나리오 1. 한 Pod 의 liveness 만 BROKEN 으로 바꾸면 무슨 일이 일어나는가
source "$(dirname "$0")/lib.sh"
k apply -f "$DEMO_DIR/k8s/app.yaml" >/dev/null
k rollout status deploy/probes-app --timeout=180s >/dev/null
wait_replicas
ensure_client
target=$(k get pods -l app=probes-app -o jsonpath='{.items[0].metadata.name}')
log "대상 Pod: $target, IP $(k get pod "$target" -o jsonpath='{.status.podIP}')"
log "liveness BROKEN"
call_pod "$target" POST '/chaos/liveness?broken=true'
observe 45
log "이벤트"
k get events --field-selector "involvedObject.name=$target" --sort-by=.lastTimestamp -o custom-columns='TIME:.lastTimestamp,REASON:.reason,MSG:.message' | tail -8
