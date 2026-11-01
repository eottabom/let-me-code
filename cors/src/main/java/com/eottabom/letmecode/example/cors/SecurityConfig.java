package com.eottabom.letmecode.example.cors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// 기본은 모든 요청을 열어두고, cors.require-authentication=true 일 때만 인증을 요구한다.
// 인증을 요구하면서 http.cors() 를 켜지 않으면 preflight(OPTIONS) 가 MVC 의 CORS 처리까지 가기 전에 401 이 된다.
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, CorsProperties corsProperties) throws Exception {
		http.csrf((csrf) -> csrf.disable());
		if (!corsProperties.requireAuthentication()) {
			http.authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll());
			return http.build();
		}

		http.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults());
		if (corsProperties.securityCorsEnabled()) {
			// CorsConfigurationSource 빈이 없으면 addCorsMappings(CorsConfig) 설정을 그대로 쓴다.
			http.cors(Customizer.withDefaults());
		}
		return http.build();
	}

}
