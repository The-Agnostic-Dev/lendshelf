package com.lendshelf.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.lendshelf.config.LibraryProperties;
import com.lendshelf.domain.Loan;

/**
 * Works out late fees. The branch charges a flat amount per day late, with no grace period.
 */
@Component
public class FeeCalculator {

    private final LibraryProperties properties;
    private final Clock clock;

    public FeeCalculator(LibraryProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public double feeForDays(long daysLate) {
        if (daysLate <= 0) {
            return 0;
        }
        double fee = daysLate * properties.lateFeePerDay();
        return Math.floor(fee * 100) / 100;
    }

    /**
     * Fee owed on a loan: the recorded fee if it was returned, otherwise what it has
     * accrued so far.
     */
    public double feeFor(Loan loan) {
        if (!loan.isOpen()) {
            return loan.getFeeCharged();
        }
        return feeForDays(loan.daysOverdue(LocalDate.now(clock)));
    }
}
