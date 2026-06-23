#!/usr/bin/env bash
# TO-BE(Istio Gateway) -> AS-IS(gateway-simple) 복원. port-forward 는 별도로 stop-port-forward.sh 로 종료.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

cp "${BASE}/external/alb-templates/as-is-simple.conf" "${BASE}/external/alb-conf/default.conf"
docker compose -f "${BASE}/docker-compose.yml" restart alb

echo "AS-IS(gateway-simple) 복원 완료"
echo "curl http://localhost:8080/api"
