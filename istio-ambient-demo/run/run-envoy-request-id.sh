#!/usr/bin/env bash
# Envoy 가 ext_authz 를 호출하기 전에 x-request-id 를 먼저 만드는지 확인한다.
#
# 1) 헤더 없이 요청하면 Envoy 가 만든 UUID 가 인가 서버와 업스트림에 똑같이 전달된다.
# 2) x-request-id 를 붙여서 요청하면 그 값이 그대로 전달된다.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../envoy-request-id"

PROJECT=envoy-request-id
trap 'docker compose -p "$PROJECT" down >/dev/null 2>&1' EXIT

docker compose -p "$PROJECT" up -d >/dev/null
# Envoy 뒤의 인가 서버와 업스트림까지 떠서 200 이 올 때까지 기다린다.
for _ in $(seq 1 30); do curl -sf -o /dev/null localhost:10000/ready && break; sleep 2; done

upstream_request_id() {
	python3 -c "import sys, json; print(json.load(sys.stdin)['headers'].get('x-request-id'))"
}

authz_request_id() {
	sleep 1
	docker compose -p "$PROJECT" logs --no-log-prefix authz 2>/dev/null | grep -o '"x-request-id": *"[^"]*"' | tail -1 | cut -d'"' -f4
}

echo "== 1) x-request-id 없이 요청"
echo "upstream: $(curl -s localhost:10000/api -H 'x-demo-user: alice' | upstream_request_id)"
echo "authz   : $(authz_request_id)"

echo "== 2) x-request-id: req-from-client 로 요청"
echo "upstream: $(curl -s localhost:10000/api -H 'x-request-id: req-from-client' | upstream_request_id)"
echo "authz   : $(authz_request_id)"
