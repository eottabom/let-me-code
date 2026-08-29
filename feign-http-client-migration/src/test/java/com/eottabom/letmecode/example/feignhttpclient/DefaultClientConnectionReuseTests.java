package com.eottabom.letmecode.example.feignhttpclient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.LongFunction;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

// default(HttpURLConnection) 는 동시 버스트가 idle 캐시 상한(http.maxConnections, 기본 5)을 넘으면
// 두 번째 버스트에서 그 상한만큼만 재사용하고 나머지는 새 커넥션을 연다. hc5(풀)는 maxTotal 이하면 전부 재사용한다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class DefaultClientConnectionReuseTests {

	@LocalServerPort
	private int port;

	@Test
	void defaultClientReusesAtMostKeepAliveCacheLimit() throws InterruptedException {
		FeignClientFactory.BuiltClient built = FeignClientFactory.create(ClientMode.DEFAULT, baseUrl());
		Set<Integer> reused = reusedPortsAcrossTwoBursts(built.client()::hello);

		int keepAliveLimit = Integer.parseInt(System.getProperty("http.maxConnections", "5"));
		assertThat(reused).hasSizeLessThanOrEqualTo(keepAliveLimit);
	}

	@Test
	void hc5ClientReusesAllConnectionsWithinPool() throws InterruptedException {
		FeignClientFactory.BuiltClient built = FeignClientFactory.create(ClientMode.HC5, baseUrl());
		Set<Integer> reused = reusedPortsAcrossTwoBursts(built.client()::hello);

		assertThat(reused).hasSize(20);
	}

	private Set<Integer> reusedPortsAcrossTwoBursts(LongFunction<String> call) throws InterruptedException {
		resetSeenPorts();
		LoadTestRunner.run("test", 20, 20, call);
		Set<Integer> round1 = seenPorts();

		resetSeenPorts();
		LoadTestRunner.run("test", 20, 20, call);
		Set<Integer> round2 = seenPorts();

		Set<Integer> reused = new HashSet<>(round1);
		reused.retainAll(round2);
		return reused;
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
