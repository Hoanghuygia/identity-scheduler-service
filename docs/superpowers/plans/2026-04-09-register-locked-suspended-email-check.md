# Register Locked/Suspended Email Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Return an `AppException` with admin-contact guidance when register is called with an email already tied to a `SUSPENDED` or `LOCKED` account.

**Architecture:** Keep logic local to `AuthServiceImpl.register` by branching on existing user status after `findByEmail`. Preserve current duplicate-email behavior for non-restricted statuses, and add focused unit tests in `AuthServiceImplTest` to lock behavior.

**Tech Stack:** Java 21, Spring Boot service layer, JUnit 5, Mockito

---

### Task 1: Add failing tests for restricted statuses

**Files:**
- Modify: `src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java`
- Test: `src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java`

- [ ] **Step 1: Write failing test for suspended account**

```java
@Test
void shouldThrowExceptionWhenEmailExistsForSuspendedUser() {
    RegisterRequest request = new RegisterRequest(
        "suspended@example.com",
        "StrongPassword123!",
        "John Doe",
        "+1234567890"
    );

    User existingUser = new User();
    existingUser.setStatus(UserStatus.SUSPENDED);
    when(userService.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

    AppException exception = assertThrows(AppException.class, () -> authService.register(request));

    assertEquals("This account is SUSPENDED. Please contact admin at admin@cty.com", exception.getMessage());
    verify(userService, never()).createUser(any());
}
```

- [ ] **Step 2: Write failing test for locked account**

```java
@Test
void shouldThrowExceptionWhenEmailExistsForLockedUser() {
    RegisterRequest request = new RegisterRequest(
        "locked@example.com",
        "StrongPassword123!",
        "John Doe",
        "+1234567890"
    );

    User existingUser = new User();
    existingUser.setStatus(UserStatus.LOCKED);
    when(userService.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

    AppException exception = assertThrows(AppException.class, () -> authService.register(request));

    assertEquals("This account is LOCKED. Please contact admin at admin@cty.com", exception.getMessage());
    verify(userService, never()).createUser(any());
}
```

- [ ] **Step 3: Run test class to verify RED**

Run: `mvn -B test -Dtest=AuthServiceImplTest`
Expected: FAIL on new tests because current implementation still returns `Email already exists`.

- [ ] **Step 4: Commit failing tests**

```bash
git add src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java
git commit -m "test: cover register behavior for suspended and locked users"
```

### Task 2: Implement minimal register branching for restricted accounts

**Files:**
- Modify: `src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java`
- Test: `src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java`

- [ ] **Step 1: Update register existing-email branch**

```java
Optional<User> existingUserOptional = userService.findByEmail(request.email());
if (existingUserOptional.isPresent()) {
    User existingUser = existingUserOptional.get();
    if (existingUser.getStatus() == UserStatus.SUSPENDED || existingUser.getStatus() == UserStatus.LOCKED) {
        throw new AppException(
            ErrorCode.FORBIDDEN,
            HttpStatus.FORBIDDEN,
            "This account is " + existingUser.getStatus() + ". Please contact admin at admin@cty.com"
        );
    }

    throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT, "Email already exists");
}
```

- [ ] **Step 2: Run test class to verify GREEN**

Run: `mvn -B test -Dtest=AuthServiceImplTest`
Expected: PASS.

- [ ] **Step 3: Run broader auth tests**

Run: `mvn -B test -Dtest="com.example.authservice.auth.**.*Test"`
Expected: PASS.

- [ ] **Step 4: Commit implementation**

```bash
git add src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java src/test/java/com/example/authservice/auth/service/AuthServiceImplTest.java
git commit -m "fix: return admin-contact error for locked and suspended register attempts"
```
