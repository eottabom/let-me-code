#!/usr/bin/env bash
# 최초 1회 실행 - "단순 라우팅" 데모(AS-IS: gateway-simple, TO-BE: Istio Gateway) 환경을 세팅한다.
#
# [AS-IS] client -> nginx(ALB) -> spring-cloud-gateway-demo(profile=simple) -> A 서버
# [TO-BE] client -> nginx(ALB) -> port-forward -> Istio Gateway -> HTTPRoute -> A 서버(Endpoints)
#
# 실행 순서:
# 1. ./run/run-simple-demo.sh
# 2. curl http://localhost:8080/api                  # AS-IS 확인 ("Gateway Result -> ..." prefix)
# 3. ./run/switch-simple-to-istio.sh                  # TO-BE 전환
# 4. curl http://localhost:8080/api                  # TO-BE 확인 (prefix 없음, A 서버 응답 그대로)
# 5. ./run/switch-simple-to-gateway.sh                # AS-IS 복원
# 6. ./run/stop-port-forward.sh istio-demo            # 정리
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

require docker
require kind
require kubectl
require istioctl

log "0. kind 클러스터 확인"
ensure_kind_cluster

log "1. Istio Ambient profile 설치"
istioctl install --context "kind-${KIND_CLUSTER}" --set profile=ambient -y

log "2. docker-compose 기동 (AS-IS: gateway-simple)"
docker compose -f "${BASE}/docker-compose.yml" --profile simple up -d --build

log "3. nginx 설정을 AS-IS 로 맞춤"
cp "${BASE}/external/alb-templates/as-is-simple.conf" "${BASE}/external/alb-conf/default.conf"
docker compose -f "${BASE}/docker-compose.yml" restart alb

log "완료"
echo "AS-IS 테스트: curl http://localhost:8080/api"
echo "TO-BE 전환:   ./run/switch-simple-to-istio.sh"
