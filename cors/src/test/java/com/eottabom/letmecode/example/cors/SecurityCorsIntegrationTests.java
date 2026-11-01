package com.eottabom.letmecode.example.cors;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityCorsIntegrationTests {

	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
			properties = { "cors.require-authentication=true", "cors.security-cors-enabled=false" })
	@DisplayName("인증을 요구하는데 http.cors() 가 없으면")
	class WithoutSecurityCors {

		@LocalServerPort
		private int port;

		@Autowired
		private RequestRecorder requestRecorder;

		@Test
		@DisplayName("preflight 가 Security 에서 401 로 끝나고, 뒤쪽 필터까지 오지 않는다")
		void preflightIsRejectedBySecurity() throws Exception {
			// given
			this.requestRecorder.clear();

			// when
			HttpResponse<String> response = CorsRequests.preflight(this.port, "/api/basic/items",
					CorsConfig.FRONT_ORIGIN, "GET", "authorization");

			// then
			assertThat(response.statusCode()).isEqualTo(401);
			assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
			assertThat(this.requestRecorder.arrived()).isEmpty();
		}

	}

	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
			properties = { "cors.require-authentication=true", "cors.security-cors-enabled=true" })
	@DisplayName("인증을 요구하면서 http.cors() 를 켜면")
	class WithSecurityCors {

		@LocalServerPort
		private int port;

		@Test
		@DisplayName("addCorsMappings 설정으로 preflight 를 통과시킨다")
		void preflightUsesMvcCorsMappings() throws Exception {
			// when
			HttpResponse<String> response = CorsRequests.preflight(this.port, "/api/basic/items",
					CorsConfig.FRONT_ORIGIN, "GET", "authorization");

			// then
			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).hasValue(CorsConfig.FRONT_ORIGIN);
			assertThat(response.headers().firstValue("Access-Control-Allow-Headers")).hasValue("authorization");
		}

	}

}
