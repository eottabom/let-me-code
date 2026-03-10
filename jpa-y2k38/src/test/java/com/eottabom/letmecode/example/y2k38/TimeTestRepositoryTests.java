package com.eottabom.letmecode.example.y2k38;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TimeTestRepositoryTests {

    @Autowired
    private TimeTestRepository repository;

    @Test
    @DisplayName("DATETIME은 2099년 날짜를 저장할 수 있다")
    void datetimeCanStoreFarFutureDate() {
        // given
        LocalDateTime futureDate = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
        Instant safeTimestamp = Instant.parse("2038-01-19T03:14:07Z");

        // when
        TimeTest entity = repository.saveAndFlush(new TimeTest(futureDate, safeTimestamp));

        // then
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getDatetime()).isEqualTo(futureDate);
    }

    @Test
    @DisplayName("TIMESTAMP 경계값(2038-01-19 03:14:07 UTC)은 정상 저장된다")
    void timestampBoundaryValueShouldBeSaved() {
        // given
        LocalDateTime futureDate = LocalDateTime.of(2038, 1, 19, 3, 14, 7);
        Instant boundaryTimestamp = Instant.parse("2038-01-19T03:14:07Z");

        // when
        TimeTest entity = repository.saveAndFlush(new TimeTest(futureDate, boundaryTimestamp));

        // then
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getTimestamp()).isEqualTo(boundaryTimestamp);
    }

    @Test
    @DisplayName("TIMESTAMP 범위를 넘는 Instant는 애플리케이션 레벨에서 검증하여 차단해야 한다")
    void timestampOverflowShouldBeCaughtByValidator() {
        // given
        // H2는 TIMESTAMP overflow를 강제하지 않으므로, 실제 MySQL에서는 저장 실패함
        // 애플리케이션 레벨에서 TimestampRangeValidator로 사전 검증 필요
        Instant overflowTimestamp = Instant.parse("2038-01-19T03:14:08Z");

        // when & then
        assertThatThrownBy(() ->
                TimestampRangeValidator.validateForMysqlTimestamp(overflowTimestamp)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MySQL TIMESTAMP 범위를 초과");
    }

    @Test
    @DisplayName("DATETIME은 TIMESTAMP 없이도 미래 날짜를 단독 저장할 수 있다")
    void datetimeOnlyWithNullTimestamp() {
        // given
        LocalDateTime farFuture = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

        // when
        TimeTest entity = repository.saveAndFlush(new TimeTest(farFuture, null));

        // then
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getDatetime()).isEqualTo(farFuture);
        assertThat(entity.getTimestamp()).isNull();
    }

}
