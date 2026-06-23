package com.eottabom.letmecode.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Istio Ambient mode 데모의 AS-IS 구간(기존 역방향 프록시 애플리케이션)을 대체하는 Spring Cloud Gateway.
 *
 * - profile "simple": A 서버를 직접 호출하던 단순 리버스 프록시 대체
 * - profile "authz": session-svc 검증 + 헤더 주입 + B 서버 호출 흐름 대체 (ext_authz 데모의 AS-IS 비교군)
 *
 * Istio Ambient + Gateway API 전환 후에는 이 애플리케이션 코드를 건드리지 않고도
 * nginx(ALB) 가 바로 Istio Gateway 로 향하도록 인프라만 바꿔서 동일한 응답을 받을 수 있음을 보여주는 것이 목적.
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayDemoProperties.class)
public class GatewayDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayDemoApplication.class, args);
	}

	@Bean
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

	@Bean
	@Profile("simple")
	public RouteLocator simpleProxyRoutes(RouteLocatorBuilder builder, GatewayDemoProperties properties) {
		return builder.routes()
			.route("simple-proxy", route -> route
				.path("/api")
				.filters(f -> f.filter(new PrefixResponseBodyFilter("Gateway Result -> ")))
				.uri(properties.aServerUri()))
			.build();
	}

	@Bean
	@Profile("authz")
	public RouteLocator authzRoutes(RouteLocatorBuilder builder, GatewayDemoProperties properties) {
		return builder.routes()
			.route("session-authz-proxy", route -> route
				.path("/api")
				.filters(f -> f.filter(new SessionAuthzGatewayFilter(
					webClientBuilder().build(), properties.sessionSvcUri())))
				.uri(properties.bServerUri()))
			.build();
	}
}
