package com.eottabom.letmecode.example.feignhttpclient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import feign.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// feign.Client.Default 는 요청마다 {@code java.net.HttpURLConnection} 을 새로 연다. getConnection(URL) 을 오버라이드해서 실제 커넥션 구현체 클래스명을 로그로 찍어, hc5/okhttp 모드 로그(ApacheHttp5Client, OkHttpClient)와 눈으로 비교할 수 있게 한다.
final class LoggingHttpUrlConnectionClient extends Client.Default {

	private static final Logger log = LoggerFactory.getLogger(LoggingHttpUrlConnectionClient.class);

	LoggingHttpUrlConnectionClient() {
		super(null, null);
	}

	@Override
	public HttpURLConnection getConnection(URL url) throws IOException {
		HttpURLConnection connection = super.getConnection(url);
		log.info("[default] connection impl={} http.maxConnections={}", connection.getClass().getName(),
				System.getProperty("http.maxConnections", "5 (default)"));
		return connection;
	}

}
