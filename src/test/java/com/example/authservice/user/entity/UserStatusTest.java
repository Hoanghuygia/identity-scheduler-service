package com.example.authservice.user.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserStatusTest {

    @Test
    void shouldHaveExpectedValues() {
        UserStatus[] statuses = UserStatus.values();
        assertEquals(4, statuses.length);
        
        assertEquals("PENDING", UserStatus.PENDING.name());
        assertEquals("ACTIVE", UserStatus.ACTIVE.name());
        assertEquals("SUSPENDED", UserStatus.SUSPENDED.name());
        assertEquals("LOCKED", UserStatus.LOCKED.name());
    }

    @Test
    void shouldConvertFromString() {
        assertEquals(UserStatus.PENDING, UserStatus.valueOf("PENDING"));
        assertEquals(UserStatus.ACTIVE, UserStatus.valueOf("ACTIVE"));
        assertEquals(UserStatus.SUSPENDED, UserStatus.valueOf("SUSPENDED"));
        assertEquals(UserStatus.LOCKED, UserStatus.valueOf("LOCKED"));
    }
}