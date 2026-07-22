package com.eottabom.letmecode.example.urlencoding;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * front/index.html 을 브라우저로 열고 버튼 클릭 → BE 호출까지 전체 FE → BE 흐름을 검증하는 E2E 테스트.
 *
 * <pre>
 * 케이스 1: 미인코딩 버튼 클릭 + 기본 Tomcat → pre 에 "HTTP 400" 표시
 * 케이스 2: 인코딩 버튼 클릭   + 기본 Tomcat → pre 에 "HTTP 200" + 필터 결과 표시
 * </pre>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class FrontE2eTests {

	@LocalServerPort
	private int port;

	private static Playwright playwright;

	private static Browser browser;

	private BrowserContext context;

	private Page page;

	@BeforeAll
	static void launchBrowser() {
		playwright = Playwright.create();
		browser = playwright.chromium().launch();
	}

	@AfterAll
	static void closeBrowser() {
		browser.close();
		playwright.close();
	}

	@BeforeEach
	void openFrontPage() {
		this.context = browser.newContext();
		this.page = this.context.newPage();

		Path htmlFile = Paths.get("url-encoding", "front", "index.html").toAbsolutePath();
		this.page.navigate("file://" + htmlFile);
		// BE 주소를 테스트 서버 포트로 교체
		this.page.evaluate("window.BE = 'http://localhost:" + this.port + "'");
	}

	@AfterEach
	void closeFrontPage() {
		this.page.close();
		this.context.close();
	}

	@Test
	@DisplayName("케이스 1: 미인코딩 버튼 클릭 + 기본 Tomcat → 결과에 400 표시")
	void case1UnencodedPipeDefaultTomcat() {
		this.page.locator("#case1-btn").click();
		this.page.waitForFunction("document.getElementById('r1').textContent.includes('HTTP')");
		String result = this.page.locator("#r1").textContent();
		assertThat(result).contains("400");
	}

	@Test
	@DisplayName("케이스 2: 인코딩 버튼 클릭 + 기본 Tomcat → 결과에 200 + 필터 값 표시")
	void case2EncodedPipeDefaultTomcat() {
		this.page.locator("#case2-btn").click();
		this.page.waitForFunction("document.getElementById('r2').textContent.includes('HTTP')");
		String result = this.page.locator("#r2").textContent();
		assertThat(result).contains("200");
		assertThat(result).contains("electronics");
	}

}
