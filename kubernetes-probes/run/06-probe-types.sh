#!/usr/bin/env bash
# 시나리오 6. TCP, Exec, gRPC probe 가 실패하면 어떤 메시지로 재시작되는가
source "$(dirname "$0")/lib.sh"
ensure_client
k delete pod tcp-probe exec-probe grpc-probe --ignore-not-found >/dev/null
k apply -f "$DEMO_DIR/k8s/probe-types.yaml" >/dev/null
k wait --for=condition=Ready pod/tcp-probe pod/exec-probe pod/grpc-probe --timeout=180s >/dev/null
sleep 20
log "정상 상태"; k get pods tcp-probe exec-probe grpc-probe

log "exec: /tmp/alive 삭제"; call_pod exec-probe POST '/chaos/alive-file?present=false'
log "gRPC: agnhost 의 health 상태를 NOT_SERVING 으로 (8080 /make-not-serving)"
ip=$(k get pod grpc-probe -o jsonpath='{.status.podIP}'); k exec client -- curl -s "http://$ip:8080/make-not-serving"; echo
log "tcp: 8080 을 듣는 프로세스를 멈춘다 (java 를 SIGSTOP 하면 포트는 열려 있다)"
k exec tcp-probe -- sh -c 'kill -STOP 1' || true
sleep 30
log "30초 뒤"; k get pods tcp-probe exec-probe grpc-probe
for p in tcp-probe exec-probe grpc-probe; do
	echo "--- $p"; k get events --field-selector "involvedObject.name=$p" -o custom-columns='REASON:.reason,MSG:.message' | grep -E 'Unhealthy|Killing' | sort -u | head -2 || echo '(probe 실패 이벤트 없음)'
done
