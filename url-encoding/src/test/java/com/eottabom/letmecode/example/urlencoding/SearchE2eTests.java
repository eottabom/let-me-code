package com.eottabom.letmecode.example.urlencoding;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Playwright E2E 테스트 — 기본 Tomcat 설정.
 *
 * <pre>
 * 케이스 1: FE 미인코딩('|' raw HTTP)          + 기본 Tomcat → 400
 * 케이스 2: FE 인코딩(encodeURIComponent → %7C) + 기본 Tomcat → 200
 * </pre>
 *
 * page.request().get() 은 raw HTTP 소켓 레벨로 요청하므로 브라우저 자동 인코딩 없이 '|' 를 그대로 전송할 수 있다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SearchE2eTests {

	@LocalServerPort
	private int port;

	private static Playwright playwright;

	private static Browser browser;

	@BeforeAll
	static void setUp() {
		playwright = Playwright.create();
		browser = playwright.chromium().launch();
	}

	@AfterAll
	static void tearDown() {
		browser.close();
		playwright.close();
	}

	@Test
	@DisplayName("케이스 1: FE 미인코딩(raw '|') + 기본 Tomcat → 400")
	void unencodedPipeDefaultTomcat() {
		try (BrowserContext context = browser.newContext()) {
			Page page = context.newPage();
			APIResponse response = page.request()
				.get("http://localhost:" + this.port + "/products?filter=category:전자기기|brand:삼성");
			assertThat(response.status()).isEqualTo(400);
		}
	}

	@Test
	@DisplayName("케이스 2: FE 인코딩(%7C) + 기본 Tomcat → 200")
	void encodedPipeDefaultTomcat() {
		try (BrowserContext context = browser.newContext()) {
			Page page = context.newPage();
			APIResponse response = page.request()
				.get("http://localhost:" + this.port
						+ "/products?filter=category:%EC%A0%84%EC%9E%90%EA%B8%B0%EA%B8%B0%7Cbrand:%EC%82%BC%EC%84%B1");
			assertThat(response.status()).isEqualTo(200);
			assertThat(response.text()).contains("category:전자기기");
		}
	}

	@Test
	@DisplayName("브라우저 navigate — 주소창에 '|' 입력 시 브라우저가 %7C 로 자동 인코딩하여 200")
	void browserNavigateAutoEncodesPipe() {
		try (BrowserContext context = browser.newContext()) {
			Page page = context.newPage();
			page.navigate("http://localhost:" + this.port + "/products?filter=category:전자기기|brand:삼성");
			assertThat(page.content()).contains("category:전자기기");
		}
	}

}
