package com.eottabom.letmecode.example.startup;

import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

// 첫 요청 비용을 원인별로 나눠 보려고 의존성이 다른 API 세 개를 둔다.
// /hello 는 Spring MVC 와 JVM 비용만, /users 는 여기에 DB 커넥션 풀, /external 은 HTTP 클라이언트 연결이 더해진다.
@RestController
class SampleController {

	private final JdbcClient jdbcClient;

	private final RestClient restClient;

	SampleController(JdbcClient jdbcClient, RestClient.Builder restClientBuilder, StartupProperties properties) {
		this.jdbcClient = jdbcClient;
		this.restClient = restClientBuilder.baseUrl(properties.stubUrl()).build();
	}

	@GetMapping("/hello")
	HelloResponse hello() {
		return new HelloResponse("hello");
	}

	@GetMapping("/users/{id}")
	UserResponse user(@PathVariable long id) {
		return this.jdbcClient.sql("select id, name from users where id = ?")
			.param(id)
			.query((resultSet, rowNumber) -> new UserResponse(resultSet.getLong("id"), resultSet.getString("name")))
			.single();
	}

	@GetMapping("/external")
	Map<String, Object> external() {
		String body = this.restClient.get().uri("/stub.json").retrieve().body(String.class);
		return Map.of("length", (body != null) ? body.length() : 0);
	}

	record HelloResponse(String message) {
	}

	record UserResponse(long id, String name) {
	}

}
