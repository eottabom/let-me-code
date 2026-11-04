package com.eottabom.letmecode.example.probes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

// DB 같은 외부 의존성을 흉내 낸 헬스 체크. 대상 호스트에 TCP 연결이 되는지만 본다.
// liveness 그룹에 넣느냐 readiness 그룹에 넣느냐에 따라 장애 시 동작이 달라지는 것을 실험한다.
@Component("dependency")
public class DependencyHealthIndicator implements HealthIndicator {

	private static final int TIMEOUT_MILLIS = 500;

	private final ProbesProperties properties;

	public DependencyHealthIndicator(ProbesProperties properties) {
		this.properties = properties;
	}

	@Override
	public Health health() {
		String host = this.properties.dependencyHost();
		if (host == null || host.isBlank()) {
			return Health.up().withDetail("dependency", "설정 안 됨").build();
		}
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, this.properties.dependencyPort()), TIMEOUT_MILLIS);
			return Health.up().withDetail("target", host + ":" + this.properties.dependencyPort()).build();
		}
		catch (IOException ex) {
			return Health.down(ex).withDetail("target", host + ":" + this.properties.dependencyPort()).build();
		}
	}

}
