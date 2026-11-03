package com.lendshelf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.lendshelf.domain.Book;
import com.lendshelf.domain.CopyStatus;
import com.lendshelf.domain.Loan;
import com.lendshelf.domain.Member;
import com.lendshelf.domain.Reservation;
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

    private Member memberNamed(String name) {
        return members.all().stream().filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
    }

    private int positionOf(Reservation reservation) {
        return reservations.queuePositions(List.of(reservation)).get(reservation.getId());
    }

    // Half of a Yellow Sun is all out on loan, with Kwame (1) and Ruth (2) already waiting.

    @Test
    void newReservationJoinsTheBackOfTheQueue() {
        Member reader = members.register("Test Reader", "reader@example.com", null);
        var reservation = reservations.reserve(reader.getId(), findBook("Half of a Yellow Sun").getId());

        assertThat(positionOf(reservation)).isEqualTo(3);
    }

    @Test
    void cancellingMovesEveryoneBehindUpOne() {
        Long bookId = findBook("Half of a Yellow Sun").getId();
        var third = reservations.reserve(members.register("Friend", "friend@example.com", null).getId(), bookId);
        var fourth = reservations.reserve(members.register("Me", "me@example.com", null).getId(), bookId);
        var fifth = reservations.reserve(members.register("Later", "later@example.com", null).getId(), bookId);
        assertThat(positionOf(fourth)).isEqualTo(4);

        reservations.cancel(third.getId());

        assertThat(positionOf(fourth)).isEqualTo(3);
        assertThat(positionOf(fifth)).isEqualTo(4);
    }

    @Test
    void handingACopyOnMovesEveryoneStillWaitingUpOne() {
        Long bookId = findBook("Half of a Yellow Sun").getId();
        var third = reservations.reserve(members.register("Test Reader", "reader@example.com", null).getId(), bookId);

        Loan loan = loans.currentLoans(memberNamed("Samuel Otieno").getId()).getFirst();
        loans.returnLoan(loan.getId());

        assertThat(reservations.queueFor(bookId)).extracting(r -> r.getMember().getName())
                .containsExactly("Ruth Wanjiru", "Test Reader");
        assertThat(positionOf(third)).isEqualTo(2);
    }

    @Test
    void noTwoWaitingMembersShareANumberAfterCancelAndRejoin() {
        Long bookId = findBook("Half of a Yellow Sun").getId();
        var a = reservations.reserve(members.register("A", "a@example.com", null).getId(), bookId);
        reservations.reserve(members.register("B", "b@example.com", null).getId(), bookId);
        reservations.cancel(a.getId());
        reservations.reserve(members.register("C", "c@example.com", null).getId(), bookId);

        var queue = reservations.queueFor(bookId);
        var numbers = reservations.queuePositions(queue).values();
        assertThat(numbers).containsExactlyInAnyOrder(1, 2, 3, 4);
    }
}
