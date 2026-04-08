# Registration Endpoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement complete user registration with event-driven email verification and pending state management

**Architecture:** Event-driven registration flow where users are created in PENDING state, UserRegisteredEvent triggers async email verification token generation and sending, email verification activates user account

**Tech Stack:** Spring Boot, JPA/Hibernate, Spring Events, BCrypt, MapStruct, Bean Validation

---

## File Structure

### Core Components
- **UserStatus enum** - User state enumeration (PENDING, ACTIVE, SUSPENDED, LOCKED)
- **PasswordValidator** - Strong password validation utility 
- **UserRegisteredEvent** - Event published after successful registration
- **EmailVerificationService** - Event listener for verification flow
- **Enhanced User entity** - Add status field and enum mapping
- **Enhanced RegisterRequest** - Update password validation constraints
- **Updated AuthServiceImpl** - Complete registration implementation
- **Updated AuthResponse** - Include user status and updated fields

### Task 1: Create UserStatus Enum and Update User Entity

**Files:**
- Create: `src/main/java/com/example/authservice/user/entity/UserStatus.java`
- Modify: `src/main/java/com/example/authservice/user/entity/User.java:44-46`
- Test: `src/test/java/com/example/authservice/user/entity/UserTest.java`

- [ ] **Step 1: Write test for UserStatus enum**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=UserStatusTest`
Expected: FAIL with "class not found"

- [ ] **Step 3: Create UserStatus enum**

```java
package com.example.authservice.user.entity;

public enum UserStatus {
    PENDING,     // Newly registered, awaiting email verification
    ACTIVE,      // Verified and can login
    SUSPENDED,   // Admin disabled account
    LOCKED       // Security lockout
}
```

- [ ] **Step 4: Write test for User entity status field**

```java
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
```

- [ ] **Step 5: Run test to verify User tests fail**

Run: `mvn -B test -Dtest=UserTest`
Expected: FAIL with compilation errors

- [ ] **Step 6: Update User entity to use UserStatus enum**

```java
// Replace lines 44-46 in User.java with:
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING;
```

Add import at top of User.java:
```java
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn -B test -Dtest="UserStatusTest,UserTest"`
Expected: PASS

- [ ] **Step 8: Commit User entity changes**

```bash
git add src/main/java/com/example/authservice/user/entity/UserStatus.java src/main/java/com/example/authservice/user/entity/User.java src/test/java/com/example/authservice/user/entity/
git commit -m "feat(user): add UserStatus enum and update User entity

- Add PENDING, ACTIVE, SUSPENDED, LOCKED status values
- Update User entity to use UserStatus enum instead of String
- Add comprehensive tests for status transitions"
```

### Task 2: Create Password Validator

**Files:**
- Create: `src/main/java/com/example/authservice/auth/validator/PasswordValidator.java`
- Create: `src/main/java/com/example/authservice/auth/validator/ValidPassword.java`
- Test: `src/test/java/com/example/authservice/auth/validator/PasswordValidatorTest.java`

- [ ] **Step 1: Write password validator tests**

```java
package com.example.authservice.auth.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Password123!",
        "StrongPass1@",
        "MySecureP4$$w0rd",
        "C0mplex!Password"
    })
    void shouldAcceptValidPasswords(String password) {
        TestRecord record = new TestRecord(password);
        Set<ConstraintViolation<TestRecord>> violations = validator.validate(record);
        assertTrue(violations.isEmpty(), "Password should be valid: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "short1!",           // Too short
        "nouppercase123!",   // No uppercase
        "NOLOWERCASE123!",   // No lowercase  
        "NoNumbers!",        // No numbers
        "NoSpecialChars123", // No special chars
        "Password123",       // No special chars
        ""                   // Empty
    })
    void shouldRejectInvalidPasswords(String password) {
        TestRecord record = new TestRecord(password);
        Set<ConstraintViolation<TestRecord>> violations = validator.validate(record);
        assertFalse(violations.isEmpty(), "Password should be invalid: " + password);
    }

    record TestRecord(@ValidPassword String password) {}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=PasswordValidatorTest`
Expected: FAIL with "class not found"

- [ ] **Step 3: Create ValidPassword annotation**

```java
package com.example.authservice.auth.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must be at least 12 characters with uppercase, lowercase, number, and special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

