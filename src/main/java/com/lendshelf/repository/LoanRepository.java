package com.lendshelf.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lendshelf.domain.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByMemberIdAndReturnedOnIsNullOrderByDueOn(Long memberId);

    List<Loan> findByMemberIdAndReturnedOnIsNotNullOrderByReturnedOnDesc(Long memberId);

    List<Loan> findByReturnedOnIsNullAndDueOnBeforeOrderByDueOn(LocalDate date);

    long countByMemberIdAndReturnedOnIsNull(Long memberId);
}
