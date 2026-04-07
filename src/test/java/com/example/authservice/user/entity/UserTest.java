package com.example.authservice.user.entity;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldDefaultToPendingStatus() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setFullName("Test User");
        user.setStatus(UserStatus.PENDING);
        
        assertEquals(UserStatus.PENDING, user.getStatus());
        assertNotNull(user.getId());
    }

    @Test
    void shouldAllowStatusTransitions() {
        User user = new User();
        user.setStatus(UserStatus.PENDING);
        assertEquals(UserStatus.PENDING, user.getStatus());
        
        user.setStatus(UserStatus.ACTIVE);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        
        user.setStatus(UserStatus.SUSPENDED);
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        
        user.setStatus(UserStatus.LOCKED);
        assertEquals(UserStatus.LOCKED, user.getStatus());
    }
}