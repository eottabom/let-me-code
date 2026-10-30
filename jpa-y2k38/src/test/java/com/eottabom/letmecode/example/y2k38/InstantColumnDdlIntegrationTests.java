package com.eottabom.letmecode.example.y2k38;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// schema.sql 없이 Hibernate 가 직접 테이블을 만들게 하면 Instant, LocalDateTime 이 어떤 컬럼이 되는지 확인한다.
// Hibernate 7.2 (Spring Boot 4.0) 기준으로 Instant 도 timestamp 가 아니라 datetime(6) 이 된다.
// 즉 Y2K38 은 ddl-auto 로 만든 컬럼이 아니라, 직접 TIMESTAMP 로 선언한 기존 스키마에서 생긴다.
@DataJpaTest(properties = { "spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class InstantColumnDdlIntegrationTests {

	@Container
	private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void mysqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Test
	@DisplayName("ddl-auto 로 만들면 Instant 와 LocalDateTime 모두 datetime(6) 컬럼이 된다")
	void instantBecomesDatetimeColumn() {
		// when
		String instantColumn = columnType("ts_col");
		String localDateTimeColumn = columnType("dt_col");

		// then
		assertThat(instantColumn).isEqualTo("datetime(6)");
		assertThat(localDateTimeColumn).isEqualTo("datetime(6)");
	}

	private String columnType(String column) {
		return this.jdbcTemplate.queryForObject(
				"SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_NAME = 'time_test' AND COLUMN_NAME = ?",
				String.class, column);
	}

}
