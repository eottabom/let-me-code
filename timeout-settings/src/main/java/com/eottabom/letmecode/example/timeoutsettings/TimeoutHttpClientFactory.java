package com.eottabom.letmecode.example.timeoutsettings;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// connectTimeout(TCP 핸드셰이크), readTimeout(응답 대기), connectionTimeToLive(keep-alive 로 재사용할 최대 수명)를
// 각각 독립적으로 조절할 수 있는 RestClient 를 만든다. 셋 다 이름은 비슷해 보이지만 서로 다른 지점에서 동작한다.
final class TimeoutHttpClientFactory {

	private TimeoutHttpClientFactory() {
	}

	static RestClient create(String baseUrl, TimeoutConfig config) {
		return createWithManager(baseUrl, config, 1000).client();
	}

	// closeExpired()/closeIdle() 처럼 풀을 직접 다뤄야 하는 테스트를 위해 connectionManager 도 함께 반환한다.
	static BuiltClient createWithManager(String baseUrl, TimeoutConfig config, long validateAfterInactivityMs) {
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setMaxConnTotal(20)
			.setMaxConnPerRoute(20)
			.setDefaultConnectionConfig(connectionConfig(config, validateAfterInactivityMs))
			.build();

		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout(Timeout.ofSeconds(5))
			.setResponseTimeout(Timeout.ofMilliseconds(config.readTimeoutMs()))
			.build();

		CloseableHttpClient httpClient = HttpClients.custom()
			.setConnectionManager(connectionManager)
			.setDefaultRequestConfig(requestConfig)
			// HttpClient5 는 GET 처럼 멱등한 요청에서 죽은 커넥션 예외(NoHttpResponseException 등)를 만나면
			// 기본적으로 조용히 재시도한다. 이 모듈은 그 재시도 뒤에 숨는 실패를 그대로 보여주는 게 목적이라 끈다.
			.disableAutomaticRetries()
			.build();

		RestClient client = RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
			.build();

		return new BuiltClient(client, connectionManager, httpClient);
	}

	// HttpClientBuilder.evictIdleConnections(...) 는 내부적으로 IdleConnectionEvictor 백그라운드 스레드를
	// 띄워서 closeIdle()/closeExpired() 를 주기적으로 대신 호출해준다.
	// evictor 를 직접 만들어 start()/shutdown() 을 관리하는 것보다 이 빌더 메서드 한 줄이 간편하다.
	static BuiltClient createWithIdleEviction(String baseUrl, TimeoutConfig config, long maxIdleMs) {
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setMaxConnTotal(20)
			.setMaxConnPerRoute(20)
			.setDefaultConnectionConfig(connectionConfig(config, 10 * 60 * 1000))
			.build();

		CloseableHttpClient httpClient = HttpClients.custom()
			.setConnectionManager(connectionManager)
			.disableAutomaticRetries()
			.evictIdleConnections(TimeValue.ofMilliseconds(maxIdleMs))
			.evictExpiredConnections()
			.build();

		RestClient client = RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
			.build();

		return new BuiltClient(client, connectionManager, httpClient);
	}

	static ConnectionConfig connectionConfig(TimeoutConfig config) {
		return connectionConfig(config, 1000);
	}

	private static ConnectionConfig connectionConfig(TimeoutConfig config, long validateAfterInactivityMs) {
		return ConnectionConfig.custom()
			.setConnectTimeout(Timeout.ofMilliseconds(config.connectTimeoutMs()))
			.setSocketTimeout(Timeout.ofMilliseconds(config.readTimeoutMs()))
			// HikariCP의 keepalive-time과 비슷한 보조 장치. 커넥션이 이 시간 이상 놀았으면 재사용 전에
			// 살아있는지 검증한다(자동으로 도는 게 아니라 대여 시점에 한 번 체크하는 방식).
			.setValidateAfterInactivity(Timeout.ofMilliseconds(validateAfterInactivityMs))
			// 풀이 물리 커넥션 하나를 재사용할 수 있는 최대 수명. HikariCP 의 max-lifetime 과 유사한 수명 제한을
			// HTTP 커넥션 풀 레벨에서 재현한다. 이 시간이 지나면 keep-alive 여부와 무관하게 커넥션을 새로 연다.
			.setTimeToLive(Timeout.ofMilliseconds(config.connectionTimeToLiveMs()))
			.build();
	}

	static TimeoutConfig defaultConfig() {
		return new TimeoutConfig(3000, 5000, TimeUnit.MINUTES.toMillis(5));
	}

	record TimeoutConfig(long connectTimeoutMs, long readTimeoutMs, long connectionTimeToLiveMs) {
	}

	record BuiltClient(RestClient client, PoolingHttpClientConnectionManager connectionManager,
			CloseableHttpClient httpClient) implements AutoCloseable {

		@Override
		public void close() throws IOException {
			this.httpClient.close();
		}

	}

}
