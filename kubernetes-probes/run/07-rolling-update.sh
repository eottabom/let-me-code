#!/usr/bin/env bash
# 시나리오 7. 롤링 업데이트 중 요청이 실패하는가 (preStop 대기 없음 / 있음)
source "$(dirname "$0")/lib.sh"
ensure_client
k delete pod tcp-probe exec-probe grpc-probe --ignore-not-found --wait=false >/dev/null
k delete deploy/probes-app --ignore-not-found >/dev/null
k apply -f "$DEMO_DIR/k8s/app.yaml" >/dev/null
k scale deploy/probes-app --replicas=3 >/dev/null
k rollout status deploy/probes-app --timeout=180s >/dev/null

run_case() {
	local name=$1
	log "$name: 0.1초마다 요청을 보내면서 rollout restart"
	k exec client -- sh -c 'end=$(( $(date +%s) + 60 )); ok=0; fail=0; while [ $(date +%s) -lt $end ]; do
		if curl -s -o /dev/null --max-time 2 -w "%{http_code}" http://probes-app/hello | grep -q 200; then ok=$((ok+1)); else fail=$((fail+1)); fi
		sleep 0.1; done; echo "성공 $ok, 실패 $fail"' &
	local pid=$!
	sleep 5
	k rollout restart deploy/probes-app >/dev/null
	k rollout status deploy/probes-app --timeout=180s >/dev/null
	wait $pid
}

for i in 1 2 3; do run_case "A) preStop 없음 #$i"; done
k patch deploy/probes-app --type=json -p='[{"op":"add","path":"/spec/template/spec/containers/0/lifecycle","value":{"preStop":{"sleep":{"seconds":5}}}}]' >/dev/null
k rollout status deploy/probes-app --timeout=180s >/dev/null
for i in 1 2 3; do run_case "B) preStop sleep 5초 #$i"; done
