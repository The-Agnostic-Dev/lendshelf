package com.lendshelf.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lendshelf.domain.Reservation;
import com.lendshelf.domain.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByBookIdAndStatusOrderByCreatedAt(Long bookId, ReservationStatus status);

    List<Reservation> findByBookIdAndStatusOrderByReservedAtAscIdAsc(Long bookId, ReservationStatus status);


    Optional<Reservation> findFirstByBookIdAndStatusOrderByCreatedAt(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, ReservationStatus status);

    List<Reservation> findByMemberIdAndStatusInOrderByCreatedAt(Long memberId, Collection<ReservationStatus> statuses);

    boolean existsByBookIdAndMemberIdAndStatusIn(Long bookId, Long memberId, Collection<ReservationStatus> statuses);

    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    long countByStatus(ReservationStatus status);
}
