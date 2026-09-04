package com.eottabom.letmecode.example.timeoutsettings;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// RestTemplate/RestClient(HttpComponentsClientHttpRequestFactory)와 달리, WebClient(Reactor Netty)는
// connect timeout(채널 옵션)과 응답 대기 timeout 을 서로 다른 API로 설정한다.
// 응답 대기는 responseTimeout(HTTP 레벨)과 ReadTimeoutHandler(소켓 read 레벨) 두 가지 방식이 있는데,
// 둘 다 백엔드 지연을 제때 끊는지 각각 확인한다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WebClientTimeoutTests {

	private static final String UNROUTABLE_URL = "http://10.255.255.1";

	@LocalServerPort
	private int port;

	@Test
	void failsAfterConnectTimeoutElapses() {
		long connectTimeoutMs = 300;
		WebClient client = TimeoutWebClientFactory.create(UNROUTABLE_URL,
				new TimeoutHttpClientFactory.TimeoutConfig(connectTimeoutMs, 5000, 300_000));

		long start = System.nanoTime();
		assertThatThrownBy(() -> client.get().uri("/api/hello").retrieve().bodyToMono(String.class).block())
			.isInstanceOf(WebClientRequestException.class);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		// connect timeout 값 근처에서 실패해야 한다. read timeout(5000ms)까지 기다렸다면 설정이 잘못 걸린 것이다.
		assertThat(elapsedMs).isBetween(connectTimeoutMs, 2000L);
	}

	@Test
	void responseTimeout_failsBeforeBackendDelayElapses() {
		long readTimeoutMs = 500;
		long backendDelayMs = 3000;
		WebClient client = TimeoutWebClientFactory.create(baseUrl(),
				new TimeoutHttpClientFactory.TimeoutConfig(3000, readTimeoutMs, 300_000));

		long start = System.nanoTime();
		assertThatThrownBy(() -> client.get()
			.uri("/api/hello?delayMs=" + backendDelayMs)
			.retrieve()
			.bodyToMono(String.class)
			.block()).isInstanceOf(WebClientRequestException.class);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		// responseTimeout 을 안 걸었다면 이 요청은 백엔드 지연(3000ms)이 끝날 때까지 안 끊긴다.
		assertThat(elapsedMs).isBetween(readTimeoutMs, backendDelayMs);
	}

	@Test
	void readTimeoutHandler_failsBeforeBackendDelayElapses() {
		long readTimeoutMs = 500;
		long backendDelayMs = 3000;
		// responseTimeout 대신 저수준 ReadTimeoutHandler 로도 같은 지연을 끊을 수 있다.
		WebClient client = TimeoutWebClientFactory.createWithReadTimeoutHandler(baseUrl(),
				new TimeoutHttpClientFactory.TimeoutConfig(3000, readTimeoutMs, 300_000));

		long start = System.nanoTime();
		assertThatThrownBy(() -> client.get()
			.uri("/api/hello?delayMs=" + backendDelayMs)
			.retrieve()
			.bodyToMono(String.class)
			.block()).isInstanceOf(WebClientRequestException.class);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		assertThat(elapsedMs).isBetween(readTimeoutMs, backendDelayMs);
	}

	@Test
	void succeedsWhenBackendRespondsBeforeReadTimeout() {
		WebClient client = TimeoutWebClientFactory.create(baseUrl(),
				new TimeoutHttpClientFactory.TimeoutConfig(3000, 5000, 300_000));

		String response = client.get().uri("/api/hello?delayMs=0").retrieve().bodyToMono(String.class).block();

		assertThat(response).isEqualTo("hello");
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

}
