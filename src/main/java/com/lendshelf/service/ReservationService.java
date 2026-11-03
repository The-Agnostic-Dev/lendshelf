package com.lendshelf.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lendshelf.domain.Book;
import com.lendshelf.domain.Copy;
import com.lendshelf.domain.CopyStatus;
import com.lendshelf.domain.Member;
import com.lendshelf.domain.Reservation;
import com.lendshelf.domain.ReservationStatus;
import com.lendshelf.repository.BookRepository;
import com.lendshelf.repository.CopyRepository;
import com.lendshelf.repository.MemberRepository;
import com.lendshelf.repository.ReservationRepository;

@Service
@Transactional
public class ReservationService {

    private static final Set<ReservationStatus> ACTIVE = EnumSet.of(ReservationStatus.WAITING, ReservationStatus.READY);

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final MemberRepository members;
    private final CopyRepository copies;
    private final Clock clock;

    public ReservationService(ReservationRepository reservations, BookRepository books, MemberRepository members,
            CopyRepository copies, Clock clock) {
        this.reservations = reservations;
        this.books = books;
        this.members = members;
        this.copies = copies;
        this.clock = clock;
    }

    public Reservation reserve(Long memberId, Long bookId) {
        Member member = members.findById(memberId).orElseThrow(() -> new NotFoundException("Member", memberId));
        Book book = books.findById(bookId).orElseThrow(() -> new NotFoundException("Book", bookId));

        if (copies.countByBookIdAndStatus(bookId, CopyStatus.AVAILABLE) > 0) {
            throw new LibraryException("A copy of " + book.getTitle() + " is on the shelf, so it can be borrowed right away.");
        }
        if (reservations.existsByBookIdAndMemberIdAndStatusIn(bookId, memberId, ACTIVE)) {
            throw new LibraryException(member.getName() + " already has a reservation for " + book.getTitle() + ".");
        }

        int position = (int) reservations.countByBookIdAndStatus(bookId, ReservationStatus.WAITING) + 1;
        return reservations.save(new Reservation(book, member, LocalDateTime.now(clock), position));
    }

    public Reservation cancel(Long reservationId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation", reservationId));
        if (!reservation.isActive()) {
            throw new LibraryException("This reservation is no longer active.");
        }
        boolean wasHoldingCopy = reservation.getStatus() == ReservationStatus.READY;
        reservation.setStatus(ReservationStatus.CANCELLED);

        if (wasHoldingCopy) {
            copies.findByBookIdOrderByBarcode(reservation.getBook().getId()).stream()
                    .filter(copy -> copy.getStatus() == CopyStatus.ON_HOLD)
                    .findFirst()
                    .ifPresent(this::passOn);
        }
        return reservation;
    }

    /**
     * A copy came back to the desk: hold it for the next person in the queue, or put it
     * back on the shelf.
     */
    public void passOn(Copy copy) {
        Optional<Reservation> next = reservations
                .findFirstByBookIdAndStatusOrderByIdAsc(copy.getBook().getId(), ReservationStatus.WAITING);
        if (next.isPresent()) {
            next.get().setStatus(ReservationStatus.READY);
            copy.setStatus(CopyStatus.ON_HOLD);
        } else {
            copy.setStatus(CopyStatus.AVAILABLE);
        }
    }

    /**
     * The ready reservation this member can collect for the given book, if any.
     */
    public Optional<Reservation> readyFor(Long bookId, Long memberId) {
        return reservations.findFirstByBookIdAndMemberIdAndStatus(bookId, memberId, ReservationStatus.READY);
    }

    @Transactional(readOnly = true)
    public List<Reservation> activeFor(Long memberId) {
        List<Reservation> memberReservations = reservations.findByMemberIdAndStatusInOrderByIdAsc(memberId, ACTIVE);
        applyQueuePositions(memberReservations);
        return memberReservations;
    }

    @Transactional(readOnly = true)
    public List<Reservation> queueFor(Long bookId) {
        List<Reservation> queue = reservations.findByBookIdAndStatusInOrderByIdAsc(bookId, ACTIVE);
        applyQueuePositions(queue);
        return queue;
    }

    private void applyQueuePositions(List<Reservation> reservations) {
        Set<Long> bookIds = reservations.stream()
                .map(r -> r.getBook().getId())
                .collect(Collectors.toSet());

        Map<Long, List<Reservation>> waitingByBook = bookIds.stream()
                .collect(Collectors.toMap(
                        bookId -> bookId,
                        bookId -> this.reservations.findByBookIdAndStatusOrderByIdAsc(bookId, ReservationStatus.WAITING)));

        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.WAITING) {
                List<Reservation> waiting = waitingByBook.get(reservation.getBook().getId());
                int position = 0;
                for (int i = 0; i < waiting.size(); i++) {
                    if (waiting.get(i).getId().equals(reservation.getId())) {
                        position = i + 1;
                        break;
                    }
                }
                reservation.setQueuePosition(position);
            } else {
                reservation.setQueuePosition(0);
            }
        }
    }

    @Transactional(readOnly = true)
    public long waitingCount() {
        return reservations.countByStatus(ReservationStatus.WAITING);
    }
}