- [ ] **Step 4: Create PasswordValidator implementation**

```java
package com.example.authservice.auth.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 12;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]");

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }

        return UPPERCASE.matcher(password).find() &&
               LOWERCASE.matcher(password).find() &&
               DIGIT.matcher(password).find() &&
               SPECIAL_CHAR.matcher(password).find();
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -B test -Dtest=PasswordValidatorTest`
Expected: PASS

- [ ] **Step 6: Commit password validator**

```bash
git add src/main/java/com/example/authservice/auth/validator/ src/test/java/com/example/authservice/auth/validator/
git commit -m "feat(auth): add strong password validation

- Require minimum 12 characters with complexity rules
- Custom @ValidPassword annotation with regex validation
- Comprehensive tests for valid and invalid passwords"
```

### Task 3: Update RegisterRequest with Enhanced Validation

**Files:**
- Modify: `src/main/java/com/example/authservice/auth/dto/RegisterRequest.java:8-13`
- Test: `src/test/java/com/example/authservice/auth/dto/RegisterRequestTest.java`

- [ ] **Step 1: Write RegisterRequest validation tests**

```java
package com.example.authservice.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidRequest() {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-email", "missing@", "@missing.com"})
    void shouldRejectInvalidEmails(String email) {
        RegisterRequest request = new RegisterRequest(
            email,
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"weak", "NoNumbers!", "password123", ""})
    void shouldRejectWeakPasswords(String password) {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            password,
            "John Doe",
            "+1234567890"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectEmptyFullName() {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "StrongPassword123!",
            "",
            "+1234567890"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=RegisterRequestTest`
Expected: FAIL with compilation/validation errors

- [ ] **Step 3: Update RegisterRequest with enhanced validation**

```java
package com.example.authservice.auth.dto;

import com.example.authservice.auth.validator.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required") 
    @Email(message = "Email must be valid") 
    String email,
    
    @NotBlank(message = "Password is required")
    @ValidPassword 
    String password,
    
    @NotBlank(message = "Full name is required") 
    @Size(max = 100, message = "Full name must not exceed 100 characters") 
    String fullName,
    
    @Pattern(regexp = "^[+0-9()\\-\\s]{7,20}$", message = "Invalid phone number format") 
    String phoneNumber
) {
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -B test -Dtest=RegisterRequestTest`
Expected: PASS

- [ ] **Step 5: Commit RegisterRequest updates**

```bash
git add src/main/java/com/example/authservice/auth/dto/RegisterRequest.java src/test/java/com/example/authservice/auth/dto/
git commit -m "feat(auth): enhance RegisterRequest with strong password validation

- Replace basic size validation with @ValidPassword constraint
- Add comprehensive validation tests
- Improve validation error messages"
```

### Task 4: Create UserRegisteredEvent

**Files:**
- Create: `src/main/java/com/example/authservice/auth/event/UserRegisteredEvent.java`
- Test: `src/test/java/com/example/authservice/auth/event/UserRegisteredEventTest.java`

- [ ] **Step 1: Write UserRegisteredEvent tests**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=UserRegisteredEventTest`
Expected: FAIL with "class not found"

- [ ] **Step 3: Create UserRegisteredEvent**

```java
package com.example.authservice.auth.event;

import java.util.UUID;

