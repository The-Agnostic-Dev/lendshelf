package com.lendshelf.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lendshelf.domain.Member;
import com.lendshelf.repository.MemberRepository;

@Service
@Transactional
public class MemberService {

    private final MemberRepository members;
    private final Clock clock;

    public MemberService(MemberRepository members, Clock clock) {
        this.members = members;
        this.clock = clock;
    }

    public Member register(String name, String email, String phone) {
        String card = "LS-%05d".formatted(members.count() + 10001);
        String cleanPhone = phone == null || phone.isBlank() ? null : phone.trim();
        return members.save(new Member(name.trim(), email.trim().toLowerCase(), cleanPhone, card, LocalDate.now(clock)));
    }

    @Transactional(readOnly = true)
    public Member get(Long id) {
        return members.findById(id).orElseThrow(() -> new NotFoundException("Member", id));
    }

    @Transactional(readOnly = true)
    public Member byCard(String cardNumber) {
        return members.findByCardNumber(cardNumber.trim().toUpperCase())
                .orElseThrow(() -> new LibraryException("No member has the card number " + cardNumber + "."));
    }

    @Transactional(readOnly = true)
    public List<Member> all() {
        return members.findAllByOrderByName();
    }
}
