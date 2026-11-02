package com.lendshelf.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lendshelf.config.LibraryProperties;
import com.lendshelf.domain.Copy;
import com.lendshelf.domain.CopyStatus;
import com.lendshelf.domain.Loan;
import com.lendshelf.domain.Member;
import com.lendshelf.domain.Reservation;
import com.lendshelf.domain.ReservationStatus;
import com.lendshelf.repository.CopyRepository;
import com.lendshelf.repository.LoanRepository;
import com.lendshelf.repository.MemberRepository;

@Service
@Transactional
public class LoanService {

    private final LoanRepository loans;
    private final CopyRepository copies;
    private final MemberRepository members;
    private final ReservationService reservations;
    private final FeeCalculator fees;
    private final LibraryProperties properties;
    private final Clock clock;

    public LoanService(LoanRepository loans, CopyRepository copies, MemberRepository members,
            ReservationService reservations, FeeCalculator fees, LibraryProperties properties, Clock clock) {
        this.loans = loans;
        this.copies = copies;
        this.members = members;
        this.reservations = reservations;
        this.fees = fees;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Lends the member a copy of the book. A copy on hold for the member is used first,
     * otherwise the first copy on the shelf.
     */
    public Loan borrow(Long memberId, Long bookId) {
        Member member = findMember(memberId);
        Optional<Reservation> ready = reservations.readyFor(bookId, memberId);

        Copy copy;
        if (ready.isPresent()) {
            copy = copies.findFirstByBookIdAndStatusOrderByBarcode(bookId, CopyStatus.ON_HOLD)
                    .orElseThrow(() -> new LibraryException("The held copy could not be found at the desk."));
        } else {
            copy = copies.findFirstByBookIdAndStatusOrderByBarcode(bookId, CopyStatus.AVAILABLE)
                    .orElseThrow(() -> new LibraryException("No copies are on the shelf right now. You can reserve it instead."));
        }
        return lend(member, copy, ready);
    }

    /**
     * Desk checkout by scanning the barcode on a specific copy.
     */
    public Loan checkout(Long memberId, String barcode) {
        Member member = findMember(memberId);
        Copy copy = copies.findByBarcode(barcode.trim())
                .orElseThrow(() -> new LibraryException("No copy has the barcode " + barcode + "."));
        Optional<Reservation> ready = reservations.readyFor(copy.getBook().getId(), memberId);

        boolean heldForMember = copy.getStatus() == CopyStatus.ON_HOLD && ready.isPresent();
        if (copy.getStatus() != CopyStatus.AVAILABLE && !heldForMember) {
            throw new LibraryException("Copy " + copy.getBarcode() + " is " + copy.getStatus().getLabel().toLowerCase() + ".");
        }
        return lend(member, copy, ready);
    }

    private Loan lend(Member member, Copy copy, Optional<Reservation> ready) {
        if (loans.countByMemberIdAndReturnedOnIsNull(member.getId()) >= properties.maxOpenLoans()) {
            throw new LibraryException(member.getName() + " already has " + properties.maxOpenLoans() + " books out.");
        }
        LocalDate today = LocalDate.now(clock);
        copy.setStatus(CopyStatus.ON_LOAN);
        ready.ifPresent(reservation -> reservation.setStatus(ReservationStatus.FULFILLED));
        return loans.save(new Loan(copy, member, today, dueDateFor(today)));
    }

    public Loan returnLoan(Long loanId) {
        Loan loan = findLoan(loanId);
        if (!loan.isOpen()) {
            throw new LibraryException(loan.getCopy().getBook().getTitle() + " was already returned.");
        }
        LocalDate today = LocalDate.now(clock);
        loan.markReturned(today, fees.feeForDays(loan.daysOverdue(today)));
        reservations.passOn(loan.getCopy());
        return loan;
    }

    public Loan renew(Long loanId) {
        Loan loan = findLoan(loanId);
        if (!loan.isOpen()) {
            throw new LibraryException("Only books that are still out can be renewed.");
        }
        if (loan.getRenewals() > properties.maxRenewals()) {
            throw new LibraryException("This loan has already been renewed " + properties.maxRenewals() + " times.");
        }
        if (!reservations.queueFor(loan.getCopy().getBook().getId()).isEmpty()) {
            throw new LibraryException("Someone is waiting for " + loan.getCopy().getBook().getTitle() + ", so it cannot be renewed.");
        }
        loan.renew(loan.getDueOn().plusDays(properties.loanDays()));
        return loan;
    }

    @Transactional(readOnly = true)
    public List<Loan> currentLoans(Long memberId) {
        return loans.findByMemberIdAndReturnedOnIsNullOrderByDueOn(memberId);
    }

    @Transactional(readOnly = true)
    public List<Loan> pastLoans(Long memberId) {
        return loans.findByMemberIdAndReturnedOnIsNotNullOrderByReturnedOnDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<Loan> overdueLoans() {
        return loans.findByReturnedOnIsNullAndDueOnBeforeOrderByDueOn(LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public long copiesOnLoan() {
        return copies.countByStatus(CopyStatus.ON_LOAN);
    }

    private LocalDate dueDateFor(LocalDate borrowedOn) {
        return borrowedOn.plusWeeks(properties.loanDays());
    }

    private Member findMember(Long memberId) {
        return members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));
    }

    private Loan findLoan(Long loanId) {
        return loans.findById(loanId).orElseThrow(() -> new NotFoundException("Loan", loanId));
    }
}
