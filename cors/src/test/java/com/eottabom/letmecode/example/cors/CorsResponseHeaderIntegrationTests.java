package com.eottabom.letmecode.example.cors;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CorsResponseHeaderIntegrationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private RequestRecorder requestRecorder;

	@BeforeEach
	void setUp() {
		this.requestRecorder.clear();
	}

	@Test
	@DisplayName("preflight 가 허용 범위 안이면 Allow-* 헤더와 Vary 를 함께 돌려준다")
	void preflightWithinPolicy() throws Exception {
		// when
		HttpResponse<String> response = CorsRequests.preflight(this.port, "/api/expose/items", CorsConfig.FRONT_ORIGIN,
				"PUT", "content-type,x-request-id");

		// then
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).hasValue(CorsConfig.FRONT_ORIGIN);
		assertThat(response.headers().firstValue("Access-Control-Allow-Methods")).hasValue("GET,POST,PUT");
		assertThat(response.headers().firstValue("Access-Control-Allow-Headers"))
			.hasValue("content-type, x-request-id");
		assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");
		assertThat(response.headers().firstValue("Access-Control-Max-Age")).hasValue("1800");
		assertThat(response.headers().allValues("Vary")).contains("Origin", "Access-Control-Request-Method",
				"Access-Control-Request-Headers");
	}

	@Test
	@DisplayName("허용하지 않은 헤더로 preflight 를 보내면 CORS 헤더 없이 403 을 돌려준다")
	void preflightWithDisallowedHeader() throws Exception {
		// when
		HttpResponse<String> response = CorsRequests.preflight(this.port, "/api/expose/items", CorsConfig.FRONT_ORIGIN,
				"PUT", "x-foo");

		// then
		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).isEqualTo("Invalid CORS request");
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
	}

	@Test
	@DisplayName("허용하지 않은 출처의 요청은 컨트롤러를 실행하지 않고 403 으로 거절한다")
	void requestFromDisallowedOrigin() throws Exception {
		// when
		HttpResponse<String> response = CorsRequests.get(this.port, "/api/basic/items", "http://evil.example");

		// then
		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(this.requestRecorder.handled()).isEmpty();
	}

	@Test
	@DisplayName("Origin 이 없는 요청은 CORS 요청이 아니라서 검사 없이 처리한다")
	void requestWithoutOrigin() throws Exception {
		// when
		HttpResponse<String> response = CorsRequests.get(this.port, "/api/basic/items", null);

		// then
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
		assertThat(this.requestRecorder.handled()).containsExactly("GET /api/basic/items");
	}

	@Test
	@DisplayName("CORS 설정이 없어도 서버는 요청을 처리하고 200 을 돌려준다, 막는 것은 브라우저다")
	void requestWithoutCorsMapping() throws Exception {
		// when
		HttpResponse<String> response = CorsRequests.get(this.port, "/api/none/items", CorsConfig.FRONT_ORIGIN);

		// then
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
		assertThat(this.requestRecorder.handled()).containsExactly("GET /api/none/items");
	}

	@Test
	@DisplayName("allowedOrigins(*) 와 allowCredentials(true) 를 같이 쓰면 CORS 요청 시점에 500 이 난다")
	void wildcardOriginWithCredentials() throws Exception {
		// when
		HttpResponse<String> response = CorsRequests.get(this.port, "/api/wild-credentials/items",
				CorsConfig.FRONT_ORIGIN);

		// then
		assertThat(response.statusCode()).isEqualTo(500);
		assertThat(this.requestRecorder.handled()).isEmpty();
	}

	@Test
	@DisplayName("allowedOriginPatterns(*) 는 요청한 출처를 그대로 돌려줘서 모든 출처에 credentials 를 허용한다")
	void originPatternsEchoAnyOrigin() throws Exception {
		// when
		HttpResponse<String> response = CorsRequests.get(this.port, "/api/pattern/items", "http://anything.example");

		// then
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://anything.example");
		assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");
	}

}
