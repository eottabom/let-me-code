package com.eottabom.letmecode.webfluxsupport;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// timeout-settings 모듈의 NettyServerIdleTimeoutTests 전용으로 REACTIVE 모드로 띄우는 최소
// WebFlux 애플리케이션이다. 반드시 com.eottabom.letmecode.example.timeoutsettings 패키지
// 바깥에 둬야 한다 - 같은 패키지에 두면 TimeoutSettingsApplication(Tomcat, @SpringBootApplication)의
// 기본 컴포넌트 스캔에 이 클래스의 컨트롤러까지 걸려서, 기존 BackendController 의 매핑과 충돌한다.
@Configuration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class WebFluxServerApplication {

	@RestController
	static class HelloController {

		@GetMapping("/api/hello")
		String hello() {
			return "hello";
		}

	}

}
