package com.eottabom.letmecode.example.y2k38;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimestampRangeValidatorTests {

	@Test
	@DisplayName("2038-01-19 03:14:07 UTC 까지는 허용")
	void allowBoundaryValue() {
		assertThatCode(() -> TimestampRangeValidator.validateForMysqlTimestamp(Instant.parse("2038-01-19T03:14:07Z")))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("2038-01-19 03:14:08 UTC 부터는 차단")
	void rejectOverflowValue() {
		assertThatThrownBy(
				() -> TimestampRangeValidator.validateForMysqlTimestamp(Instant.parse("2038-01-19T03:14:08Z")))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("null은 허용")
	void allowNull() {
		assertThatCode(() -> TimestampRangeValidator.validateForMysqlTimestamp(null)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("2099년 값은 차단")
	void rejectFarFuture() {
		assertThatThrownBy(
				() -> TimestampRangeValidator.validateForMysqlTimestamp(Instant.parse("2099-12-31T23:59:59Z")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("MySQL TIMESTAMP 범위를 초과");
	}

}
