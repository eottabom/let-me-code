#!/usr/bin/env bash
# 사용: ./run/stop-port-forward.sh <namespace>   (istio-demo 또는 istio-demo-authz)
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
source ./lib.sh

NS="${1:?usage: stop-port-forward.sh <namespace>}"
stop_port_forward "${NS}"
