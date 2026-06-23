# istio-ambient-demo

기존 애플리케이션 레이어의 역방향 프록시 아키텍처를 Istio Ambient mode + Gateway API 로 전환하는 과정을
로컬에서 그대로 재현해보는 데모입니다. AS-IS 비교군으로 Spring Cloud Gateway 를 사용합니다.

데모는 두 가지로 나뉘어 있습니다.

- **simple**: 단순 라우팅만 하는 역방향 프록시를 Istio Gateway + HTTPRoute 로 대체
- **authz**: 인증/인가 검증 로직(세션 검증 + 헤더 주입)을 Istio AuthorizationPolicy(CUSTOM) + ext_authz 로 대체

## 구성도

```
[simple]
AS-IS: client -> nginx(ALB) -> gateway(profile=simple) -> A 서버
TO-BE: client -> nginx(ALB) -> port-forward -> Istio Gateway -> HTTPRoute -> A 서버(Endpoints)

[authz]
AS-IS: client -> nginx(ALB) -> gateway(profile=authz) -> session-svc(검증) -> B 서버
TO-BE: client -> nginx(ALB) -> port-forward -> Istio Gateway -> AuthorizationPolicy(ext_authz) -> B 서버
```

## 모듈/디렉터리

| 경로 | 설명 |
| --- | --- |
| `gateway/` | Spring Cloud Gateway. `simple`/`authz` 프로필로 AS-IS 동작을 재현 |
| `authz-server/` | Envoy ext_authz gRPC 서버 (Spring Boot + Spring gRPC). TO-BE 인가 컴포넌트 |
| `external/` | docker-compose 로 띄우는 nginx(ALB), session-svc, b-server 및 nginx conf 템플릿 |
| `k8s/simple/` | simple 데모용 Namespace, Service+Endpoints, Gateway+HTTPRoute |
| `k8s/authz/` | authz 데모용 Namespace, authz-server Deployment, Service+Endpoints, Gateway+HTTPRoute, AuthorizationPolicy |
| `run/` | 실행/전환/정리 스크립트 |
| `docker-compose.yml` | `simple`/`authz` profile 로 AS-IS 컨테이너 구성을 분리 |

## 사전 준비

```bash
brew install kind kubectl istioctl
```

Docker Desktop 이 떠 있어야 합니다 (kind 노드가 Docker 컨테이너로 뜨고, `host.docker.internal` 을 통해
호스트의 docker-compose 컨테이너와 통신합니다).

## simple 데모 실행

```bash
./run/run-simple-demo.sh                # kind 클러스터 생성 + Istio Ambient 설치 + AS-IS(gateway-simple) 기동
curl http://localhost:8080/api          # AS-IS: "Gateway Result -> A (external server)"

./run/switch-simple-to-istio.sh         # TO-BE 전환 (HTTPRoute 적용 + port-forward)
curl http://localhost:8080/api          # TO-BE: "A (external server)" (prefix 없음 = Istio Gateway 가 직접 라우팅)

./run/switch-simple-to-gateway.sh       # AS-IS 복원 (port-forward 는 별도 종료 필요)
./run/stop-port-forward.sh istio-demo
```

## authz 데모 실행

```bash
./run/run-authz-demo.sh
curl -v http://localhost:8080/api                              # AS-IS 차단: 403 (x-demo-user 없음)
curl -H 'x-demo-user: alice' http://localhost:8080/api          # AS-IS 허용

./run/switch-authz-to-istio.sh                                  # TO-BE 전환 (authz-server 빌드 + kind load + ext_authz 적용)
curl -v http://localhost:8080/api                              # TO-BE 차단: 403 (ext_authz)
curl -H 'x-demo-user: alice' http://localhost:8080/api          # TO-BE 허용 (AS-IS와 동일한 헤더 결과)

./run/switch-authz-to-gateway.sh                                # AS-IS 복원
./run/stop-port-forward.sh istio-demo-authz
```

## 정리

```bash
./run/stop-apps.sh   # docker-compose + port-forward만 정리 (kind 클러스터는 유지, 다시 전환 테스트 가능)
./run/clear.sh        # docker-compose + port-forward + kind 클러스터까지 전체 삭제
```

## 참고

- kind 클러스터 이름: `istio-demo` (kubeconfig context: `kind-istio-demo`)
- nginx 설정은 `external/alb-templates/*.conf` 를 `external/alb-conf/default.conf` 로 복사하는 방식으로 전환합니다.
  (`default.conf` 는 런타임 생성 파일이라 git 에는 포함하지 않습니다.)
- `run/lib.sh` 의 `resolve_host_ip` 가 kind 노드 컨테이너 안에서 `host.docker.internal` 을 조회해서
  k8s `Endpoints` 에 등록합니다. Docker Desktop(macOS/Windows) 환경 기준입니다.
