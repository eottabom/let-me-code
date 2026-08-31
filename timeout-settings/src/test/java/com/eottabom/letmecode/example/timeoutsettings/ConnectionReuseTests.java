package com.eottabom.letmecode.example.timeoutsettings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

// keepalive/커넥션 풀이 "그냥 켜두면 알아서 재사용되는" 것이 아니라, 풀의 connection time-to-live 를
// 넘기면 재사용을 멈추고 새 TCP 커넥션을 연다는 것을 remotePort 로 확인한다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ConnectionReuseTests {

	@LocalServerPort
	private int port;

	@Test
	void reusesSameConnectionWithinTimeToLive() {
		resetSeenPorts();
		RestClient client = TimeoutHttpClientFactory.create(baseUrl(),
				new TimeoutHttpClientFactory.TimeoutConfig(3000, 3000, 60_000));

		for (int i = 0; i < 5; i++) {
			client.get().uri("/api/hello").retrieve().body(String.class);
		}

		// 순차 호출 5번이 전부 같은 물리 커넥션(remote port)을 재사용해야 한다.
		assertThat(seenPorts()).hasSize(1);
	}

	@Test
	void opensNewConnectionAfterTimeToLiveExpires() throws InterruptedException {
		resetSeenPorts();
		long timeToLiveMs = 300;
		RestClient client = TimeoutHttpClientFactory.create(baseUrl(),
				new TimeoutHttpClientFactory.TimeoutConfig(3000, 3000, timeToLiveMs));

		client.get().uri("/api/hello").retrieve().body(String.class);
		assertThat(seenPorts()).hasSize(1);

		// time-to-live 를 넘길 때까지 기다린 뒤 다시 호출하면, 같은 클라이언트/풀이어도 새 remote port 가 찍힌다.
		Thread.sleep(timeToLiveMs + 200);
		client.get().uri("/api/hello").retrieve().body(String.class);

		assertThat(seenPorts()).hasSize(2);
	}

	private Set<Integer> seenPorts() {
		Integer[] ports = RestClient.create(baseUrl()).get().uri("/api/debug/ports").retrieve().body(Integer[].class);
		return new HashSet<>(Arrays.asList(ports));
	}

	private void resetSeenPorts() {
		RestClient.create(baseUrl()).post().uri("/api/debug/ports/reset").retrieve().toBodilessEntity();
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

}
