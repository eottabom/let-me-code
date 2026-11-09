package com.eottabom.letmecode.example.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 시간이 걸리는 시작 작업을 흉내 낸다. Runner 는 웹 서버가 포트를 연 뒤, ApplicationReadyEvent 전에 실행된다.
@Component
class SlowRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SlowRunner.class);

	private final StartupProperties properties;

	SlowRunner(StartupProperties properties) {
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
