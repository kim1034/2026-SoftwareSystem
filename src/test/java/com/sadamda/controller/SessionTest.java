package com.sadamda.controller;

import com.sadamda.common.AccessDeniedException;
import com.sadamda.common.LoginRequiredException;
import com.sadamda.model.member.Member;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionTest {

    static Member member(long id, Member.Role role) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        when(member.getRole()).thenReturn(role);
        return member;
    }

    @Test
    void guest_requireLogin_throws() {
        assertThrows(LoginRequiredException.class, () -> new Session().requireLogin());
    }

    @Test
    void member_requireAdmin_throwsAccessDenied() {
        Session session = new Session();
        session.login(member(1L, Member.Role.MEMBER));
        assertThrows(AccessDeniedException.class, session::requireAdmin);
    }

    @Test
    void admin_requireAdmin_returnsMember() {
        Session session = new Session();
        Member admin = member(9L, Member.Role.ADMIN);
        session.login(admin);
        assertSame(admin, session.requireAdmin());
        assertTrue(session.isAdmin());
    }
}
