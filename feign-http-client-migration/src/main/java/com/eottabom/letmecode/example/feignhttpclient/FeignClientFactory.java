package com.eottabom.letmecode.example.feignhttpclient;

import java.util.concurrent.TimeUnit;

import feign.Feign;
import feign.hc5.ApacheHttp5Client;
import feign.okhttp.OkHttpClient;
import okhttp3.ConnectionPool;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ClientMode 별로 커넥션 처리 방식이 다른 BackendClient 를 만든다.
final class FeignClientFactory {

	private static final Logger log = LoggerFactory.getLogger(FeignClientFactory.class);

	private FeignClientFactory() {
	}

	static BuiltClient create(ClientMode mode, String baseUrl) {
		return switch (mode) {
			case DEFAULT -> defaultClient(baseUrl);
			case HC5 -> hc5Client(baseUrl);
			case OKHTTP -> okHttpClient(baseUrl);
			case HTTP_INTERFACE -> throw new IllegalArgumentException("HTTP_INTERFACE 는 HttpInterfaceRunner 를 사용한다");
		};
	}

	private static BuiltClient defaultClient(String baseUrl) {
		// feign.Client.Default -> HttpURLConnection, 풀 없음. 실제 구현체는
		// LoggingHttpUrlConnectionClient 로그로 확인.
		BackendClient client = Feign.builder()
			.client(new LoggingHttpUrlConnectionClient())
			.target(BackendClient.class, baseUrl);
		Runnable printStats = () -> log.info("[default] 코드로 조회 가능한 풀 통계 없음. netstat/lsof 로 TCP 커넥션 수를 직접 세어야 한다.");
		return new BuiltClient(client, printStats);
	}

	private static BuiltClient hc5Client(String baseUrl) {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(50);
		connectionManager.setDefaultMaxPerRoute(50);

		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();

		BackendClient client = Feign.builder()
			.client(new ApacheHttp5Client(httpClient))
			.target(BackendClient.class, baseUrl);

		Runnable printStats = () -> log.info("[hc5] pool stats = {}", connectionManager.getTotalStats());

		return new BuiltClient(client, printStats);
	}

	private static BuiltClient okHttpClient(String baseUrl) {
		ConnectionPool pool = new ConnectionPool(50, 5, TimeUnit.MINUTES);
		okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder().connectionPool(pool).build();

		BackendClient client = Feign.builder()
			.client(new OkHttpClient(okHttpClient))
			.target(BackendClient.class, baseUrl);

		Runnable printStats = () -> log.info("[okhttp] connectionCount={} idleConnectionCount={}",
				pool.connectionCount(), pool.idleConnectionCount());

		return new BuiltClient(client, printStats);
	}

	record BuiltClient(BackendClient client, Runnable printPoolStats) {
	}

}
