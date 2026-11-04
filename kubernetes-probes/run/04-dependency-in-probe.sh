#!/usr/bin/env bash
# 시나리오 4. DB(외부 의존성) 체크를 liveness 에 넣었을 때와 readiness 에 넣었을 때
#   사용법: 04-dependency-in-probe.sh liveness | readiness
source "$(dirname "$0")/lib.sh"
group=${1:?liveness 또는 readiness}
k delete deploy/probes-app --ignore-not-found >/dev/null
k apply -f "$DEMO_DIR/k8s/fake-db.yaml" >/dev/null
k scale deploy/db --replicas=1 >/dev/null
k rollout status deploy/db --timeout=120s >/dev/null
k apply -f "$DEMO_DIR/k8s/app.yaml" >/dev/null
k scale deploy/probes-app --replicas=3 >/dev/null
if [[ $group == liveness ]]; then
	k set env deploy/probes-app DEPENDENCY_HOST=db PROBES_LIVENESS_INCLUDE=livenessState,dependency >/dev/null
else
	k set env deploy/probes-app DEPENDENCY_HOST=db PROBES_READINESS_INCLUDE=readinessState,dependency >/dev/null
fi
k rollout status deploy/probes-app --timeout=180s >/dev/null
log "$group 그룹에 DB 체크 포함. DB 를 내린다"
k scale deploy/db --replicas=0 >/dev/null
observe 60
log "DB 복구"
k scale deploy/db --replicas=1 >/dev/null
observe 40
