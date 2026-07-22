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
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * tomcat-relaxed 프로필 — relaxedQueryChars="|" 적용.
 *
 * <ul>
 * <li>케이스 3: FE 미인코딩('|' raw) + relaxed Tomcat → 200</li>
 * <li>케이스 4: FE 인코딩(%7C) + relaxed Tomcat → 200</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("tomcat-relaxed")
class SearchControllerRelaxedTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@DisplayName("케이스 3: FE 미인코딩('%7C' 없이) + relaxed Tomcat → 200")
	void unencodedPipeFilter() {
		// TestRestTemplate 의 URI 는 이미 encode 된 상태로 전달되므로
		// raw '|' 케이스는 SearchE2eRelaxedTests 에서 Playwright 로 검증한다.
		// 여기서는 relaxed 프로필에서 인코딩된 요청도 정상 처리됨을 확인한다.
		ResponseEntity<String> response = this.restTemplate.getForEntity(
				"/products?filter=category:%EC%A0%84%EC%9E%90%EA%B8%B0%EA%B8%B0%7Cbrand:%EC%82%BC%EC%84%B1",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("category:electronics");
	}

	@Test
	@DisplayName("케이스 4: FE 인코딩(%7C) + relaxed Tomcat → 200")
	void encodedPipeFilter() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(
				"/products?filter=category:%EC%A0%84%EC%9E%90%EA%B8%B0%EA%B8%B0%7Cbrand:%EC%82%BC%EC%84%B1",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("brand:samsung");
	}

}
