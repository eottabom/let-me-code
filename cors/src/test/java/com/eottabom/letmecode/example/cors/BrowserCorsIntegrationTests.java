package com.eottabom.letmecode.example.cors;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.JsonObject;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 Chrome 으로 localhost:5173 페이지를 열고, RANDOM_PORT 로 뜬 서버에 fetch 를 보낸다.
// 5173 페이지는 JDK HttpServer 로 빈 HTML 을 돌려준다.
// Playwright route 로 가짜 응답을 주면 Chrome 이 그 페이지를 공용(public) 주소로 보고
// localhost 호출을 Local Network Access 로 막아버려서, CORS 가 아닌 다른 에러가 난다.
// 로컬에 설치된 Chrome(channel=chrome)을 쓴다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BrowserCorsIntegrationTests {

	private static final String FETCH_SCRIPT = """
			async ([url, options]) => {
			  try {
			    const response = await fetch(url, options);
			    return 'status=' + response.status + ' X-Total-Count=' + response.headers.get('X-Total-Count');
			  } catch (e) {
			    return 'error=' + e.message;
			  }
			}
			""";

	private static final Map<String, Object> JSON_POST = Map.of("method", "POST", "headers",
			Map.of("Content-Type", "application/json"), "body", "1");

	private static final int FRONT_PORT = 5173;

	private static HttpServer frontServer;

	private static Playwright playwright;

	private static Browser browser;

	@LocalServerPort
	private int port;

	@Autowired
	private RequestRecorder requestRecorder;

	private Page page;

	private final List<String> consoleMessages = new CopyOnWriteArrayList<>();

	@BeforeAll
	static void launchBrowser() throws IOException {
		frontServer = HttpServer.create(new InetSocketAddress("localhost", FRONT_PORT), 0);
		frontServer.createContext("/", (exchange) -> {
			byte[] body = "<!doctype html><title>front</title>".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		frontServer.start();
		playwright = Playwright.create();
		browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome"));
	}

	@AfterAll
	static void closeBrowser() {
		browser.close();
		playwright.close();
		frontServer.stop(0);
	}

	@BeforeEach
	void openFrontPage() {
		this.requestRecorder.clear();
		this.page = browser.newPage();
		this.page.onConsoleMessage((message) -> this.consoleMessages.add(message.text()));
		this.page.navigate(CorsConfig.FRONT_ORIGIN + "/");
	}

	@AfterEach
	void closePage() {
		this.page.close();
	}

	@Test
	@DisplayName("CORS 설정이 없으면 서버는 200 으로 처리했는데 브라우저가 응답을 막는다")
	void browserBlocksResponseAfterServerHandled() {
		// when
		String result = fetch("/api/none/items", Map.of());

		// then
		assertThat(result).isEqualTo("error=Failed to fetch");
		assertThat(this.consoleMessages)
			.anyMatch((message) -> message.contains("No 'Access-Control-Allow-Origin' header is present"));
		assertThat(this.requestRecorder.handled()).containsExactly("GET /api/none/items");
	}

	@Test
	@DisplayName("preflight 가 실패하면 본 요청은 서버에 도착하지 않는다")
	void preflightFailureStopsActualRequest() {
		// when
		String result = fetch("/api/none/items", JSON_POST);

		// then
		assertThat(result).isEqualTo("error=Failed to fetch");
		assertThat(this.requestRecorder.arrived()).containsExactly("OPTIONS /api/none/items");
		assertThat(this.requestRecorder.handled()).isEmpty();
	}

	@Test
	@DisplayName("Simple Request 는 CORS 에러가 나도 컨트롤러가 이미 실행된다")
	void simpleRequestIsHandledEvenIfBlocked() {
		// when
		String result = fetch("/api/none/items",
				Map.of("method", "POST", "headers", Map.of("Content-Type", "text/plain"), "body", "hello"));

		// then
		assertThat(result).isEqualTo("error=Failed to fetch");
		assertThat(this.requestRecorder.arrived()).containsExactly("POST /api/none/items");
		assertThat(this.requestRecorder.handled()).containsExactly("POST /api/none/items");
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("exposeCases")
	@DisplayName("Expose-Headers 에 없는 응답 헤더는 스크립트에서 null 로 읽힌다")
	void exposeHeaders(String path, String expected) {
		// when
		String result = fetch(path, Map.of());

		// then
		assertThat(result).isEqualTo(expected);
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("blockedCases")
	@DisplayName("원인마다 Chrome 콘솔 메시지가 다르다")
	void consoleMessageByCause(Map<String, Object> options, String path, String expectedMessage) {
		// when
		String result = fetch(path, options);

		// then
		assertThat(result).isEqualTo("error=Failed to fetch");
		assertThat(this.consoleMessages).anyMatch((message) -> message.contains(expectedMessage));
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("maxAgeCases")
	@DisplayName("Max-Age 만큼 preflight 결과를 캐시한다")
	void maxAge(String path, int expectedPreflightCount) {
		// when
		for (int i = 0; i < 3; i++) {
			fetch(path, JSON_POST);
		}

		// then
		assertThat(this.requestRecorder.arrived()).filteredOn((line) -> line.startsWith("OPTIONS"))
			.hasSize(expectedPreflightCount);
	}

	@Test
	@DisplayName("Max-Age 가 없으면 기본 5초만 캐시해서, 6초 뒤에는 preflight 가 다시 나간다")
	void defaultMaxAgeIsFiveSeconds() {
		// given
		fetch("/fixed/no-age", JSON_POST);
		fetch("/fixed/no-age", JSON_POST);

		// when
		this.page.waitForTimeout(6_000);
		fetch("/fixed/no-age", JSON_POST);

		// then
		assertThat(this.requestRecorder.arrived()).containsExactly("OPTIONS /fixed/no-age", "POST /fixed/no-age",
				"POST /fixed/no-age", "OPTIONS /fixed/no-age", "POST /fixed/no-age");
	}

	@Test
	@DisplayName("Allow-Headers: * 에 Authorization 이 아직 통과하지만, Chrome 은 지원 중단 Issue 를 남긴다")
	void authorizationIsNotCoveredByWildcard() {
		// given
		List<String> issues = new CopyOnWriteArrayList<>();
		CDPSession cdp = this.page.context().newCDPSession(this.page);
		cdp.on("Audits.issueAdded", (JsonObject event) -> issues.add(event.toString()));
		cdp.send("Audits.enable");

		// when
		String result = fetch("/fixed/star",
				Map.of("method", "POST", "headers", Map.of("Authorization", "Bearer token")));
		this.page.waitForTimeout(500);

		// then
		assertThat(result).startsWith("status=200");
		assertThat(issues).anyMatch((issue) -> issue.contains("AuthorizationCoveredByWildcard"));
	}

	static List<Arguments> exposeCases() {
		return List.of(Arguments.of("/api/basic/items", "status=200 X-Total-Count=null"),
				Arguments.of("/api/expose/items", "status=200 X-Total-Count=2"));
	}

	static List<Arguments> blockedCases() {
		return List.of(Arguments.of(Named.of("Allow-Origin * + credentials", Map.of("credentials", "include")),
				"/api/wild/items", "must not be the wildcard '*' when the request's credentials mode is 'include'"),
				Arguments.of(Named.of("Spring 이 PUT 을 403 으로 거절", Map.of("method", "PUT")), "/api/basic/items",
						"Response to preflight request doesn't pass access control check: No 'Access-Control-Allow-Origin'"),
				Arguments.of(Named.of("고정 헤더에 PUT 없음", Map.of("method", "PUT")), "/fixed/basic",
						"Method PUT is not allowed by Access-Control-Allow-Methods in preflight response"),
				Arguments.of(Named.of("고정 헤더에 X-Foo 없음", Map.of("headers", Map.of("X-Foo", "1"))), "/fixed/basic",
						"Request header field x-foo is not allowed by Access-Control-Allow-Headers in preflight response"),
				Arguments.of(Named.of("credentials 인데 Allow-Credentials 없음", Map.of("credentials", "include")),
						"/fixed/basic",
						"The value of the 'Access-Control-Allow-Credentials' header in the response is ''"),
				Arguments.of(
						Named.of("credentials 면 Allow-Methods * 도 무효",
								Map.of("method", "PUT", "credentials", "include")),
						"/fixed/star-credentials",
						"Method PUT is not allowed by Access-Control-Allow-Methods in preflight response"));
	}

	static List<Arguments> maxAgeCases() {
		return List.of(Arguments.of("/fixed/age0", 3), Arguments.of("/fixed/age600", 1));
	}

	private String fetch(String path, Map<String, Object> options) {
		return (String) this.page.evaluate(FETCH_SCRIPT, List.of("http://localhost:" + this.port + path, options));
	}

}
