#!/usr/bin/env bash
# 데모 환경 전체 초기화: docker-compose, port-forward, k8s 네임스페이스, kind 클러스터까지 모두 삭제.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

log "port-forward 정리"
stop_port_forward "istio-demo"
stop_port_forward "istio-demo-authz"

log "docker compose 정리 (simple, authz 프로필 모두)"
docker compose -f "${BASE}/docker-compose.yml" --profile simple --profile authz down --remove-orphans

log "nginx 설정 초기화"
rm -f "${BASE}/external/alb-conf/default.conf"

log "kind 클러스터 삭제 ('${KIND_CLUSTER}')"
if kind get clusters 2>/dev/null | grep -qx "${KIND_CLUSTER}"; then
	kind delete cluster --name "${KIND_CLUSTER}"
else
	echo "kind 클러스터 '${KIND_CLUSTER}' 가 이미 없음"
fi

echo "전체 초기화 완료"
