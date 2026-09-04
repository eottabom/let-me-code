package com.eottabom.letmecode.example.timeoutsettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

// Tomcat 과 달리 Reactor Netty(WebFlux) 서버는 connection-timeout/idle-timeout 의 기본값이 없다
// (Spring Boot 4.0.7 기준, NettyServerProperties 의 필드가 생성자에서 초기화되지 않는다).
// server.netty.idle-timeout 을 명시적으로 짧게 잡으면 Tomcat 의 keep-alive-timeout 과 똑같이
// 유휴 커넥션을 정리한다는 것을 raw socket 으로 확인한다.
class NettyServerIdleTimeoutTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void serverClosesConnectionAfterIdleTimeout() throws IOException, InterruptedException {
		this.context = new SpringApplicationBuilder(
				com.eottabom.letmecode.webfluxsupport.WebFluxServerApplication.class)
			.web(WebApplicationType.REACTIVE)
			.properties("server.port=0", "server.netty.idle-timeout=200ms")
			.run();

		int port = ((WebServerApplicationContext) this.context).getWebServer().getPort();

		try (Socket socket = new Socket("localhost", port)) {
			socket.setSoTimeout(3000);

			sendKeepAliveRequest(socket);
			readFullResponse(socket);

			// idle-timeout(200ms) 을 넘길 때까지 기다린 뒤 같은 소켓을 재사용해본다.
			Thread.sleep(1000);

			sendKeepAliveRequest(socket);

			// 서버가 닫았다는 사실은 두 형태로 관측된다. FIN 으로 정상 종료면 read() 가 EOF(-1) 을 돌려주고,
			// RST 로 끊겼으면 SocketException 이 난다. 어느 쪽이든 결론은 같으므로 둘 다 통과로 본다.
			boolean serverClosed;
			try {
				serverClosed = socket.getInputStream().read() == -1;
			}
			catch (SocketException ex) {
				serverClosed = true;
			}

			assertThat(serverClosed).as("idle-timeout 이 지난 커넥션은 서버가 이미 닫았어야 한다").isTrue();
		}
	}

	private void sendKeepAliveRequest(Socket socket) throws IOException {
		OutputStream out = socket.getOutputStream();
		String request = "GET /api/hello HTTP/1.1\r\n" + "Host: localhost\r\n" + "Connection: keep-alive\r\n\r\n";
		out.write(request.getBytes(StandardCharsets.US_ASCII));
		out.flush();
	}

	private void readFullResponse(Socket socket) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
		int contentLength = 0;
		String line;
		while ((line = reader.readLine()) != null && !line.isEmpty()) {
			if (line.toLowerCase().startsWith("content-length:")) {
				contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
			}
		}
		for (int i = 0; i < contentLength; i++) {
			reader.read();
		}
	}

}
