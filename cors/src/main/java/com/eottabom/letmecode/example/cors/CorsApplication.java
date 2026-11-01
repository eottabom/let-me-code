package com.eottabom.letmecode.example.cors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// 실행: ./gradlew :cors:bootRun
// API 서버는 8080, 프론트(origin)는 capture/index.html 을 5173 포트로 따로 띄운다 (README 참고).
@SpringBootApplication
@ConfigurationPropertiesScan
public class CorsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CorsApplication.class, args);
	}

}
