package com.lendshelf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.lendshelf.domain.Book;
import com.lendshelf.domain.CopyStatus;
import com.lendshelf.domain.Loan;
import com.lendshelf.domain.Member;
import com.lendshelf.domain.ReservationStatus;
import com.lendshelf.service.CatalogService;
import com.lendshelf.service.LibraryException;
import com.lendshelf.service.LoanService;
import com.lendshelf.service.MemberService;
import com.lendshelf.service.ReservationService;

@SpringBootTest
@Transactional
class LendingFlowTest {

    @Autowired
    CatalogService catalog;

    @Autowired
    LoanService loans;

    @Autowired
    ReservationService reservations;

    @Autowired
    MemberService members;

    private Book findBook(String title) {
        return catalog.search(title, 0).getContent().stream()
                .filter(b -> b.getTitle().equals(title))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void catalogueIsSortedByTitle() {
        var first = catalog.search("", 0).getContent();
        assertThat(first).hasSize(10);
        assertThat(first.get(0).getTitle()).isEqualTo("A Brief History of Time");
    }

    @Test
    void searchFindsByTitleAndAuthor() {
        assertThat(catalog.search("Things Fall", 0).getContent())
                .extracting(Book::getTitle).containsExactly("Things Fall Apart");
        assertThat(catalog.search("Austen", 0).getTotalElements()).isEqualTo(3);
    }

    @Test
    void borrowingTakesACopyOffTheShelf() {
        Member reader = members.register("Test Reader", "reader@example.com", "");
        Book book = findBook("Middlemarch");

        Loan loan = loans.borrow(reader.getId(), book.getId());

        assertThat(loan.isOpen()).isTrue();
        assertThat(loan.getCopy().getStatus()).isEqualTo(CopyStatus.ON_LOAN);
        assertThatThrownBy(() -> loans.borrow(reader.getId(), book.getId()))
                .isInstanceOf(LibraryException.class);
    }

    @Test
    void returningPutsTheCopyBack() {
        Member reader = members.register("Test Reader", "reader@example.com", null);
        Book book = findBook("Kindred");
        Loan loan = loans.borrow(reader.getId(), book.getId());

        loans.returnLoan(loan.getId());

        assertThat(loan.isOpen()).isFalse();
        assertThat(loan.getCopy().getStatus()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void renewingExtendsFromTheCurrentDueDate() {
        Member reader = members.register("Test Reader", "reader@example.com", null);
        Loan loan = loans.borrow(reader.getId(), findBook("Persuasion").getId());
        var due = loan.getDueOn();

        loans.renew(loan.getId());

        assertThat(loan.getDueOn()).isEqualTo(due.plusDays(14));
        assertThat(loan.getRenewals()).isEqualTo(1);
    }

    @Test
    void reservingIsOnlyForBooksThatAreAllOut() {
        Member reader = members.register("Test Reader", "reader@example.com", null);

        assertThatThrownBy(() -> reservations.reserve(reader.getId(), findBook("Matilda").getId()))
                .isInstanceOf(LibraryException.class);

        var reservation = reservations.reserve(reader.getId(), findBook("Half of a Yellow Sun").getId());
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.WAITING);
    }

    @Test
    void returnedCopyIsHeldForTheNextReservation() {
        Member samuel = members.all().stream().filter(m -> m.getName().equals("Samuel Otieno")).findFirst().orElseThrow();
        Loan loan = loans.currentLoans(samuel.getId()).getFirst();

        loans.returnLoan(loan.getId());

        assertThat(loan.getCopy().getStatus()).isEqualTo(CopyStatus.ON_HOLD);
    }

    @Test
    void cancellingReservationMovesLaterQueuePositionsUp() {
        Member m1 = members.register("Reader One", "reader1@example.com", null);
        Member m2 = members.register("Reader Two", "reader2@example.com", null);
        Member m3 = members.register("Reader Three", "reader3@example.com", null);
        Member m4 = members.register("Reader Four", "reader4@example.com", null);
        Book book = findBook("Half of a Yellow Sun");
        var r1 = reservations.reserve(m1.getId(), book.getId());
        var r2 = reservations.reserve(m2.getId(), book.getId());
        var r3 = reservations.reserve(m3.getId(), book.getId());
        var r4 = reservations.reserve(m4.getId(), book.getId());

        assertThat(r1.getPosition()).isEqualTo(1);
        assertThat(r2.getPosition()).isEqualTo(2);
        assertThat(r3.getPosition()).isEqualTo(3);
        assertThat(r4.getPosition()).isEqualTo(4);

        reservations.cancel(r3.getId());

        assertThat(r1.getPosition()).isEqualTo(1);
        assertThat(r2.getPosition()).isEqualTo(2);
        assertThat(r4.getPosition()).isEqualTo(3);
    }
}
