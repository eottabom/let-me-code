package com.eottabom.letmecode.example.probes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

// 빈 초기화 단계에서 시간을 끌어 느린 애플리케이션 시작을 흉내 낸다.
// 내장 Tomcat 은 모든 싱글톤 빈이 만들어진 뒤에 포트를 열기 때문에, 이 동안 probe 는 connection refused 로 실패한다.
@Component
public class SlowStartup {

	private static final Logger log = LoggerFactory.getLogger(SlowStartup.class);

	public SlowStartup(ProbesProperties properties) throws InterruptedException {
		long seconds = properties.startupDelaySeconds();
		if (seconds > 0) {
			log.info("시작 지연 {}초", seconds);
			Thread.sleep(seconds * 1000);
			log.info("시작 지연 끝");
		}
	}

}
