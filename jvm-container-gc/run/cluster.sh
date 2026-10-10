#!/usr/bin/env bash
# 사용법: cluster.sh up | forward | down
#   up      kind 클러스터 생성 → 앱 이미지 빌드/적재 → 관측 스택 배포
#   forward Grafana(13000), Prometheus(19090) 포트 포워딩 (Ctrl+C 로 종료)
#   down    클러스터 삭제
source "$(dirname "$0")/lib.sh"

up() {
	if ! kind get clusters 2>/dev/null | grep -qx "$KIND_CLUSTER"; then
		log "kind 클러스터 생성: $KIND_CLUSTER"
		kind create cluster --name "$KIND_CLUSTER"
	fi
	log "앱 빌드 및 이미지 적재"
	(cd "$DEMO_DIR/.." && ./gradlew -q :jvm-container-gc:app:bootJar)
	local agent="$DEMO_DIR/app/build/otel/opentelemetry-javaagent.jar"
	if [[ ! -f "$agent" ]]; then
		mkdir -p "$(dirname "$agent")"
		curl -fL -o "$agent" "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION:-2.31.1}/opentelemetry-javaagent.jar"
	fi
	docker build -q -t "$IMAGE" "$DEMO_DIR/app"
	kind load docker-image "$IMAGE" --name "$KIND_CLUSTER"

	log "관측 스택 배포"
	$KCTL apply -f "$DEMO_DIR/k8s/observability.yaml"
	$KCTL create configmap grafana-dashboards-json --from-file="$DEMO_DIR/k8s/grafana" --dry-run=client -o yaml | $KCTL apply -f -
	$KCTL create configmap k6-script --from-file="$DEMO_DIR/k8s/load/k6.js" --dry-run=client -o yaml | $KCTL apply -f -
	$KCTL rollout restart deploy/grafana >/dev/null
	for name in otel-collector prometheus grafana; do
		$KCTL rollout status "deploy/$name" --timeout=300s
	done
}

forward() {
	$KCTL port-forward svc/grafana 13000:3000 >/dev/null &
	$KCTL port-forward svc/prometheus 19090:9090 >/dev/null &
	trap 'kill $(jobs -p)' EXIT
	printf 'Grafana: http://localhost:13000/d/jvm-resize\nPrometheus: http://localhost:19090\n'
	wait
}

down() {
	kind delete cluster --name "$KIND_CLUSTER"
}

case "${1:-}" in
	up | forward | down) "$1" ;;
	*) echo "usage: $0 up | forward | down" >&2; exit 1 ;;
esac
