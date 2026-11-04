package com.eottabom.letmecode.example.probes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// probe 실험용으로 애플리케이션 상태를 일부러 바꾸는 엔드포인트.
@RestController
public class ChaosController {

	// exec probe 가 확인하는 파일. 애플리케이션이 준비되면 만들고, 엔드포인트로 지운다.
	static final Path ALIVE_FILE = Path.of("/tmp/alive");

	private final ApplicationEventPublisher publisher;

	public ChaosController(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void createAliveFile() throws IOException {
		Files.writeString(ALIVE_FILE, "ok");
	}

	@PostMapping("/chaos/liveness")
	public Map<String, String> liveness(@RequestParam boolean broken) {
		LivenessState state = broken ? LivenessState.BROKEN : LivenessState.CORRECT;
		AvailabilityChangeEvent.publish(this.publisher, this, state);
		return Map.of("liveness", state.name());
	}

	@PostMapping("/chaos/readiness")
	public Map<String, String> readiness(@RequestParam boolean refuse) {
		ReadinessState state = refuse ? ReadinessState.REFUSING_TRAFFIC : ReadinessState.ACCEPTING_TRAFFIC;
		AvailabilityChangeEvent.publish(this.publisher, this, state);
		return Map.of("readiness", state.name());
	}

	@PostMapping("/chaos/alive-file")
	public Map<String, Boolean> aliveFile(@RequestParam boolean present) throws IOException {
		if (present) {
			Files.writeString(ALIVE_FILE, "ok");
		}
		else {
			Files.deleteIfExists(ALIVE_FILE);
		}
		return Map.of("present", Files.exists(ALIVE_FILE));
	}

	// timeoutSeconds 실험용. 지정한 시간만큼 늦게 응답한다.
	@GetMapping("/slow")
	public Map<String, Long> slow(@RequestParam(defaultValue = "0") long millis) throws InterruptedException {
		Thread.sleep(millis);
		return Map.of("sleptMillis", millis);
	}

	@GetMapping("/hello")
	public Map<String, String> hello() {
		return Map.of("pod", System.getenv().getOrDefault("HOSTNAME", "local"));
	}

}
