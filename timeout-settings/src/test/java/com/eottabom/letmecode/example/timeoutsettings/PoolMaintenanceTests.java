package com.eottabom.letmecode.example.timeoutsettings;

import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

// Apache HttpClient5 는 PoolingHttpClientConnectionManager 를 직접 다뤄서 유휴 커넥션을 퇴거시킬 수 있다.
// closeIdle() 을 호출하면 실제로 풀 통계(available)가 줄어드는 것을 확인한다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PoolMaintenanceTests {

	@LocalServerPort
	private int port;

	@Test
	void closeIdleEvictsIdleConnectionsFromPool() throws Exception {
		try (TimeoutHttpClientFactory.BuiltClient built = TimeoutHttpClientFactory.createWithManager(baseUrl(),
				TimeoutHttpClientFactory.defaultConfig(), 1000)) {

			built.client().get().uri("/api/hello").retrieve().body(String.class);

			PoolStats beforeEvict = built.connectionManager().getTotalStats();
			assertThat(beforeEvict.getAvailable()).as("요청이 끝나면 커넥션은 대여 중이 아니라 풀에서 대기(Available) 상태여야 한다").isEqualTo(1);

			// closeIdle(ZERO) 은 "지금 이 순간부터 유휴 상태인 커넥션은 전부 퇴거" 라는 뜻이라, 방금 반납된
			// 커넥션도 즉시 대상이 된다. IdleConnectionEvictor 가 백그라운드에서 주기적으로 호출해주는 것과 같은 동작이다.
			built.connectionManager().closeIdle(TimeValue.ZERO_MILLISECONDS);

			PoolStats afterEvict = built.connectionManager().getTotalStats();
			assertThat(afterEvict.getAvailable()).as("퇴거 후에는 풀에 남아있는 유휴 커넥션이 없어야 한다").isZero();
		}
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

}
