package com.eottabom.letmecode.example.timeoutsettings;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// TCP 커넥션은 정상적으로 맺혔지만 응답이 늦게 오는 상황(백엔드가 3초간 응답을 지연)을 재현한다.
// connection timeout 이 아니라 read timeout 이 발동해야 하고, 대기 시간도 read timeout 값 근처여야 한다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ReadTimeoutTests {

	@LocalServerPort
	private int port;

	@Test
	void failsAfterReadTimeoutElapses_notAfterBackendDelay() {
		long readTimeoutMs = 500;
		long backendDelayMs = 3000;
		RestClient client = TimeoutHttpClientFactory.create(baseUrl(),
				new TimeoutHttpClientFactory.TimeoutConfig(3000, readTimeoutMs, 300_000));

		long start = System.nanoTime();
		assertThatThrownBy(() -> client.get().uri("/api/hello?delayMs=" + backendDelayMs).retrieve().body(String.class))
			.isInstanceOf(ResourceAccessException.class);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		// read timeout(500ms) 근처에서 끊겨야 한다. 백엔드 지연(3000ms)까지 기다렸다면 read timeout 설정이 안 먹은
		// 것이다.
		assertThat(elapsedMs).isBetween(readTimeoutMs, backendDelayMs);
	}

	@Test
	void succeedsWhenBackendRespondsBeforeReadTimeout() {
		RestClient client = TimeoutHttpClientFactory.create(baseUrl(), TimeoutHttpClientFactory.defaultConfig());

		String response = client.get().uri("/api/hello?delayMs=0").retrieve().body(String.class);

		assertThat(response).isEqualTo("hello");
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

}
