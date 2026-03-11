package com.eottabom.letmecode.example.y2k38;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_test")
public class TimeTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dt_col")
    private LocalDateTime datetime;

    @Column(name = "ts_col")
    private Instant timestamp;

    protected TimeTest() {
    }

    public TimeTest(LocalDateTime datetime, Instant timestamp) {
        this.datetime = datetime;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

}
