package com.eottabom.letmecode.example.cors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 경로마다 CORS 정책을 다르게 두고, 같은 컨트롤러(ItemController)를 호출해서 차이를 본다.
// /api/none/** 은 일부러 매핑하지 않는다.
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	/**
	 * 프론트 페이지 출처. 브라우저 테스트와 캡처 페이지가 이 포트로 뜬다.
	 */
	public static final String FRONT_ORIGIN = "http://localhost:5173";

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// 지정하지 않은 값은 Spring 기본값: methods GET, HEAD, POST / headers * / maxAge 1800
		registry.addMapping("/api/basic/**").allowedOrigins(FRONT_ORIGIN);

		registry.addMapping("/api/expose/**")
			.allowedOrigins(FRONT_ORIGIN)
			.allowedMethods("GET", "POST", "PUT")
			.allowedHeaders("Content-Type", "X-Request-Id")
			.exposedHeaders("X-Total-Count")
			.allowCredentials(true)
			.maxAge(1800);

		registry.addMapping("/api/wild/**").allowedOrigins("*");

		// 애플리케이션은 뜨지만, CORS 요청이 들어오는 순간 IllegalArgumentException 으로 500 이 난다.
		registry.addMapping("/api/wild-credentials/**").allowedOrigins("*").allowCredentials(true);

		// 요청한 Origin 을 그대로 돌려주므로 사실상 모든 출처에 credentials 를 허용한다. 피해야 할 설정.
		registry.addMapping("/api/pattern/**").allowedOriginPatterns("*").allowCredentials(true);
	}

}
