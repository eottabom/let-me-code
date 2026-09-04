package com.eottabom.letmecode.example.timeoutsettings;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

// WebClient(Reactor Netty)는 connect timeout 과 응답 대기 timeout 을 완전히 다른 API 로 설정한다.
// connect timeout 은 Netty 채널 옵션이고, 응답 대기는 responseTimeout(HTTP 레벨) 또는
// ReadTimeoutHandler(소켓 read 레벨)로 나뉜다. 둘 다 안 걸면 응답이 영원히 안 와도 요청이 끝없이 대기한다.
final class TimeoutWebClientFactory {

	private TimeoutWebClientFactory() {
	}

	// HTTP 응답 대기 timeout 은 responseTimeout 으로 잡는 게 가장 직관적이다.
	static WebClient create(String baseUrl, TimeoutHttpClientFactory.TimeoutConfig config) {
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeoutMs())
			.responseTimeout(Duration.ofMillis(config.readTimeoutMs()));

		return webClient(baseUrl, httpClient);
	}

	// 같은 상황을 소켓 read/write 정체 기준으로 잡고 싶을 때 쓰는 저수준 핸들러 방식.
	// responseTimeout(HTTP 응답 대기)과는 동작하는 계층이 다르다.
	static WebClient createWithReadTimeoutHandler(String baseUrl, TimeoutHttpClientFactory.TimeoutConfig config) {
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeoutMs())
			.doOnConnected(
					(conn) -> conn.addHandlerLast(new ReadTimeoutHandler(config.readTimeoutMs(), TimeUnit.MILLISECONDS))
						.addHandlerLast(new WriteTimeoutHandler(config.readTimeoutMs(), TimeUnit.MILLISECONDS)));

		return webClient(baseUrl, httpClient);
	}

	private static WebClient webClient(String baseUrl, HttpClient httpClient) {
		return WebClient.builder().baseUrl(baseUrl).clientConnector(new ReactorClientHttpConnector(httpClient)).build();
	}

}
