package com.eottabom.letmecode.example.y2k38;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 MySQL 에서 TIMESTAMP 범위를 넘는 값을 저장하면 어떻게 되는지 확인한다.
// @DataJpaTest 는 기본적으로 임베디드 DB(H2)로 바꿔치기하는데, H2 는 TIMESTAMP 범위를 넘어도 예외를 내지 않는다.
// 그래서 replace = NONE 으로 Testcontainers MySQL 을 그대로 쓰게 한다. Docker 가 없으면 skip 된다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MysqlTimestampIntegrationTests {

	@Container
	private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

	@Autowired
	private TimeTestRepository repository;

	@DynamicPropertySource
	static void mysqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Test
	@DisplayName("DATETIME 은 2099년 날짜를 저장할 수 있다")
	void datetimeCanStoreFarFutureDate() {
		// given
		LocalDateTime futureDate = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

		// when
		TimeTest entity = this.repository.saveAndFlush(new TimeTest(futureDate, null));

		// then
		assertThat(entity.getId()).isNotNull();
		assertThat(entity.getDatetime()).isEqualTo(futureDate);
	}

	@Test
	@DisplayName("TIMESTAMP 경계값(2038-01-19 03:14:07 UTC)은 저장된다")
	void timestampBoundaryValueIsSaved() {
		// given
		Instant boundary = Instant.parse("2038-01-19T03:14:07Z");

		// when
		TimeTest entity = this.repository.saveAndFlush(new TimeTest(null, boundary));

		// then
		assertThat(entity.getId()).isNotNull();
	}

	@Test
	@DisplayName("TIMESTAMP 범위를 1초라도 넘으면 strict sql_mode 에서 저장이 실패한다")
	void timestampOverflowFailsOnMysql() {
		// given
		Instant overflow = Instant.parse("2038-01-19T03:14:08Z");

		// when & then
		assertThatThrownBy(() -> this.repository.saveAndFlush(new TimeTest(null, overflow)))
			.hasRootCauseMessage("Data truncation: Incorrect datetime value: '2038-01-19 03:14:08' for column 'ts_col' at row 1");
	}

}
