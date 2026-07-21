package com.eottabom.letmecode.example.urlencoding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기본 Tomcat 설정 — relaxedQueryChars 미적용.
 *
 * <ul>
 * <li>케이스 2: FE 가 %7C 로 인코딩해서 전송 + 기본 Tomcat → 200 OK</li>
 * </ul>
 *
 * 케이스 1(FE 미인코딩 raw '|' + 기본 Tomcat → 400)은 Raw HTTP 소켓 레벨 검증이 필요하므로 SearchE2eTests 에서
 * Playwright 로 검증한다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SearchControllerDefaultTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@DisplayName("일반 단일 필터 — 정상 응답")
	void singleFilter() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/products?filter=category:전자기기",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("category:전자기기");
	}

	@Test
	@DisplayName("케이스 2: FE 가 '|' 를 %7C 로 인코딩 + 기본 Tomcat → 200")
	void encodedPipeFilter() {
		// FE 에서 encodeURIComponent('category:전자기기|brand:삼성') 한 결과
		ResponseEntity<String> response = this.restTemplate.getForEntity(
				"/products?filter=category:%EC%A0%84%EC%9E%90%EA%B8%B0%EA%B8%B0%7Cbrand:%EC%82%BC%EC%84%B1",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("category:전자기기");
		assertThat(response.getBody()).contains("brand:삼성");
	}

}
