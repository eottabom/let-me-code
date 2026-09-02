package com.eottabom.letmecode.example.timeoutsettings;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Tomcat 의 keep-alive-timeout 을 짧게 잡아 서버가 먼저 커넥션을 끊게 만든 뒤, 클라이언트 풀이 그 사실을
// 모른 채 같은 커넥션을 재사용하려는 상황을 재현한다. validateAfterInactivity 가 없으면(길게 잡으면) 죽은
// 커넥션을 그대로 재사용하다 예외가 나고, 짧게 잡으면 재사용 전에 살아있는지 확인해서 투명하게 새 커넥션으로
// 바꿔치기한다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "server.tomcat.keep-alive-timeout=200ms")
class ValidateAfterInactivityTests {

	@LocalServerPort
	private int port;

	@Test
	void withoutValidateAfterInactivity_reusingDeadConnectionFails() throws Exception {
		resetSeenPorts();
		// validateAfterInactivity 를 10분으로 잡아, 이 테스트가 도는 동안은 사실상 꺼둔 것과 같다.
		try (TimeoutHttpClientFactory.BuiltClient built = TimeoutHttpClientFactory.createWithManager(baseUrl(),
				TimeoutHttpClientFactory.defaultConfig(), 10 * 60 * 1000)) {

			built.client().get().uri("/api/hello").retrieve().body(String.class);

			// Tomcat 의 keep-alive-timeout(200ms) 과 Poller sweep 주기를 확실히 넘긴다.
			Thread.sleep(2500);

			// 풀은 이 커넥션이 아직 살아있다고 믿고 그대로 내준다. Tomcat 은 이미 닫았기 때문에 예외가 난다.
			assertThatThrownBy(() -> built.client().get().uri("/api/hello").retrieve().body(String.class))
				.isInstanceOf(ResourceAccessException.class);
		}
	}

	@Test
	void withValidateAfterInactivity_deadConnectionIsTransparentlyReplaced() throws Exception {
		resetSeenPorts();
		// 200ms 이상 논 커넥션은 재사용 전에 검증한다. Tomcat 의 keep-alive-timeout(200ms) 보다 살짝 크게 잡아서
		// "죽었을 가능성이 있는 시점"에는 항상 검증이 걸리게 한다.
		try (TimeoutHttpClientFactory.BuiltClient built = TimeoutHttpClientFactory.createWithManager(baseUrl(),
				TimeoutHttpClientFactory.defaultConfig(), 300)) {

			built.client().get().uri("/api/hello").retrieve().body(String.class);
			assertThat(seenPorts()).hasSize(1);

			Thread.sleep(2500);

			// 검증 덕분에 예외 없이 새 커넥션으로 갈아타고 정상 응답한다.
			String response = built.client().get().uri("/api/hello").retrieve().body(String.class);

			assertThat(response).isEqualTo("hello");
			assertThat(seenPorts()).as("죽은 커넥션 대신 새로 연결된 remote port 가 기록돼야 한다").hasSize(2);
		}
	}

	private Set<Integer> seenPorts() {
		Integer[] ports = RestClient.create(baseUrl()).get().uri("/api/debug/ports").retrieve().body(Integer[].class);
		return java.util.Arrays.stream(ports).collect(java.util.stream.Collectors.toSet());
	}

	private void resetSeenPorts() {
		RestClient.create(baseUrl()).post().uri("/api/debug/ports/reset").retrieve().toBodilessEntity();
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

}
