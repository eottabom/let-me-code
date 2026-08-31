package com.eottabom.letmecode.example.timeoutsettings;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// delayMs 만큼 응답을 지연시켜 read timeout 을 재현하고, remotePort 기록으로 커넥션 재사용 여부를 확인한다.
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