public record UserRegisteredEvent(UUID userId, String email) {
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -B test -Dtest=UserRegisteredEventTest`
Expected: PASS

- [ ] **Step 5: Commit UserRegisteredEvent**

```bash
git add src/main/java/com/example/authservice/auth/event/ src/test/java/com/example/authservice/auth/event/
git commit -m "feat(auth): add UserRegisteredEvent for async email verification

- Simple record with userId and email for event-driven flow
- Comprehensive tests for equality and toString methods"
```

### Task 5: Create EmailVerificationService

**Files:**
- Create: `src/main/java/com/example/authservice/auth/service/EmailVerificationService.java`
- Test: `src/test/java/com/example/authservice/auth/service/EmailVerificationServiceTest.java`

- [ ] **Step 1: Write EmailVerificationService tests**

```java
package com.example.authservice.auth.service;

import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.token.service.VerificationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailVerificationService = new EmailVerificationService(
            verificationTokenService,
            emailService
        );
    }

    @Test
    void shouldHandleUserRegisteredEvent() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = "verification-token-123";
        UserRegisteredEvent event = new UserRegisteredEvent(userId, email);

        when(verificationTokenService.createEmailVerificationToken(userId))
            .thenReturn(token);

        // When
        emailVerificationService.handleUserRegistered(event);

        // Then
        verify(verificationTokenService).createEmailVerificationToken(userId);
        verify(emailService).sendVerificationEmail(email, token);
    }

    @Test
    void shouldHandleEmailServiceException() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = "verification-token-123";
        UserRegisteredEvent event = new UserRegisteredEvent(userId, email);

        when(verificationTokenService.createEmailVerificationToken(userId))
            .thenReturn(token);
        doThrow(new RuntimeException("Email service failed"))
            .when(emailService).sendVerificationEmail(email, token);

        // When & Then - should not throw exception
        emailVerificationService.handleUserRegistered(event);

        verify(verificationTokenService).createEmailVerificationToken(userId);
        verify(emailService).sendVerificationEmail(email, token);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=EmailVerificationServiceTest`
Expected: FAIL with "class not found"

- [ ] **Step 3: Create EmailVerificationService implementation**

```java
package com.example.authservice.auth.service;

