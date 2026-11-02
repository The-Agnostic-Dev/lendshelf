package com.lendshelf;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.lendshelf.config.LibraryProperties;
import com.lendshelf.service.FeeCalculator;

class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator(
            new LibraryProperties(14, 0.15, 2, 5, 10, false),
            Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void noFeeWhenNotLate() {
        assertThat(calculator.feeForDays(0)).isZero();
        assertThat(calculator.feeForDays(-4)).isZero();
    }

    @Test
    void chargesPerDayLate() {
        assertThat(calculator.feeForDays(1)).isEqualTo(0.15);
        assertThat(calculator.feeForDays(2)).isEqualTo(0.30);
        assertThat(calculator.feeForDays(10)).isEqualTo(1.50);
    }
}
