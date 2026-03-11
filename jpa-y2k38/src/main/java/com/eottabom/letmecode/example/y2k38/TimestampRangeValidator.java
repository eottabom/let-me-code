package com.eottabom.letmecode.example.y2k38;

import java.time.Instant;

public final class TimestampRangeValidator {

    private static final Instant MYSQL_TIMESTAMP_MAX =
            Instant.parse("2038-01-19T03:14:07Z");

    private TimestampRangeValidator() {
    }

    public static void validateForMysqlTimestamp(Instant instant) {
        if (instant != null && instant.isAfter(MYSQL_TIMESTAMP_MAX)) {
            throw new IllegalArgumentException(
                    "MySQL TIMESTAMP 범위를 초과했습니다: " + instant);
        }
    }

}