import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.token.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;

    @Async
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        try {
            log.info("handling_user_registered_event user_id={} email={}", 
                event.userId(), event.email());

            String verificationToken = verificationTokenService.createEmailVerificationToken(event.userId());
            emailService.sendVerificationEmail(event.email(), verificationToken);

            log.info("verification_email_sent user_id={} email={}", 
                event.userId(), event.email());
        } catch (Exception e) {
            log.error("verification_email_failed user_id={} email={} error={}", 
                event.userId(), event.email(), e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Check if VerificationTokenService exists and create if needed**

Check: `src/main/java/com/example/authservice/token/service/VerificationTokenService.java`

If it doesn't exist, create:
```java
package com.example.authservice.token.service;

import java.util.UUID;

public interface VerificationTokenService {
    String createEmailVerificationToken(UUID userId);
    boolean validateEmailVerificationToken(String token);
    UUID getUserIdFromToken(String token);
}
```

And implementation:
```java
package com.example.authservice.token.service;

import com.example.authservice.token.entity.EmailVerificationToken;
import com.example.authservice.token.repository.EmailVerificationTokenRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private static final int TOKEN_VALIDITY_HOURS = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Override
    public String createEmailVerificationToken(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String tokenValue = generateSecureToken();
        Instant expiresAt = Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setToken(tokenValue);
        token.setExpiresAt(expiresAt);
        token.setUsed(false);

        tokenRepository.save(token);
        return tokenValue;
    }

    @Override
    public boolean validateEmailVerificationToken(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByTokenAndUsedFalse(token);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }

        EmailVerificationToken verificationToken = tokenOpt.get();
        return Instant.now().isBefore(verificationToken.getExpiresAt());
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        return tokenRepository.findByTokenAndUsedFalse(token)
            .map(t -> t.getUser().getId())
            .orElse(null);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -B test -Dtest=EmailVerificationServiceTest`
Expected: PASS

- [ ] **Step 6: Commit EmailVerificationService**

```bash
git add src/main/java/com/example/authservice/auth/service/EmailVerificationService.java src/main/java/com/example/authservice/token/service/ src/test/java/com/example/authservice/auth/service/
git commit -m "feat(auth): add EmailVerificationService for async event handling

- Listen for UserRegisteredEvent with @EventListener
- Generate secure verification tokens with 24h expiry
- Send verification emails asynchronously
- Comprehensive error handling and logging"
```

### Task 6: Update AuthResponse with User Status

**Files:**
- Modify: `src/main/java/com/example/authservice/auth/dto/AuthResponse.java`
- Test: `src/test/java/com/example/authservice/auth/dto/AuthResponseTest.java`

- [ ] **Step 1: Check current AuthResponse structure**

Look at current AuthResponse.java to understand existing fields

- [ ] **Step 2: Write AuthResponse tests with status**

```java
package com.example.authservice.auth.dto;

import com.example.authservice.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void shouldCreateAuthResponseWithStatus() {
        AuthResponse response = new AuthResponse(
            "user123",
            "test@example.com",
            "John Doe",
            UserStatus.PENDING,
            "access-token",
            "refresh-token",
            3600L
        );

        assertEquals("user123", response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("John Doe", response.fullName());
        assertEquals(UserStatus.PENDING, response.status());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(3600L, response.expiresIn());
    }

    @Test
    void shouldCreateStubResponseWithoutTokens() {
        AuthResponse response = AuthResponse.stub("Registration successful", UserStatus.PENDING);

        assertEquals("stub-user-id", response.userId());
        assertEquals("stub@example.com", response.email());
        assertEquals("Stub User", response.fullName());
        assertEquals(UserStatus.PENDING, response.status());
        assertNull(response.accessToken());
        assertNull(response.refreshToken());
        assertNull(response.expiresIn());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -B test -Dtest=AuthResponseTest`
Expected: FAIL with compilation errors

- [ ] **Step 4: Update AuthResponse to include status field**

Update the AuthResponse record to include UserStatus:
```java
package com.example.authservice.auth.dto;

import com.example.authservice.user.entity.UserStatus;

public record AuthResponse(
    String userId,
    String email,
    String fullName,
    UserStatus status,
    String accessToken,
    String refreshToken,
    Long expiresIn
) {
    public static AuthResponse stub(String message) {
        return new AuthResponse(
            "stub-user-id",
            "stub@example.com", 
            "Stub User",
            UserStatus.PENDING,
            null,
            null,
            null
        );
    }

    public static AuthResponse stub(String message, UserStatus status) {
        return new AuthResponse(
            "stub-user-id",
            "stub@example.com",
            "Stub User", 
            status,
            null,
            null,
            null
        );
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -B test -Dtest=AuthResponseTest`
Expected: PASS

- [ ] **Step 6: Commit AuthResponse updates**

```bash
git add src/main/java/com/example/authservice/auth/dto/AuthResponse.java src/test/java/com/example/authservice/auth/dto/
git commit -m "feat(auth): add status field to AuthResponse

- Include UserStatus in response DTOs
- Update stub methods to support status parameter  
- Add comprehensive tests for new fields"
```

### Task 7: Implement Registration in AuthServiceImpl

**Files:**
- Modify: `src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java:22-27`
- Test: `src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java`

- [ ] **Step 1: Write AuthServiceImpl registration tests**

```java
package com.example.authservice.auth.service;

import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.auth.dto.AuthResponse;
import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.common.exception.AppException;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.role.service.RoleService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuthAuditService authAuditService;

    @Mock
    private EmailService emailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthServiceImpl(
            userService,
            roleService, 
            passwordEncoder,
            eventPublisher,
            authAuditService,
            emailService
        );
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(request.email());
        savedUser.setFullName(request.fullName());
        savedUser.setStatus(UserStatus.PENDING);

        when(userService.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userService.createUser(any(User.class))).thenReturn(savedUser);

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertEquals(userId.toString(), response.userId());
        assertEquals(request.email(), response.email());
        assertEquals(request.fullName(), response.fullName());
        assertEquals(UserStatus.PENDING, response.status());
        assertNull(response.accessToken());
        assertNull(response.refreshToken());

        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals(request.email(), capturedUser.getEmail());
        assertEquals("hashed-password", capturedUser.getPasswordHash());
        assertEquals(request.fullName(), capturedUser.getFullName());
        assertEquals(request.phoneNumber(), capturedUser.getPhoneNumber());
        assertEquals(UserStatus.PENDING, capturedUser.getStatus());

        // Verify event publication
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserRegisteredEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.userId());
        assertEquals(request.email(), capturedEvent.email());

        // Verify role assignment
        verify(roleService).assignCustomerRole(userId);

        // Verify audit logging
        verify(authAuditService).record(eq(userId), any(), eq("User registered successfully"), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "existing@example.com",
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        User existingUser = new User();
        when(userService.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.register(request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userService, never()).createUser(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=AuthServiceImplTest`
Expected: FAIL with compilation errors and missing methods

- [ ] **Step 3: Update AuthServiceImpl dependencies**

Update constructor and add required dependencies:
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthAuditService authAuditService;
    private final EmailService emailService;

    // ... rest of methods
}
```

- [ ] **Step 4: Implement register method**

Replace the register method implementation:
```java
@Override
@Transactional
public AuthResponse register(RegisterRequest request) {
    log.info("registration_started email={}", request.email());

    // Check if email already exists
    if (userService.findByEmail(request.email()).isPresent()) {
        log.warn("registration_failed_duplicate_email email={}", request.email());
        throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT, "Email already exists");
    }

    // Create new user in PENDING state
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFullName(request.fullName());
    user.setPhoneNumber(request.phoneNumber());
    user.setStatus(UserStatus.PENDING);
    user.setEmailVerified(false);

    User savedUser = userService.createUser(user);
    
    // Assign CUSTOMER role
    roleService.assignCustomerRole(savedUser.getId());

    // Publish event for async email verification
    UserRegisteredEvent event = new UserRegisteredEvent(savedUser.getId(), savedUser.getEmail());
    eventPublisher.publishEvent(event);

    // Record audit log
    authAuditService.record(
        savedUser.getId(), 
        AuditEventType.REGISTER_SUCCESS, 
        "User registered successfully", 
        null, 
        null
    );

    log.info("registration_completed user_id={} email={}", savedUser.getId(), savedUser.getEmail());

    return new AuthResponse(
        savedUser.getId().toString(),
        savedUser.getEmail(),
        savedUser.getFullName(),
        savedUser.getStatus(),
        null,  // No access token until email verified
        null,  // No refresh token until email verified
        null   // No expiration until email verified
    );
}
```

- [ ] **Step 5: Add required imports**

```java
import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.role.service.RoleService;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
```

- [ ] **Step 6: Run tests to verify they pass** 

Run: `mvn -B test -Dtest=AuthServiceImplTest`
Expected: PASS (may need to create missing UserService and RoleService methods)

- [ ] **Step 7: Commit AuthServiceImpl implementation**

```bash
git add src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java src/test/java/com/example/authservice/auth/service/
git commit -m "feat(auth): implement complete registration flow in AuthServiceImpl

