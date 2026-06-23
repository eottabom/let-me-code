#!/usr/bin/env bash
# AS-IS(gateway-authz + session-svc) -> TO-BE(Istio Gateway + ext_authz) 전환.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

NS="istio-demo-authz"
IMAGE="istio-ambient-demo-authz-server:0.0.1"

log "1. authz-server 이미지 빌드 + kind 클러스터에 로드"
docker build -t "${IMAGE}" -f "${BASE}/authz-server/Dockerfile" "${BASE}/.."
kind load docker-image "${IMAGE}" --name "${KIND_CLUSTER}"

log "2. istiod ConfigMap 에 ext_authz provider 등록"
python3 - <<'PYEOF'
import subprocess, json

result = subprocess.run(
	["kubectl", "--context", "kind-istio-demo", "get", "configmap", "istio", "-n", "istio-system",
	 "-o", "jsonpath={.data.mesh}"],
	capture_output=True, text=True,
)
current = result.stdout
if "demo-authz-grpc" in current:
	print("demo-authz-grpc already configured")
else:
	extension = (
		"extensionProviders:\n"
		"- name: demo-authz-grpc\n"
		"  envoyExtAuthzGrpc:\n"
		"    service: authz-server.istio-demo-authz.svc.cluster.local\n"
		"    port: \"9001\"\n"
	)
	new_mesh = current.rstrip() + "\n" + extension
	patch = json.dumps({"data": {"mesh": new_mesh}})
	subprocess.run(
		["kubectl", "--context", "kind-istio-demo", "patch", "configmap", "istio", "-n", "istio-system",
		 "--type=merge", "-p", patch],
		check=True,
	)
	print("Mesh config patched: demo-authz-grpc added")
PYEOF

log "3. istiod 재시작"
${KCTL} rollout restart deployment/istiod -n istio-system
${KCTL} rollout status deployment/istiod -n istio-system --timeout=60s

log "4. Gateway API CRD 확인"
ensure_gateway_api_crds

log "5. host IP 조회"
HOST_IP="$(resolve_host_ip)"
echo "host.docker.internal -> ${HOST_IP}"

log "6. k8s 매니페스트 적용"
${KCTL} apply -f "${BASE}/k8s/authz/00-namespace.yaml"
${KCTL} apply -f "${BASE}/k8s/authz/10-authz-server.yaml"
apply_with_host_ip "${BASE}/k8s/authz/20-external-service.yaml" "${HOST_IP}"
${KCTL} apply -f "${BASE}/k8s/authz/30-gateway-httproute.yaml"
${KCTL} apply -f "${BASE}/k8s/authz/40-authorizationpolicy.yaml"

log "7. authz-server / Istio Gateway 기동 대기"
wait_for_deployment "${NS}" "authz-server"
wait_for_deployment "${NS}" "demo-gw-istio"

log "8. port-forward 시작 (:${PF_PORT})"
start_port_forward "${NS}"

log "9. nginx 설정을 TO-BE 로 전환"
cp "${BASE}/external/alb-templates/to-be-authz.conf" "${BASE}/external/alb-conf/default.conf"
docker compose -f "${BASE}/docker-compose.yml" restart alb

echo ""
echo "Istio ext_authz 적용 완료"
echo "차단 테스트: curl -v http://localhost:8080/api                       # -> 403"
echo "허용 테스트: curl -H 'x-demo-user: alice' http://localhost:8080/api  # -> B 서버 응답 (AS-IS와 동일)"
