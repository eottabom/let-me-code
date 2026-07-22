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
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Playwright E2E 테스트 — relaxed Tomcat 설정(케이스 3·4).
 *
 * <pre>
 * 케이스 3: FE 미인코딩(raw '|')          + relaxed Tomcat → 200
 * 케이스 4: FE 인코딩(%7C)                + relaxed Tomcat → 200
 * </pre>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("tomcat-relaxed")
class SearchE2eRelaxedTests {

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
	@DisplayName("케이스 3: FE 미인코딩(raw '|') + relaxed Tomcat → 200")
	void unencodedPipeRelaxedTomcat() {
		try (BrowserContext context = browser.newContext()) {
			Page page = context.newPage();
			APIResponse response = page.request()
				.get("http://localhost:" + this.port + "/products?filter=category:electronics|brand:samsung");
			assertThat(response.status()).isEqualTo(200);
			assertThat(response.text()).contains("category:electronics");
		}
	}

	@Test
	@DisplayName("케이스 4: FE 인코딩(%7C) + relaxed Tomcat → 200")
	void encodedPipeRelaxedTomcat() {
		try (BrowserContext context = browser.newContext()) {
			Page page = context.newPage();
			APIResponse response = page.request()
				.get("http://localhost:" + this.port
						+ "/products?filter=category:%EC%A0%84%EC%9E%90%EA%B8%B0%EA%B8%B0%7Cbrand:%EC%82%BC%EC%84%B1");
			assertThat(response.status()).isEqualTo(200);
			assertThat(response.text()).contains("brand:samsung");
		}
	}

}
