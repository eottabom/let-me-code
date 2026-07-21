package com.eottabom.letmecode.example.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 기존 역방향 프록시가 session-svc 를 호출해서 검증하고, 통과하면 인가 헤더를 주입해 백엔드(B 서버)를 호출하던 흐름을 동일하게 재현한다.
 *
 * Istio Ambient + ext_authz 전환 후에는 이 검증/헤더 주입 역할을 Envoy ext_authz filter
 * (istio-ext-authz-server 모듈) 가 대체하므로, 클라이언트와 백엔드 입장에서는 동일한 헤더 계약을 유지한 채 인가 로직만 인프라 레이어로
 * 옮겨가는 것을 보여주는 비교 대상이다.
 */
public class SessionAuthzGatewayFilter implements GatewayFilter {

	private static final String DEMO_USER_HEADER = "X-Demo-User";

	private static final String REQUEST_ID_HEADER = "X-Request-Id";

	private static final String AUTHZ_USER_HEADER = "X-Authz-User";

	private static final String AUTHZ_RESULT_HEADER = "X-Authz-Result";

	private final WebClient webClient;

	private final String sessionSvcUri;

	public SessionAuthzGatewayFilter(WebClient webClient, String sessionSvcUri) {
		this.webClient = webClient;
		this.sessionSvcUri = sessionSvcUri;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String demoUser = request.getHeaders().getFirst(DEMO_USER_HEADER) != null
				? request.getHeaders().getFirst(DEMO_USER_HEADER) : "";
		String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER) != null
				? request.getHeaders().getFirst(REQUEST_ID_HEADER) : "req-" + UUID.randomUUID();

		return webClient.get()
			.uri(sessionSvcUri + "/check")
			.header(DEMO_USER_HEADER, demoUser)
			.header(REQUEST_ID_HEADER, requestId)
			.exchangeToMono(response -> {
				if (!response.statusCode().is2xxSuccessful()) {
					return deny(exchange, requestId);
				}

				ServerWebExchange mutatedExchange = exchange.mutate()
					.request(builder -> builder.header(DEMO_USER_HEADER, demoUser)
						.header(AUTHZ_USER_HEADER, demoUser)
						.header(AUTHZ_RESULT_HEADER, "allow")
						.header(REQUEST_ID_HEADER, requestId))
					.build();

				return chain.filter(mutatedExchange);
			})
			.onErrorResume(error -> deny(exchange, requestId));
	}

	private Mono<Void> deny(ServerWebExchange exchange, String requestId) {
		exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
		exchange.getResponse().getHeaders().add(AUTHZ_RESULT_HEADER, "denied");
		exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
		String body = "DENY by session-svc: missing or invalid " + DEMO_USER_HEADER + " (requestId=" + requestId
				+ ")\n";
		return exchange.getResponse()
			.writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
	}

}
