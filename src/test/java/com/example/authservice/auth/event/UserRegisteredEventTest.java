package com.example.authservice.auth.event;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserRegisteredEventTest {

    @Test
    void shouldCreateEventWithUserIdAndEmail() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        UserRegisteredEvent event = new UserRegisteredEvent(userId, email);

        assertEquals(userId, event.userId());
        assertEquals(email, event.email());
    }

    @Test
    void shouldBeEqualWhenSameValues() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        UserRegisteredEvent event1 = new UserRegisteredEvent(userId, email);
        UserRegisteredEvent event2 = new UserRegisteredEvent(userId, email);

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void shouldHaveToString() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        UserRegisteredEvent event = new UserRegisteredEvent(userId, email);
        String toString = event.toString();

        assertTrue(toString.contains(userId.toString()));
        assertTrue(toString.contains(email));
    }
}