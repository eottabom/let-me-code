package com.eottabom.letmecode.example.timeoutsettings;

import org.junit.jupiter.api.Test;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// TCP 핸드셰이크 자체가 끝나지 않는 상황(방화벽에 막힌 IP 등)을 재현한다.
// 10.255.255.1 은 라우팅되지 않는 사설 대역이라 SYN 에 대한 응답이 영원히 오지 않는다.
// connectTimeout 이 짧으면 그 시간만큼만 기다리고 실패해야 한다.
class ConnectTimeoutTests {

	private static final String UNROUTABLE_URL = "http://10.255.255.1";

	@Test
	void failsAfterConnectTimeoutElapses() {
		long connectTimeoutMs = 300;
		RestClient client = TimeoutHttpClientFactory.create(UNROUTABLE_URL,
				new TimeoutHttpClientFactory.TimeoutConfig(connectTimeoutMs, 5000, 300_000));

		long start = System.nanoTime();
		assertThatThrownBy(() -> client.get().uri("/api/hello").retrieve().body(String.class))
			.isInstanceOf(ResourceAccessException.class);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000;

		// connect timeout 값 근처에서 실패해야 한다. read timeout(5000ms)까지 기다렸다면 설정이 잘못 걸린 것이다.
		assertThat(elapsedMs).isBetween(connectTimeoutMs, 2000L);
	}

}
