#!/usr/bin/env bash
# docker-compose 애플리케이션 + port-forward 만 정리한다. kind 클러스터/Istio 설치는 그대로 유지.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

log "port-forward 정리"
stop_port_forward "istio-demo"
stop_port_forward "istio-demo-authz"

log "docker compose 정리 (simple, authz 프로필 모두)"
docker compose -f "${BASE}/docker-compose.yml" --profile simple --profile authz down --remove-orphans

echo "애플리케이션 정리 완료 (kind 클러스터 '${KIND_CLUSTER}' 는 유지됨)"
