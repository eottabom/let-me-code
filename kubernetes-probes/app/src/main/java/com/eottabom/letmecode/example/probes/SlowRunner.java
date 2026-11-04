package com.eottabom.letmecode.example.probes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 캐시 워밍처럼 ApplicationRunner 에서 시간을 끄는 시작 작업을 흉내 낸다.
// Runner 는 HTTP 서버가 뜬 뒤에 실행되므로, 이 동안 liveness 는 200, readiness 는 503 을 응답한다.
@Component
public class SlowRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SlowRunner.class);

	private final ProbesProperties properties;

	public SlowRunner(ProbesProperties properties) {
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) throws InterruptedException {
		long seconds = this.properties.runnerDelaySeconds();
		if (seconds > 0) {
			log.info("Runner 지연 {}초", seconds);
			Thread.sleep(seconds * 1000);
			log.info("Runner 지연 끝");
		}
	}

}
