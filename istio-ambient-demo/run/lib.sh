#!/usr/bin/env bash
# istio-ambient-demo 의 run 스크립트들이 공유하는 함수 모음.
set -euo pipefail

BASE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KCTL="kubectl --context kind-istio-demo"
KIND_CLUSTER="istio-demo"
PF_PORT=18080

log() { printf "\n==== %s ====\n" "$*"; }

require() {
	command -v "$1" >/dev/null 2>&1 || { echo "Missing command: $1" >&2; exit 1; }
}

ensure_kind_cluster() {
	if ! kind get clusters 2>/dev/null | grep -qx "${KIND_CLUSTER}"; then
		echo "Creating kind cluster '${KIND_CLUSTER}'..."
		kind create cluster --name "${KIND_CLUSTER}"
	fi
}

# kind 노드는 Docker 컨테이너이므로 Docker Desktop 의 host.docker.internal 을 그대로 resolve 할 수 있다.
# 이 IP 를 k8s Endpoints 에 등록해서 Istio Gateway -> 호스트(Mac)의 docker-compose 컨테이너로 가는 경로를 만든다.
resolve_host_ip() {
	local node
	node="$(kind get nodes --name "${KIND_CLUSTER}" | head -1)"
	docker exec "${node}" getent hosts host.docker.internal | awk '{print $1}'
}

apply_with_host_ip() {
	local file="$1"
	local host_ip="$2"
	sed "s/\${HOST_IP}/${host_ip}/g" "${file}" | ${KCTL} apply -f -
}

wait_for_deployment() {
	local namespace="$1"
	local deployment="$2"
	for _ in $(seq 1 30); do
		${KCTL} get deployment/"${deployment}" -n "${namespace}" >/dev/null 2>&1 && break
		sleep 2
	done
	${KCTL} rollout status deployment/"${deployment}" -n "${namespace}" --timeout=120s
}

ensure_gateway_api_crds() {
	if ! ${KCTL} get crd gateways.gateway.networking.k8s.io >/dev/null 2>&1; then
		echo "Installing Gateway API CRDs..."
		${KCTL} apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml
		${KCTL} wait --for=condition=Established crd/gateways.gateway.networking.k8s.io --timeout=60s
	fi
}

start_port_forward() {
	local namespace="$1"
	local pid_file="${BASE}/run/port-forward.${namespace}.pid"

	if [ -f "${pid_file}" ]; then
		local old_pid
		old_pid="$(cat "${pid_file}" || true)"
		[ -n "${old_pid}" ] && kill -0 "${old_pid}" >/dev/null 2>&1 && kill "${old_pid}" >/dev/null 2>&1 || true
		rm -f "${pid_file}"
	fi

	local existing_pid
	existing_pid="$(lsof -ti:${PF_PORT} 2>/dev/null || true)"
	if [ -n "${existing_pid}" ]; then
		echo "Releasing port ${PF_PORT} occupied by PID ${existing_pid}..."
		kill ${existing_pid} 2>/dev/null || true
		sleep 0.5
	fi

	# --address 0.0.0.0: docker-compose 의 nginx 컨테이너가 host.docker.internal 로 이 포트에 붙을 수 있어야 함
	${KCTL} port-forward --address 0.0.0.0 -n "${namespace}" svc/demo-gw-istio ${PF_PORT}:80 >/dev/null 2>&1 &
	echo "$!" > "${pid_file}"

	for _ in $(seq 1 25); do
		(echo > /dev/tcp/127.0.0.1/${PF_PORT}) >/dev/null 2>&1 && break
		sleep 0.2
	done
}

stop_port_forward() {
	local namespace="$1"
	local pid_file="${BASE}/run/port-forward.${namespace}.pid"

	if [ ! -f "${pid_file}" ]; then
		echo "No port-forward pid file for ${namespace}."
		return 0
	fi

	local pid
	pid="$(cat "${pid_file}" || true)"
	[ -n "${pid}" ] && kill -0 "${pid}" >/dev/null 2>&1 && kill "${pid}" >/dev/null 2>&1 || true
	rm -f "${pid_file}"
	echo "Port-forward (${namespace}) stopped."
}
