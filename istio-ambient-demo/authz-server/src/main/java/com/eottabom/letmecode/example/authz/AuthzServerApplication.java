package com.eottabom.letmecode.example.authz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Istio Ambient mode 의 ext_authz 데모에서 Envoy(Istio Gateway) 가 gRPC 로 호출하는 외부 인가 서버.
 *
 * 동작 흐름:
 * Istio Gateway (Envoy) → AuthorizationPolicy(CUSTOM) → ext_authz filter → 이 서버:9001 (gRPC)
 * → X-Demo-User 헤더 검증
 * → 허용: OkHttpResponse 에 X-Authz-User / X-Authz-Result 헤더를 담아 반환 (Envoy 가 upstream 요청에 주입)
 * → 차단: DeniedHttpResponse(403) 반환 (Envoy 가 upstream 호출 없이 즉시 403 응답)
 *
 * spring-cloud-gateway-demo(profile=authz) 의 SessionAuthzGatewayFilter 와 동일한 검증 규칙을 사용해서,
 * "AS-IS: 애플리케이션 레이어에서 검증" → "TO-BE: 인프라 레이어(Istio)에서 검증" 전환을 비교할 수 있게 한다.
 */
@SpringBootApplication
public class AuthzServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthzServerApplication.class, args);
	}
}
