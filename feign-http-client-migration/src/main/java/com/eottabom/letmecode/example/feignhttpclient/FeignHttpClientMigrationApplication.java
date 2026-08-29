package com.eottabom.letmecode.example.feignhttpclient;

import java.util.function.LongFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// 실행: ./gradlew :feign-http-client-migration:bootRun --args="default 30 200"
// args: mode(default|hc5|okhttp|http-interface) concurrency|count delayMs [seq|burst2]
// seq: 순서대로 호출 (커넥션 재사용 여부)
// burst2: 동시 버스트를 두 번 연달아 (idle 캐시 상한을 넘겼을 때 재사용이 끊기는지)
@SpringBootApplication
public class FeignHttpClientMigrationApplication {

	private static final Logger log = LoggerFactory.getLogger(FeignHttpClientMigrationApplication.class);

	private static final String BASE_URL = "http://localhost:8090";

	public static void main(String[] args) {
		SpringApplication.run(FeignHttpClientMigrationApplication.class, args);
	}

	@Bean
	CommandLineRunner loadTestRunner() {
		return (args) -> {
			if (args.length < 1) {
				log.info("Usage: <mode: default|hc5|okhttp|http-interface> [count=20] [delayMs=100] [seq]");
				return;
			}

			ClientMode mode = ClientMode.from(args[0]);
			int count = (args.length > 1) ? Integer.parseInt(args[1]) : 20;
			long delayMs = (args.length > 2) ? Long.parseLong(args[2]) : 100;
			String runMode = (args.length > 3) ? args[3] : "";
			boolean sequential = "seq".equalsIgnoreCase(runMode);
			boolean twoRounds = "burst2".equalsIgnoreCase(runMode);

			LongFunction<String> call;
			Runnable printPoolStats;
			if (mode == ClientMode.HTTP_INTERFACE) {
				HttpInterfaceClientFactory.BuiltClient built = HttpInterfaceClientFactory.create(BASE_URL);
				call = built.client()::hello;
				printPoolStats = built.printPoolStats();
			}
			else {
				FeignClientFactory.BuiltClient built = FeignClientFactory.create(mode, BASE_URL);
				call = built.client()::hello;
				printPoolStats = built.printPoolStats();
			}

			if (sequential) {
				LoadTestRunner.runSequential(mode.name(), count, delayMs, call);
			}
			else if (twoRounds) {
				LoadTestRunner.runTwoRounds(mode.name(), count, delayMs, call);
			}
			else {
				LoadTestRunner.run(mode.name(), count, delayMs, call);
			}
			printPoolStats.run();
		};
	}

}
