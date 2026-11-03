package com.lendshelf;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.lendshelf.domain.Book;
import com.lendshelf.domain.Copy;
import com.lendshelf.domain.Loan;
import com.lendshelf.domain.Member;

class LoanTest {

    private final Book book = new Book("Emma", "Jane Austen", "9780141439587", 1815, "Classics");
    private final Member member = new Member("Test Reader", "reader@example.com", null, "LS-99999", LocalDate.of(2026, 1, 1));

    @Test
    void notOverdueOnTheDueDate() {
        Loan loan = new Loan(new Copy(book, "X1"), member, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15));
        assertThat(loan.isOverdue(LocalDate.of(2026, 3, 15))).isFalse();
        assertThat(loan.daysOverdue(LocalDate.of(2026, 3, 15))).isZero();
    }

    @Test
    void countsDaysPastDue() {
        Loan loan = new Loan(new Copy(book, "X1"), member, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15));
        assertThat(loan.isOverdue(LocalDate.of(2026, 3, 18))).isTrue();
        assertThat(loan.daysOverdue(LocalDate.of(2026, 3, 18))).isEqualTo(3);
    }

    @Test
    void countsFortyDaysPastDue() {
        LocalDate reference = LocalDate.of(2026, 4, 24);
        Loan loan = new Loan(new Copy(book, "X1"), member, reference.minusDays(50), reference.minusDays(40));
        assertThat(loan.isOverdue(reference)).isTrue();
        assertThat(loan.daysOverdue(reference)).isEqualTo(40);
    }

    @Test
    void returnedLoanStopsAccruing() {
        Loan loan = new Loan(new Copy(book, "X1"), member, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 15));
        loan.markReturned(LocalDate.of(2026, 3, 17), 0.30);
        assertThat(loan.isOpen()).isFalse();
        assertThat(loan.daysOverdue(LocalDate.of(2026, 3, 25))).isEqualTo(2);
    }
}
