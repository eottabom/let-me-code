package com.eottabom.letmecode.example.timeoutsettings;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

// WebClient(Reactor Netty)는 connect timeout과 read/write timeout을 완전히 다른 방식으로 설정한다.
// connect timeout은 Netty 채널 옵션이고, read/write timeout은 연결 후 파이프라인에 꽂는 핸들러다.
// ReadTimeoutHandler를 빼먹으면 connect timeout만 걸리고, 응답이 영원히 안 와도 요청이 끝없이 대기한다.
final class TimeoutWebClientFactory {

	private TimeoutWebClientFactory() {
	}

	static WebClient create(String baseUrl, TimeoutHttpClientFactory.TimeoutConfig config) {
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.connectTimeoutMs())
			.doOnConnected(
					(conn) -> conn.addHandlerLast(new ReadTimeoutHandler(config.readTimeoutMs(), TimeUnit.MILLISECONDS))
						.addHandlerLast(new WriteTimeoutHandler(config.readTimeoutMs(), TimeUnit.MILLISECONDS)));

		return WebClient.builder().baseUrl(baseUrl).clientConnector(new ReactorClientHttpConnector(httpClient)).build();
	}

}
