#!/usr/bin/env bash
# TO-BE(Istio ext_authz) -> AS-IS(gateway-authz + session-svc) 복원. port-forward 는 별도로 stop-port-forward.sh 로 종료.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

cp "${BASE}/external/alb-templates/as-is-authz.conf" "${BASE}/external/alb-conf/default.conf"
docker compose -f "${BASE}/docker-compose.yml" restart alb

echo "AS-IS(gateway-authz) 복원 완료"
echo "curl -v http://localhost:8080/api"
echo "curl -H 'x-demo-user: alice' http://localhost:8080/api"
