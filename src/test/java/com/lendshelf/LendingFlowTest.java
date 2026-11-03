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

        assertThat(reservationRepository.findById(r2.getId()).orElseThrow().getQueuePosition()).isEqualTo(3);
        assertThat(reservationRepository.findById(r3.getId()).orElseThrow().getQueuePosition()).isEqualTo(4);

        reservations.cancel(r2.getId());

        assertThat(reservationRepository.findById(r3.getId()).orElseThrow().getQueuePosition()).isEqualTo(3);
    }

    @Test
    void cancellingEarlierReservationMovesNextUp() {
        Member first = members.register("First Reader", "first@example.com", null);
        Member second = members.register("Second Reader", "second@example.com", null);
        Book book = findBook("Half of a Yellow Sun");

        var r1 = reservations.reserve(first.getId(), book.getId());
        var r2 = reservations.reserve(second.getId(), book.getId());

        reservations.cancel(r1.getId());

        assertThat(reservationRepository.findById(r2.getId()).orElseThrow().getQueuePosition()).isEqualTo(3);
    }

    @Test
    void cancellingFirstWaitingAfterReadyExcludesReadyAndRenumbersWaiting() {
        Book book = null;
        Reservation dummy = null;
        Member dummyMember = null;
        int page = 0;
        while (true) {
            var pageContent = catalog.search("", page).getContent();
            if (pageContent.isEmpty()) break;
            for (Book candidate : pageContent) {
                if (!reservations.findByBook(candidate.getId()).isEmpty()) continue;
                Member m = members.register("Dummy " + UUID.randomUUID(), "dummy-" + UUID.randomUUID() + "@example.com", null);
                try {
                    dummy = reservations.reserve(m.getId(), candidate.getId());
                    book = candidate;
                    dummyMember = m;
                    break;
                } catch (LibraryException e) {
                    // not all out, try next candidate
                }
            }
            if (book != null) break;
            page++;
        }
        assertThat(book).isNotNull();

        Member new1Member = members.register("New1 " + UUID.randomUUID(), "new1-" + UUID.randomUUID() + "@example.com", null);
        Member new2Member = members.register("New2 " + UUID.randomUUID(), "new2-" + UUID.randomUUID() + "@example.com", null);
        Member new3Member = members.register("New3 " + UUID.randomUUID(), "new3-" + UUID.randomUUID() + "@example.com", null);
        var new1 = reservations.reserve(new1Member.getId(), book.getId());
        var new2 = reservations.reserve(new2Member.getId(), book.getId());
        var new3 = reservations.reserve(new3Member.getId(), book.getId());

        Loan loanToReturn = null;
        for (Member member : members.all()) {
            for (Loan loan : loans.currentLoans(member.getId())) {
                if (loan.getCopy().getBook().getId().equals(book.getId())) {
                    loanToReturn = loan;
                    break;
                }
            }
            if (loanToReturn != null) break;
        }
        assertThat(loanToReturn).isNotNull();

        loans.returnLoan(loanToReturn.getId());

        reservations.cancel(new1.getId());

        var waitingAfterCancel = reservations.findByBook(book.getId());
        assertThat(waitingAfterCancel)
            .noneMatch(r -> r.getId().equals(dummy.getId()))
            .allMatch(r -> r.getStatus() == ReservationStatus.WAITING)
            .hasSize(2);
        assertThat(waitingAfterCancel)
            .extracting(r -> r.getQueuePosition())
            .containsExactlyInAnyOrder(1, 2);

        var new2After = reservationRepository.findById(new2.getId()).orElseThrow();
        var new3After = reservationRepository.findById(new3.getId()).orElseThrow();
        assertThat(new2After.getQueuePosition()).isEqualTo(1);
        assertThat(new3After.getQueuePosition()).isEqualTo(2);
    }
}
