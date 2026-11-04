#!/usr/bin/env bash
# 시나리오 3. 시작에 40초 걸리는 애플리케이션
#   A) liveness 만 있을 때 (15초 안에 못 뜨면 재시작) → 계속 죽는지
#   B) startupProbe(5초 x 12번 = 최대 60초)를 추가했을 때
source "$(dirname "$0")/lib.sh"
k apply -f "$DEMO_DIR/k8s/app.yaml" >/dev/null
k scale deploy/probes-app --replicas=1 >/dev/null

log "A) STARTUP_DELAY_SECONDS=40, startupProbe 없음"
k set env deploy/probes-app STARTUP_DELAY_SECONDS=40 >/dev/null
sleep 3
observe 100
k get events --sort-by=.lastTimestamp -o custom-columns='REASON:.reason,MSG:.message' | grep -E 'Liveness|Killing|BackOff' | tail -4

log "B) startupProbe 추가"
k patch deploy/probes-app --type=json -p='[{"op":"add","path":"/spec/template/spec/containers/0/startupProbe","value":{"httpGet":{"path":"/actuator/health/liveness","port":8080},"periodSeconds":5,"failureThreshold":12}}]' >/dev/null
sleep 3
observe 70
