#!/usr/bin/env bash
# 준비. kind 클러스터를 만들고, 애플리케이션 이미지를 빌드해서 클러스터에 넣는다.
# 클러스터가 이미 있으면 만들지 않고 이미지만 다시 넣는다.
source "$(dirname "$0")/lib.sh"
ROOT_DIR="$(cd "$DEMO_DIR/.." && pwd)"

if ! kind get clusters | grep -qx "$CLUSTER"; then
	log "kind 클러스터 $CLUSTER 생성"
	kind create cluster --name "$CLUSTER"
fi

log "jar 빌드"
(cd "$ROOT_DIR" && ./gradlew -q :kubernetes-probes:app:bootJar)

log "이미지 빌드 후 클러스터에 넣기"
docker build -q -t probes-app:local "$DEMO_DIR/app"
kind load docker-image probes-app:local --name "$CLUSTER"

kubectl --context "kind-$CLUSTER" create namespace "$NS" --dry-run=client -o yaml | kubectl --context "kind-$CLUSTER" apply -f - >/dev/null
log "준비 끝. run/01-liveness-fail.sh 부터 실행하면 된다"
