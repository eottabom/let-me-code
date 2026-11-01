package com.eottabom.letmecode.example.cors;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// 브라우저가 보내는 CORS 요청 헤더(Origin, Access-Control-Request-*)를 그대로 흉내 내서 서버 응답만 확인한다.
final class CorsRequests {

	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	private CorsRequests() {
	}

	static HttpResponse<String> preflight(int port, String path, String origin, String method, String headers)
			throws IOException, InterruptedException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(uri(port, path))
			.method("OPTIONS", HttpRequest.BodyPublishers.noBody())
			.header("Origin", origin)
			.header("Access-Control-Request-Method", method);
		if (headers != null) {
			builder.header("Access-Control-Request-Headers", headers);
		}
		return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	static HttpResponse<String> get(int port, String path, String origin) throws IOException, InterruptedException {
		HttpRequest.Builder builder = HttpRequest.newBuilder(uri(port, path)).GET();
		if (origin != null) {
			builder.header("Origin", origin);
		}
		return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static URI uri(int port, String path) {
		return URI.create("http://localhost:" + port + path);
	}

}