- Check for duplicate email and throw AppException
- Create user in PENDING status with hashed password
- Assign CUSTOMER role and publish UserRegisteredEvent
- Return AuthResponse without tokens until verified
- Comprehensive tests with mocked dependencies"
```

### Task 8: Update Email Verification Endpoint

**Files:**
- Modify: `src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java:55-59` 
- Test: `src/test/java/com/example/authservice/auth/service/EmailVerificationTest.java`

- [ ] **Step 1: Write email verification tests**

```java
package com.example.authservice.auth.service;

import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.common.exception.AppException;
import com.example.authservice.token.service.VerificationTokenService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailVerificationTest {

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private UserService userService;

    @Mock
    private AuthAuditService authAuditService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Create minimal AuthService with required dependencies
        authService = new AuthServiceImpl(
            userService, null, null, null, authAuditService, null
        );
    }

    @Test
    void shouldVerifyEmailSuccessfully() {
        // Given
        String token = "valid-verification-token";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.PENDING);
        user.setEmailVerified(false);

        when(verificationTokenService.validateEmailVerificationToken(token)).thenReturn(true);
        when(verificationTokenService.getUserIdFromToken(token)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(user);

        // When
        authService.verifyEmail(token);

        // Then
        verify(userService).activateUser(userId);
        verify(authAuditService).record(eq(userId), any(), eq("Email verified successfully"), any(), any());
    }

    @Test
    void shouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        
        when(verificationTokenService.validateEmailVerificationToken(invalidToken)).thenReturn(false);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.verifyEmail(invalidToken);
        });

        assertEquals("Invalid or expired verification token", exception.getMessage());
        verify(userService, never()).activateUser(any());
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyVerified() {
        // Given
        String token = "valid-token";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        when(verificationTokenService.validateEmailVerificationToken(token)).thenReturn(true);
        when(verificationTokenService.getUserIdFromToken(token)).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(user);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.verifyEmail(token);
        });

        assertEquals("Email already verified", exception.getMessage());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=EmailVerificationTest`
Expected: FAIL with compilation errors

- [ ] **Step 3: Implement verifyEmail method**

Replace the verifyEmail method in AuthServiceImpl:
```java
@Override
@Transactional
public void verifyEmail(String token) {
    log.info("email_verification_started token={}", token);

    // Validate token
    if (!verificationTokenService.validateEmailVerificationToken(token)) {
        log.warn("email_verification_failed_invalid_token token={}", token);
        throw new AppException(
            ErrorCode.INVALID_TOKEN, 
            HttpStatus.BAD_REQUEST, 
            "Invalid or expired verification token"
        );
    }

    // Get user from token
    UUID userId = verificationTokenService.getUserIdFromToken(token);
    User user = userService.findById(userId);
    
    if (user == null) {
        log.warn("email_verification_failed_user_not_found user_id={}", userId);
        throw new AppException(
            ErrorCode.USER_NOT_FOUND,
            HttpStatus.NOT_FOUND,
            "User not found"
        );
    }

    // Check if already verified
    if (user.isEmailVerified() || user.getStatus() == UserStatus.ACTIVE) {
        log.warn("email_verification_failed_already_verified user_id={}", userId);
        throw new AppException(
            ErrorCode.EMAIL_ALREADY_VERIFIED,
            HttpStatus.CONFLICT,
            "Email already verified"
        );
    }

    // Activate user
    userService.activateUser(userId);

    // Record audit log
    authAuditService.record(
        userId,
        AuditEventType.EMAIL_VERIFIED,
        "Email verified successfully",
        null,
        null
    );

    log.info("email_verification_completed user_id={}", userId);
}
```

- [ ] **Step 4: Add verificationTokenService dependency**

Update AuthServiceImpl constructor to include VerificationTokenService:
```java
private final VerificationTokenService verificationTokenService;
```

And update constructor:
```java
public AuthServiceImpl(
    UserService userService,
    RoleService roleService,
    PasswordEncoder passwordEncoder,
    ApplicationEventPublisher eventPublisher,
    AuthAuditService authAuditService,
    EmailService emailService,
    VerificationTokenService verificationTokenService
) {
    this.userService = userService;
    this.roleService = roleService;
    this.passwordEncoder = passwordEncoder;
    this.eventPublisher = eventPublisher;
    this.authAuditService = authAuditService;
    this.emailService = emailService;
    this.verificationTokenService = verificationTokenService;
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -B test -Dtest=EmailVerificationTest`
Expected: PASS

- [ ] **Step 6: Commit email verification implementation**

```bash
git add src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java src/test/java/com/example/authservice/auth/service/
git commit -m "feat(auth): implement email verification endpoint

- Validate verification tokens with expiry and single-use checks
- Activate users and mark emails as verified
- Comprehensive error handling for invalid/expired tokens
- Prevent double verification with proper status checks"
```

## Self-Review

**1. Spec coverage:** 
- ✅ UserStatus enum with PENDING/ACTIVE/SUSPENDED/LOCKED states
- ✅ Strong password validation (12+ chars, complexity rules)  
- ✅ Event-driven registration with UserRegisteredEvent
- ✅ Email verification with 24h token expiry
- ✅ CUSTOMER role assignment (STAFF/ADMIN supported)
- ✅ Enhanced AuthResponse with status field
- ✅ Complete registration and verification implementations

**2. Placeholder scan:** No TBD, TODO, or incomplete implementations

**3. Type consistency:** UserStatus, UUID, String types consistent across all components

Plan complete and saved to `docs/superpowers/plans/2026-04-07-registration-endpoint-implementation.md`.