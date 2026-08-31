package com.eottabom.letmecode.example.timeoutsettings;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// connectTimeout(TCP 핸드셰이크), readTimeout(응답 대기), connectionTimeToLive(keep-alive 로 재사용할 최대 수명)를
// 각각 독립적으로 조절할 수 있는 RestClient 를 만든다. 셋 다 이름은 비슷해 보이지만 서로 다른 지점에서 동작한다.
final class TimeoutHttpClientFactory {

	private TimeoutHttpClientFactory() {
	}

	static RestClient create(String baseUrl, TimeoutConfig config) {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(20);
		connectionManager.setDefaultMaxPerRoute(20);
		connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
			.setConnectTimeout(Timeout.ofMilliseconds(config.connectTimeoutMs()))
			.setSocketTimeout(Timeout.ofMilliseconds(config.readTimeoutMs()))
			// 풀이 물리 커넥션 하나를 재사용할 수 있는 최대 수명. HikariCP 의 max-lifetime 과 유사한 수명 제한을
			// HTTP 커넥션 풀 레벨에서 재현한다. 이 시간이 지나면 keep-alive 여부와 무관하게 커넥션을 새로 연다.
			.setTimeToLive(Timeout.ofMilliseconds(config.connectionTimeToLiveMs()))
			.build());

		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(5)).build();

		CloseableHttpClient httpClient = HttpClients.custom()
			.setConnectionManager(connectionManager)
			.setDefaultRequestConfig(requestConfig)
			.build();

		return RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
			.build();
	}

	static TimeoutConfig defaultConfig() {
		return new TimeoutConfig(3000, 5000, TimeUnit.MINUTES.toMillis(5));
	}

	record TimeoutConfig(long connectTimeoutMs, long readTimeoutMs, long connectionTimeToLiveMs) {
	}

}
