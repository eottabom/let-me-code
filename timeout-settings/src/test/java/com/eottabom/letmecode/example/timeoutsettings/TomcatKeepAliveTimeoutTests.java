package com.eottabom.letmecode.example.timeoutsettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

// Tomcat 의 keep-alive-timeout 을 짧게 잡아두면, 응답을 다 보낸 뒤 그 시간 안에 다음 요청이 오지 않을 때
// 커넥션을 먼저 닫아버린다는 것을 raw socket 으로 직접 확인한다.
// 운영에서는 이 값이 로드밸런서의 idle timeout 보다 짧으면, 로드밸런서가 이미 닫힌 타깃 커넥션을
// 재사용하려다 502 를 반환하는 원인이 될 수 있다.
// Tomcat 의 Poller 는 idle 커넥션을 매 요청마다가 아니라 주기적으로(기본 1초 간격) 훑으면서 정리한다.
// 그래서 keep-alive-timeout 을 짧게 잡아도 "정확히 그 시간 뒤"가 아니라 "다음 sweep 때" 끊긴다.
// 테스트에서는 sweep 주기보다 확실히 긴 시간을 기다려 타이밍에 흔들리지 않게 한다.
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "server.tomcat.keep-alive-timeout=200ms")
class TomcatKeepAliveTimeoutTests {

	@LocalServerPort
	private int port;

	@Test
	void serverClosesConnectionAfterKeepAliveTimeout() throws IOException, InterruptedException {
		try (Socket socket = new Socket("localhost", this.port)) {
			socket.setSoTimeout(3000);

			sendKeepAliveRequest(socket);
			readFullResponse(socket);

			// keep-alive-timeout(200ms) 은 물론 Tomcat Poller 의 sweep 주기(기본 1초)까지 확실히 넘기고
			// 기다린다.
			Thread.sleep(1500);

			// Tomcat 은 이미 이 커넥션을 닫았어야 한다. 클라이언트가 모르고 같은 소켓에 두 번째 요청을 보내면
			// 서버가 즉시 리셋하거나(다음 read 에서 EOF), 애초에 커넥션이 반쯤 닫혀 있어 응답이 오지 않는다.
			sendKeepAliveRequest(socket);
			int firstByte = socket.getInputStream().read();

			assertThat(firstByte).as("keep-alive-timeout 이후 재사용된 소켓은 서버가 이미 닫아서 EOF(-1)여야 한다").isEqualTo(-1);
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
