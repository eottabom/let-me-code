package com.eottabom.letmecode.example.y2k38.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CouponRepositoryTests {

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("쿠폰 만료일은 2099년이어도 DATETIME으로 정상 저장된다")
    void couponExpiresAtFarFuture() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
        Coupon coupon = new Coupon("WELCOME-2099", expiresAt);

        // when
        Coupon saved = couponRepository.saveAndFlush(coupon);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("쿠폰 만료일이 2040년이어도 DATETIME으로 정상 저장된다")
    void couponExpiresAtAfter2038() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2040, 6, 15, 12, 0, 0);
        Coupon coupon = new Coupon("SUMMER-2040", expiresAt);

        // when
        Coupon saved = couponRepository.saveAndFlush(coupon);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("쿠폰 만료일이 현재 근처 날짜여도 정상 저장된다")
    void couponExpiresAtNearFuture() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
        Coupon coupon = new Coupon("YEAR-END-2026", expiresAt);

        // when
        Coupon saved = couponRepository.saveAndFlush(coupon);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
    }

}
