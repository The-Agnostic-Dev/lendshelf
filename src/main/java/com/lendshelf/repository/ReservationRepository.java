package com.lendshelf.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.lendshelf.domain.Reservation;
import com.lendshelf.domain.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByBookIdAndStatusOrderByIdAsc(Long bookId, ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findFirstByBookIdAndStatusOrderByIdAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, ReservationStatus status);

    List<Reservation> findByMemberIdAndStatusInOrderByIdAsc(Long memberId, Collection<ReservationStatus> statuses);

    boolean existsByBookIdAndMemberIdAndStatusIn(Long bookId, Long memberId, Collection<ReservationStatus> statuses);

    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    long countByStatus(ReservationStatus status);
}
