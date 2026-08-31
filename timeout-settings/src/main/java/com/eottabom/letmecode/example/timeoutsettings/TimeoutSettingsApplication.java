package com.eottabom.letmecode.example.timeoutsettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

// 실행: ./gradlew :timeout-settings:bootRun --args="hikari-lifetime"
// args: hikari-lifetime [waitTimeoutSeconds=35] [borrowCount=5] [intervalMs=10000]
// Docker 데몬이 떠 있어야 한다 (Testcontainers 로 MySQL 을 띄운다).
//
// HTTP 클라이언트 타임아웃(connect/read)과 커넥션 재사용은 RANDOM_PORT 백엔드가 필요해서
// 자동화 테스트(src/test)로 재현한다. 여기서는 실행 중인 애플리케이션 없이도 바로 확인할 수 있는
// HikariCP max-lifetime 회전만 커맨드라인으로 제공한다.
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class TimeoutSettingsApplication {

	private static final Logger log = LoggerFactory.getLogger(TimeoutSettingsApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(TimeoutSettingsApplication.class, args);
	}

	@Bean
	CommandLineRunner scenarioRunner() {
		return (args) -> {
			if (args.length < 1) {
				log.info("Usage: hikari-lifetime [waitTimeoutSeconds=35] [borrowCount=5] [intervalMs=10000]");
				log.info(
						"connect-timeout / read-timeout / connection-reuse 시나리오는 ./gradlew :timeout-settings:test 로 확인한다.");
				return;
			}

			if ("hikari-lifetime".equals(args[0])) {
				int waitTimeoutSeconds = (args.length > 1) ? Integer.parseInt(args[1]) : 35;
				int borrowCount = (args.length > 2) ? Integer.parseInt(args[2]) : 5;
				long intervalMs = (args.length > 3) ? Long.parseLong(args[3]) : 10_000;
				HikariLifetimeDemo.run(waitTimeoutSeconds, borrowCount, intervalMs);
				return;
			}

			log.info("Unknown scenario: {}", args[0]);
		};
	}

}
