#!/usr/bin/env bash
# 시나리오 5. 응답이 1.5초 걸리는 엔드포인트를 liveness 로 쓰면 (timeoutSeconds 기본값 1초)
source "$(dirname "$0")/lib.sh"
k delete deploy/probes-app --ignore-not-found >/dev/null
k apply -f "$DEMO_DIR/k8s/app.yaml" >/dev/null
k scale deploy/probes-app --replicas=1 >/dev/null
k rollout status deploy/probes-app --timeout=180s >/dev/null
for timeout in 1 3; do
	log "liveness = /slow?millis=1500, timeoutSeconds=$timeout"
	k patch deploy/probes-app --type=json -p="[{\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/livenessProbe\",\"value\":{\"httpGet\":{\"path\":\"/slow?millis=1500\",\"port\":8080},\"periodSeconds\":5,\"failureThreshold\":3,\"timeoutSeconds\":$timeout}}]" >/dev/null
	k rollout status deploy/probes-app --timeout=180s >/dev/null
	sleep 5
	pod=$(k get pods -l app=probes-app --sort-by=.metadata.creationTimestamp -o jsonpath='{.items[-1:].metadata.name}')
	sleep 40
	k get pod "$pod" --no-headers -o custom-columns='POD:.metadata.name,RESTARTS:.status.containerStatuses[0].restartCount'
	k get events --field-selector "involvedObject.name=$pod" -o custom-columns='REASON:.reason,MSG:.message' | grep -E 'Liveness|Killing' | sort -u | head -3
done
