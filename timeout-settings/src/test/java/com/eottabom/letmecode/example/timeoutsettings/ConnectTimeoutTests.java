package com.eottabom.letmecode.example.timeoutsettings;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// connect timeout 은 TCP 핸드셰이크를 기다리는 시간이다.
// 실제 blackhole IP 로 시간을 재는 테스트는 OS 라우팅/방화벽 정책에 따라 즉시 실패하거나 오래 걸릴 수 있다.
// 여기서는 Apache HttpClient5 의 권장 API 인 ConnectionConfig 에 timeout 값이 들어가는지 직접 검증한다.
class ConnectTimeoutTests {

	@Test
	void configuresTimeoutsOnConnectionConfig() {
		TimeoutHttpClientFactory.TimeoutConfig timeoutConfig = new TimeoutHttpClientFactory.TimeoutConfig(300, 5000,
				300_000);

		ConnectionConfig connectionConfig = TimeoutHttpClientFactory.connectionConfig(timeoutConfig);

		assertThat(connectionConfig.getConnectTimeout().toMilliseconds()).isEqualTo(300);
		assertThat(connectionConfig.getSocketTimeout().toMilliseconds()).isEqualTo(5000);
		assertThat(connectionConfig.getTimeToLive().toMilliseconds()).isEqualTo(300_000);
	}

}
