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
import com.lendshelf.domain.Reservation;
import com.lendshelf.domain.ReservationStatus;
import java.util.UUID;
import com.lendshelf.service.CatalogService;
import com.lendshelf.service.LibraryException;
import com.lendshelf.service.LoanService;
import com.lendshelf.service.MemberService;
import com.lendshelf.service.ReservationService;
import com.lendshelf.repository.ReservationRepository;

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

    @Autowired
    ReservationRepository reservationRepository;

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
    void cancellingReservationRenumbersQueue() {
        Member first = members.register("First Reader", "first@example.com", null);
        Member second = members.register("Second Reader", "second@example.com", null);
        Member third = members.register("Third Reader", "third@example.com", null);
        Book book = findBook("Half of a Yellow Sun");

        var r1 = reservations.reserve(first.getId(), book.getId());
        var r2 = reservations.reserve(second.getId(), book.getId());
        var r3 = reservations.reserve(third.getId(), book.getId());

        reservations.cancel(r1.getId());

        var waitingAfterFirstCancel = reservations.findByBook(book.getId());
        assertThat(waitingAfterFirstCancel)
            .filteredOn(r -> r.getId().equals(r2.getId()) || r.getId().equals(r3.getId()))
            .hasSize(2);
        assertThat(waitingAfterFirstCancel)
            .filteredOn(r -> r.getId().equals(r2.getId()))
            .singleElement()
            .extracting(Reservation::getQueuePosition)
            .isEqualTo(1);
        assertThat(waitingAfterFirstCancel)
            .filteredOn(r -> r.getId().equals(r3.getId()))
            .singleElement()
            .extracting(Reservation::getQueuePosition)
            .isEqualTo(2);

        reservations.cancel(r2.getId());

        var waitingAfterSecondCancel = reservations.findByBook(book.getId());
        assertThat(waitingAfterSecondCancel)
            .filteredOn(r -> r.getId().equals(r3.getId()))
            .hasSize(1);
        assertThat(waitingAfterSecondCancel)
            .filteredOn(r -> r.getId().equals(r3.getId()))
            .singleElement()
            .extracting(Reservation::getQueuePosition)
            .isEqualTo(1);
    }

    @Test
    void cancellingEarlierReservationMovesNextUp() {
        Member first = members.register("First Reader", "first@example.com", null);
        Member second = members.register("Second Reader", "second@example.com", null);
        Book book = findBook("Half of a Yellow Sun");

        var r1 = reservations.reserve(first.getId(), book.getId());
        var r2 = reservations.reserve(second.getId(), book.getId());

        reservations.cancel(r1.getId());

        var waiting = reservations.findByBook(book.getId());
        assertThat(waiting)
            .filteredOn(r -> r.getId().equals(r2.getId()))
            .hasSize(1);
        assertThat(waiting)
            .filteredOn(r -> r.getId().equals(r2.getId()))
            .singleElement()
            .extracting(Reservation::getQueuePosition)
            .isEqualTo(1);
    }

    @Test
    void cancellingFirstWaitingAfterReadyExcludesReadyAndRenumbersWaiting() {
        Book book = findBook("A Brief History of Time");
        Member dummyMember = members.register("Dummy " + UUID.randomUUID(), "dummy-" + UUID.randomUUID() + "@example.com", null);
        Loan dummyLoan = loans.borrow(dummyMember.getId(), book.getId());
        final Reservation[] dummyHolder = new Reservation[1];
        dummyHolder[0] = reservations.reserve(dummyMember.getId(), book.getId());
        assertThat(dummyHolder[0]).isNotNull();

        Member new1Member = members.register("New1 " + UUID.randomUUID(), "new1-" + UUID.randomUUID() + "@example.com", null);
        Member new2Member = members.register("New2 " + UUID.randomUUID(), "new2-" + UUID.randomUUID() + "@example.com", null);
        Member new3Member = members.register("New3 " + UUID.randomUUID(), "new3-" + UUID.randomUUID() + "@example.com", null);
        var new1 = reservations.reserve(new1Member.getId(), book.getId());
        var new2 = reservations.reserve(new2Member.getId(), book.getId());
        var new3 = reservations.reserve(new3Member.getId(), book.getId());

        Loan loanToReturn = dummyLoan;

        loans.returnLoan(loanToReturn.getId());

        reservations.cancel(new1.getId());

        var waitingAfterCancel = reservations.findByBook(book.getId());
        assertThat(waitingAfterCancel)
            .noneMatch(r -> r.getId().equals(dummyHolder[0].getId()))
            .allMatch(r -> r.getStatus() == ReservationStatus.WAITING)
            .hasSize(2);
        assertThat(waitingAfterCancel)
            .extracting(r -> r.getQueuePosition())
            .containsExactly(1, 2);
        assertThat(waitingAfterCancel)
            .extracting(Reservation::getId)
            .containsExactly(new2.getId(), new3.getId());
    }

    @Test
    void findByBookReturnsSequentialPositionsIgnoringStoredQueuePositions() {
        Member first = members.register("Stale First", "stale1@example.com", null);
        Member second = members.register("Stale Second", "stale2@example.com", null);
        Book book = findBook("Half of a Yellow Sun");

        var r1 = reservations.reserve(first.getId(), book.getId());
        var r2 = reservations.reserve(second.getId(), book.getId());

        // Set stored queuePosition to stale duplicate values
        r1.setQueuePosition(10);
        r2.setQueuePosition(10);
        reservationRepository.save(r1);
        reservationRepository.save(r2);

        var waiting = reservations.findByBook(book.getId());
        assertThat(waiting)
            .filteredOn(r -> r.getId().equals(r1.getId()) || r.getId().equals(r2.getId()))
            .hasSize(2);
        assertThat(waiting)
            .filteredOn(r -> r.getId().equals(r1.getId()) || r.getId().equals(r2.getId()))
            .extracting(Reservation::getQueuePosition)
            .containsExactly(1, 2);
        assertThat(waiting)
            .filteredOn(r -> r.getId().equals(r1.getId()) || r.getId().equals(r2.getId()))
            .extracting(Reservation::getId)
            .containsExactly(r1.getId(), r2.getId());
    }

    @Test
    void passOnMovesEveryoneBehindUp() {
        Member borrower = members.register("Borrower " + UUID.randomUUID(), "borrower-" + UUID.randomUUID() + "@example.com", null);
        Book book = findBook("A Brief History of Time");
        Loan loan = loans.borrow(borrower.getId(), book.getId());

        Member firstWaiter = members.register("Waiter1 " + UUID.randomUUID(), "waiter1-" + UUID.randomUUID() + "@example.com", null);
        Member secondWaiter = members.register("Waiter2 " + UUID.randomUUID(), "waiter2-" + UUID.randomUUID() + "@example.com", null);
        Member thirdWaiter = members.register("Waiter3 " + UUID.randomUUID(), "waiter3-" + UUID.randomUUID() + "@example.com", null);

        var r1 = reservations.reserve(firstWaiter.getId(), book.getId());
        var r2 = reservations.reserve(secondWaiter.getId(), book.getId());
        var r3 = reservations.reserve(thirdWaiter.getId(), book.getId());

        loans.returnLoan(loan.getId());

        var waiting = reservations.findByBook(book.getId());
        assertThat(waiting)
            .hasSize(2)
            .noneMatch(r -> r.getId().equals(r1.getId()))
            .allMatch(r -> r.getStatus() == ReservationStatus.WAITING)
            .extracting(r -> r.getQueuePosition())
            .containsExactly(1, 2);
        assertThat(waiting)
            .extracting(Reservation::getId)
            .containsExactly(r2.getId(), r3.getId());
    }
}
