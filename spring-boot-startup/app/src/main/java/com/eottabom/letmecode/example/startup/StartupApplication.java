package com.eottabom.letmecode.example.startup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StartupApplication {

	// /actuator/startup 으로 볼 시작 단계 기록 개수. 이보다 많으면 뒤쪽 단계는 버려진다.
	private static final int STARTUP_STEP_CAPACITY = 20_000;

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(StartupApplication.class);
		application.setApplicationStartup(new BufferingApplicationStartup(STARTUP_STEP_CAPACITY));
		// 컨텍스트가 만들어지기 전 이벤트(Starting, EnvironmentPrepared 등)까지 받으려면 빈이 아니라 여기서 등록해야 한다.
		application.addListeners(new StartupTimeline());
		application.run(args);
	}

}
