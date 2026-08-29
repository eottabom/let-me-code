package com.eottabom.letmecode.example.feignhttpclient;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

// Feign 마이그레이션 목표: RestClient + HttpServiceProxyFactory, hc5와 동일한 풀 설정으로 비교.
final class HttpInterfaceClientFactory {

	private static final Logger log = LoggerFactory.getLogger(HttpInterfaceClientFactory.class);

	private HttpInterfaceClientFactory() {
	}

	static BuiltClient create(String baseUrl) {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(50);
		connectionManager.setDefaultMaxPerRoute(50);
		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();

		RestClient restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
			.build();
		HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
			.build();
		HttpInterfaceBackendClient client = proxyFactory.createClient(HttpInterfaceBackendClient.class);

		Runnable printStats = () -> log.info("[http-interface] pool stats = {}", connectionManager.getTotalStats());
		return new BuiltClient(client, printStats);
	}

	record BuiltClient(HttpInterfaceBackendClient client, Runnable printPoolStats) {
	}

}
