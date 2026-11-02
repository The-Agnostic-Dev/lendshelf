package com.lendshelf.web;

import org.springframework.stereotype.Component;

/**
 * The member currently being served at a circulation desk, so the librarian can scan
 * several books without re-entering the card number each time.
 */
@Component
public class DeskSession {

    private Long memberId;

    public Long getMemberId() {
        return memberId;
    }

    public void start(Long memberId) {
        this.memberId = memberId;
    }

    public void finish() {
        this.memberId = null;
    }

    public boolean isActive() {
        return memberId != null;
    }
}
