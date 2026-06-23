#!/usr/bin/env bash
# 최초 1회 실행 - "ext_authz" 데모(AS-IS: gateway-authz+session-svc, TO-BE: Istio ext_authz) 환경을 세팅한다.
#
# [AS-IS] client -> nginx(ALB) -> spring-cloud-gateway-demo(profile=authz) -> session-svc(검증) -> B 서버
# [TO-BE] client -> nginx(ALB) -> port-forward -> Istio Gateway -> AuthorizationPolicy(CUSTOM ext_authz) -> B 서버
#
# 실행 순서:
# 1. ./run/run-authz-demo.sh
# 2. curl -v http://localhost:8080/api                              # AS-IS 차단 확인 (x-demo-user 없음 -> 403)
# 3. curl -H 'x-demo-user: alice' http://localhost:8080/api          # AS-IS 허용 확인
# 4. ./run/switch-authz-to-istio.sh                                  # TO-BE 전환
# 5. curl -v http://localhost:8080/api                              # TO-BE 차단 확인 (ext_authz -> 403)
# 6. curl -H 'x-demo-user: alice' http://localhost:8080/api          # TO-BE 허용 확인
# 7. ./run/switch-authz-to-gateway.sh                                # AS-IS 복원
# 8. ./run/stop-port-forward.sh istio-demo-authz                     # 정리
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

log "2. docker-compose 기동 (AS-IS: gateway-authz + session-svc + b)"
docker compose -f "${BASE}/docker-compose.yml" --profile authz up -d --build

log "3. nginx 설정을 AS-IS 로 맞춤"
cp "${BASE}/external/alb-templates/as-is-authz.conf" "${BASE}/external/alb-conf/default.conf"
docker compose -f "${BASE}/docker-compose.yml" restart alb

log "완료"
echo "차단 테스트: curl -v http://localhost:8080/api"
echo "허용 테스트: curl -H 'x-demo-user: alice' http://localhost:8080/api"
echo "TO-BE 전환:  ./run/switch-authz-to-istio.sh"
