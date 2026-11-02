package com.lendshelf.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lendshelf.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByOrderByName();

    Optional<Member> findByCardNumber(String cardNumber);
}
