#!/usr/bin/env bash
# AS-IS(gateway-simple) -> TO-BE(Istio Gateway) 전환.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

NS="istio-demo"

log "1. host IP 조회"
HOST_IP="$(resolve_host_ip)"
echo "host.docker.internal -> ${HOST_IP}"

log "2. Gateway API CRD 확인"
ensure_gateway_api_crds

log "3. k8s 매니페스트 적용"
${KCTL} apply -f "${BASE}/k8s/simple/00-namespace.yaml"
apply_with_host_ip "${BASE}/k8s/simple/10-external-service.yaml" "${HOST_IP}"
${KCTL} apply -f "${BASE}/k8s/simple/20-gateway-httproute.yaml"

log "4. Istio Gateway Deployment 대기"
wait_for_deployment "${NS}" "demo-gw-istio"

log "5. port-forward 시작 (:${PF_PORT})"
start_port_forward "${NS}"

log "6. nginx 설정을 TO-BE 로 전환"
cp "${BASE}/external/alb-templates/to-be-simple.conf" "${BASE}/external/alb-conf/default.conf"
docker compose -f "${BASE}/docker-compose.yml" restart alb

echo ""
echo "Istio Gateway 라우팅 적용 완료"
echo "curl http://localhost:8080/api  # prefix 없이 A 서버 응답 그대로 와야 함"
