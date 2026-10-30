package com.eottabom.letmecode.example.feignhttpclient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

// 풀 크기(50)보다 많은 동시 요청을 보냈을 때 실제로 열린 커넥션 수를 비교한다.
// hc5 는 maxTotal 에서 막고 나머지를 대기시키지만,
// okhttp 의 ConnectionPool(50, ...) 은 idle 커넥션 개수만 제한해서 동시 요청 수만큼 연결이 열린다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ConnectionUpperBoundTests {

	private static final int POOL_SIZE = 50;

	private static final int CONCURRENCY = 80;

	private static final long DELAY_MS = 300;

	@LocalServerPort
	private int port;

	@Test
	@DisplayName("hc5 는 동시 요청이 maxTotal 을 넘어도 커넥션을 maxTotal 까지만 연다")
	void hc5CapsConnectionsAtMaxTotal() throws InterruptedException {
		// given
		FeignClientFactory.BuiltClient built = FeignClientFactory.create(ClientMode.HC5, baseUrl());
		resetSeenPorts();

		// when
		LoadTestRunner.run("hc5", CONCURRENCY, DELAY_MS, built.client()::hello);

		// then
		assertThat(seenPorts()).hasSizeLessThanOrEqualTo(POOL_SIZE);
	}

	@Test
	@DisplayName("okhttp 는 ConnectionPool 크기와 상관없이 동시 요청 수만큼 커넥션을 연다")
	void okHttpHasNoConnectionUpperBound() throws InterruptedException {
		// given
		FeignClientFactory.BuiltClient built = FeignClientFactory.create(ClientMode.OKHTTP, baseUrl());
		resetSeenPorts();

		// when
		LoadTestRunner.run("okhttp", CONCURRENCY, DELAY_MS, built.client()::hello);

		// then
		assertThat(seenPorts()).hasSize(CONCURRENCY);
	}

	private Set<Integer> seenPorts() {
		Integer[] ports = restClient().get().uri("/api/debug/ports").retrieve().body(Integer[].class);
		return new HashSet<>(Arrays.asList(ports));
	}

	private void resetSeenPorts() {
		restClient().post().uri("/api/debug/ports/reset").retrieve().toBodilessEntity();
	}

	private RestClient restClient() {
		return RestClient.create(baseUrl());
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

}
