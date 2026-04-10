# Me Endpoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `GET /api/v1/auth/me` to return the authenticated user's safe profile fields using `userId` from token-backed security context only.

**Architecture:** Keep endpoint ownership in `auth` feature package. Introduce a dedicated response DTO (`CurrentUserResponse`) to prevent leaking auth/session fields. Implement lookup flow in `AuthServiceImpl.me()` using `SecurityContextUtil.currentUserId()` and map only allowlisted fields.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Spring Security, JUnit 5, Mockito, MockMvc, Maven.

---

## File Structure

- Create: `src/main/java/com/example/authservice/auth/dto/CurrentUserResponse.java`
  - Dedicated safe response record for `/me`.
- Modify: `src/main/java/com/example/authservice/auth/service/AuthService.java`
  - Change `me()` return type to `CurrentUserResponse`.
- Modify: `src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java`
  - Implement trusted identity flow and `USER_NOT_FOUND` handling.
- Modify: `src/main/java/com/example/authservice/auth/controller/AuthController.java`
  - Return `ApiResponse<CurrentUserResponse>` with non-stub message.
- Modify: `src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java`
  - Add service tests for `/me` success and user-not-found.
- Modify: `src/test/java/com/example/authservice/integration/AuthSessionFlowIntegrationTest.java`
  - Add integration tests for `/me` success and unauthorized access.

### Task 1: Write Failing Tests for Service-Level `/me` Behavior

**Files:**
- Modify: `src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java`

- [ ] **Step 1: Add failing unit test imports and test cases**

```java
import com.example.authservice.auth.dto.CurrentUserResponse;
import com.example.authservice.common.exception.ErrorCode;

// ...

@Test
void shouldReturnCurrentUserProfileFromSecurityContext() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setEmail("me@example.com");
    user.setFullName("Me User");
    user.setStatus(UserStatus.ACTIVE);
    user.setEmailVerified(true);

    when(userService.getById(userId)).thenReturn(user);

    try (MockedStatic<SecurityContextUtil> mockedSecurityContextUtil = mockStatic(SecurityContextUtil.class)) {
        mockedSecurityContextUtil.when(SecurityContextUtil::currentUserId).thenReturn(userId);

        CurrentUserResponse response = authService.me();

        assertEquals(userId.toString(), response.userId());
        assertEquals("me@example.com", response.email());
        assertEquals("Me User", response.fullName());
        assertEquals(UserStatus.ACTIVE, response.status());
        assertTrue(response.emailVerified());
        verify(userService).getById(userId);
    }
}

@Test
void shouldThrowNotFoundWhenCurrentUserMissing() {
    UUID userId = UUID.randomUUID();
    when(userService.getById(userId)).thenReturn(null);

    try (MockedStatic<SecurityContextUtil> mockedSecurityContextUtil = mockStatic(SecurityContextUtil.class)) {
        mockedSecurityContextUtil.when(SecurityContextUtil::currentUserId).thenReturn(userId);

        AppException exception = assertThrows(AppException.class, () -> authService.me());

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("User not found", exception.getMessage());
        verify(userService).getById(userId);
    }
}
```

- [ ] **Step 2: Run unit test class to verify new tests fail first**

Run: `mvn -B test -Dtest=AuthServiceImplTest`

Expected: FAIL due to missing `CurrentUserResponse` type and/or `me()` signature/implementation mismatch.

- [ ] **Step 3: Commit failing tests**

```bash
git add src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java
git commit -m "test: add me service behavior coverage"
```

### Task 2: Implement `/me` Endpoint with Safe DTO and Trusted Identity

**Files:**
- Create: `src/main/java/com/example/authservice/auth/dto/CurrentUserResponse.java`
- Modify: `src/main/java/com/example/authservice/auth/service/AuthService.java`
- Modify: `src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java`
- Modify: `src/main/java/com/example/authservice/auth/controller/AuthController.java`

- [ ] **Step 1: Create dedicated response DTO**

```java
package com.example.authservice.auth.dto;

import com.example.authservice.user.entity.UserStatus;

public record CurrentUserResponse(
    String userId,
    String email,
    String fullName,
    UserStatus status,
    boolean emailVerified
) {
}
```

- [ ] **Step 2: Change service contract return type**

```java
// AuthService.java
import com.example.authservice.auth.dto.CurrentUserResponse;

CurrentUserResponse me();
```

- [ ] **Step 3: Implement `AuthServiceImpl.me()` with security-context user id**

```java
@Override
public CurrentUserResponse me() {
    UUID userId = SecurityContextUtil.currentUserId();
    User user = userService.getById(userId);

    if (user == null) {
        throw new AppException(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found");
    }

    return new CurrentUserResponse(
        user.getId().toString(),
        user.getEmail(),
        user.getFullName(),
        user.getStatus(),
        user.isEmailVerified()
    );
}
```

- [ ] **Step 4: Update controller method to return new DTO and final message**

```java
@SecurityRequirement(name = "bearerAuth")
@GetMapping("/me")
public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {
    return ResponseEntity.ok(
        ApiResponse.success("Current user profile fetched successfully", authService.me())
    );
}
```

- [ ] **Step 5: Run focused unit test class and compile check**

Run: `mvn -B test -Dtest=AuthServiceImplTest`

Expected: PASS for `AuthServiceImplTest` including new `/me` tests.

Run: `mvn -B compile`

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit implementation**

```bash
git add src/main/java/com/example/authservice/auth/dto/CurrentUserResponse.java src/main/java/com/example/authservice/auth/service/AuthService.java src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java src/main/java/com/example/authservice/auth/controller/AuthController.java
git commit -m "feat: implement me endpoint with trusted token identity"
```

### Task 3: Add Integration Coverage for `/me` API Contract

**Files:**
- Modify: `src/test/java/com/example/authservice/integration/AuthSessionFlowIntegrationTest.java`

- [ ] **Step 1: Add `GET /me` imports and tests**

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// ...

@Test
void shouldReturnCurrentUserProfileForAuthenticatedRequest() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = saveUser(userId, "me-integration@example.com");

    mockMvc.perform(get("/api/v1/auth/me").with(authenticated(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Current user profile fetched successfully"))
        .andExpect(jsonPath("$.data.userId").value(user.getId().toString()))
        .andExpect(jsonPath("$.data.email").value("me-integration@example.com"))
        .andExpect(jsonPath("$.data.fullName").value("Session User"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.emailVerified").value(true))
        .andExpect(jsonPath("$.data.accessToken").doesNotExist())
        .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
        .andExpect(jsonPath("$.data.sessionId").doesNotExist())
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
}

@Test
void shouldReturnUnauthorizedWhenMeCalledWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 2: Run integration test class**

Run: `mvn -B test -Dtest=AuthSessionFlowIntegrationTest`

Expected: PASS with new `/me` success and unauthorized assertions.

- [ ] **Step 3: Run full verification command used by CI**

Run: `mvn -B clean verify`

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit integration tests**

```bash
git add src/test/java/com/example/authservice/integration/AuthSessionFlowIntegrationTest.java
git commit -m "test: cover me endpoint integration contract"
```

## Final Validation Checklist

- [ ] `CurrentUserResponse` contains only safe fields.
- [ ] `AuthServiceImpl.me()` obtains `userId` exclusively from `SecurityContextUtil.currentUserId()`.
- [ ] `/api/v1/auth/me` no longer returns stub data.
- [ ] Sensitive fields are absent from `/me` JSON payload.
- [ ] Unit and integration tests pass.
- [ ] `mvn -B clean verify` passes.
