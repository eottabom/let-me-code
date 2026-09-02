package com.eottabom.letmecode.example.timeoutsettings;

import org.apache.hc.client5.http.impl.IdleConnectionEvictor;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

// PoolMaintenanceTests 는 closeIdle() 을 테스트 코드에서 직접 호출하는 "수동" 퇴거를 보여준다.
// 이 테스트는 그 API 자체를 스케줄링해주는 IdleConnectionEvictor 를 빈처럼 start()/shutdown() 만
// 관리해두면, 요청 코드에서 closeIdle() 을 직접 호출하지 않아도 백그라운드 스레드가 알아서
// 유휴 커넥션을 주기적으로 퇴거한다는 것을 보여준다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class IdleConnectionEvictorTests {

	@LocalServerPort
	private int port;

	private IdleConnectionEvictor evictor;

	@AfterEach
	void tearDown() {
		if (this.evictor != null) {
			this.evictor.shutdown();
		}
	}

	@Test
	void evictorAutomaticallyClosesIdleConnectionsInTheBackground() throws Exception {
		try (TimeoutHttpClientFactory.BuiltClient built = TimeoutHttpClientFactory.createWithManager(baseUrl(),
				TimeoutHttpClientFactory.defaultConfig(), 10 * 60 * 1000)) {

			built.client().get().uri("/api/hello").retrieve().body(String.class);
			assertThat(built.connectionManager().getTotalStats().getAvailable()).as("요청이 끝나면 커넥션은 풀에 유휴 상태로 반납된다")
				.isEqualTo(1);

			// 100ms 마다 한 번씩 훑으면서, 유휴 시간이 0ms 를 넘긴(=조금이라도 논) 커넥션은 전부 퇴거한다.
			// 애플리케이션 코드는 start() 만 호출할 뿐, closeIdle()/closeExpired() 를 직접 부르지 않는다.
			this.evictor = new IdleConnectionEvictor(built.connectionManager(), TimeValue.ofMilliseconds(100),
					TimeValue.ZERO_MILLISECONDS);
			this.evictor.start();

			// 최소 한 번의 sweep 주기(100ms)가 지나가도록 기다린다.
			Thread.sleep(500);

			PoolStats afterEvictorRuns = built.connectionManager().getTotalStats();
			assertThat(afterEvictorRuns.getAvailable()).as("closeIdle() 을 직접 호출하지 않아도 evictor 가 알아서 퇴거했어야 한다").isZero();
		}
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

}
