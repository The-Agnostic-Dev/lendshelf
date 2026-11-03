package com.lendshelf.web;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * The member currently being served at a circulation desk, so the librarian can scan
 * several books without re-entering the card number each time.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
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
