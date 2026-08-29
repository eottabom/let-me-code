package com.eottabom.letmecode.example.feignhttpclient;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 네 가지 클라이언트가 모두 같은 백엔드를 정상 호출하는지, hc5/okhttp/http-interface 는 풀 통계를 조회할 수 있는지 확인한다.
// default 가 실제로 HttpURLConnection 을 쓰는지는 LoggingHttpUrlConnectionClient 의 런타임 로그로 확인한다(README 참고).
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class FeignClientFactoryTests {

	private static final String BASE_URL = "http://localhost:8090";

	@Test
	void defaultClientCallsBackend() {
		FeignClientFactory.BuiltClient built = FeignClientFactory.create(ClientMode.DEFAULT, BASE_URL);
		assertThat(built.client().hello(0)).isEqualTo("hello");
	}

	@Test
	void hc5ClientExposesPoolStats() {
		FeignClientFactory.BuiltClient built = FeignClientFactory.create(ClientMode.HC5, BASE_URL);
		assertThat(built.client().hello(0)).isEqualTo("hello");
		assertThat(built.printPoolStats()).isNotNull();
	}

	@Test
	void okHttpClientExposesPoolStats() {
		FeignClientFactory.BuiltClient built = FeignClientFactory.create(ClientMode.OKHTTP, BASE_URL);
		assertThat(built.client().hello(0)).isEqualTo("hello");
		assertThat(built.printPoolStats()).isNotNull();
	}

	@Test
	void httpInterfaceCallsBackend() {
		HttpInterfaceClientFactory.BuiltClient built = HttpInterfaceClientFactory.create(BASE_URL);
		assertThat(built.client().hello(0)).isEqualTo("hello");
	}

	@Test
	void factoryRejectsHttpInterfaceMode() {
		assertThatThrownBy(() -> FeignClientFactory.create(ClientMode.HTTP_INTERFACE, BASE_URL))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
