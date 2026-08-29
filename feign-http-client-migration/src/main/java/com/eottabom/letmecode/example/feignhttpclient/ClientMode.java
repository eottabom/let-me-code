package com.eottabom.letmecode.example.feignhttpclient;

public enum ClientMode {

	/** OpenFeign 만 추가했을 때의 기본값. JDK {@code HttpURLConnection} 기반, 별도 커넥션 풀 없음. */
	DEFAULT,

	/** feign-hc5 의존성 추가 + Apache HttpClient5 커넥션 풀 사용. */
	HC5,

	/** feign-okhttp 의존성 추가 + OkHttp 커넥션 풀 사용. */
	OKHTTP,

	/** Feign 을 걷어내고 Spring 6 HTTP Interface(HttpServiceProxyFactory)로 마이그레이션한 경우. */
	HTTP_INTERFACE;

	static ClientMode from(String raw) {
		for (ClientMode mode : values()) {
			if (mode.name().equalsIgnoreCase(raw) || mode.name().replace("_", "-").equalsIgnoreCase(raw)) {
				return mode;
			}
		}
		throw new IllegalArgumentException("Unknown mode: " + raw + " (default|hc5|okhttp|http-interface)");
	}

}
