package com.eottabom.letmecode.example.feignhttpclient;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 요청이 실제로 어떤 소켓(remote port)에서 들어왔는지 기록한다.
// 같은 remote port 가 반복되면 커넥션 재사용, 매번 다른 remote port 면 매 요청마다 새 TCP 커넥션이 열린 것이다.
@RestController
public class BackendController {

	private static final Logger log = LoggerFactory.getLogger(BackendController.class);

	private final Set<Integer> seenRemotePorts = ConcurrentHashMap.newKeySet();

	@GetMapping("/api/hello")
	public String hello(@RequestParam(defaultValue = "0") long delayMs, HttpServletRequest request)
			throws InterruptedException {
		if (delayMs > 0) {
			Thread.sleep(delayMs);
		}
		this.seenRemotePorts.add(request.getRemotePort());
		log.info("handled request from remotePort={} localPort={}", request.getRemotePort(), request.getLocalPort());
		return "hello";
	}

	@GetMapping("/api/debug/ports")
	public Set<Integer> seenPorts() {
		return Set.copyOf(this.seenRemotePorts);
	}

	@PostMapping("/api/debug/ports/reset")
	public void resetSeenPorts() {
		this.seenRemotePorts.clear();
	}

}
