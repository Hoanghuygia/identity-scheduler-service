# Registration Endpoint Implementation Design

**Date**: April 7, 2026  
**Author**: System Design  
**Status**: Pending Review

## Overview

Implement a complete user registration flow using event-driven architecture with pending user state management. Users register, receive verification email, and must verify before login is allowed.

## Requirements

- Users register with email, password, full name, and phone number
- Email must be unique (return error if duplicate)
- Users start in PENDING state, become ACTIVE after email verification
- Default role assignment: CUSTOMER (with future support for STAFF, ADMIN)
- Email verification tokens expire after 24 hours
- Strong password validation (min 12 chars + complexity requirements)
- Event-driven email verification (async, no blocking on email failures)

## Architecture

### Component Overview

```
AuthController.register()
    ↓
AuthServiceImpl.register()
    ↓ (creates user in PENDING state)
Database
    ↓ (publishes event)
UserRegisteredEvent
    ↓ (async processing)
EmailVerificationService.handleUserRegistered()
    ↓ (generates token + sends email)
EmailService + VerificationTokenService
```

### User State Machine

```
PENDING → (email verification) → ACTIVE
ACTIVE → (admin action) → SUSPENDED
ACTIVE → (security action) → LOCKED
```

## Data Model Changes

### User Entity Enhancement

**New UserStatus Enum:**
- `PENDING`: Newly registered, awaiting email verification
- `ACTIVE`: Verified and can login
- `SUSPENDED`: Admin disabled account
- `LOCKED`: Security lockout

**User Entity Modifications:**
- Add `status` field with `@Enumerated(EnumType.STRING)`
- Update constraints and validation

### Role System

**Default roles:**
- `CUSTOMER`: Default for new registrations
- `STAFF`: Future admin assignment
- `ADMIN`: Future admin assignment

## Event System

### UserRegisteredEvent
```java
public record UserRegisteredEvent(UUID userId, String email) {}
```

**Event Flow:**
1. Registration succeeds → publish UserRegisteredEvent
2. EmailVerificationService listens for event
3. Generate verification token (24h expiry)
4. Send verification email
5. Log any failures (don't propagate to registration)

## Security Enhancements

### Password Validation
- Minimum 12 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z) 
- At least one digit (0-9)
- At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)

### Email Verification
- Cryptographically secure random tokens
- 24-hour expiration
- Single-use tokens (invalidated after verification)

## API Behavior

### Registration Endpoint (`POST /api/v1/auth/register`)

**Success Response (201):**
```json
{
  "success": true,
  "message": "Registration successful. Please check your email to verify your account.",
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "User Name",
    "status": "PENDING"
  }
}
```

**Error Cases:**
- 400: Validation errors (weak password, invalid email, etc.)
- 409: Email already exists
- 500: Server error during registration

### Email Verification Endpoint (`GET /api/v1/auth/verify-email?token=xyz`)

**Success Response (200):**
```json
{
  "success": true,
  "message": "Email verified successfully. You can now log in.",
  "data": null
}
```

**Error Cases:**
- 400: Invalid or expired token
- 409: Email already verified

## Database Schema Updates

### Users Table
```sql
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
CREATE INDEX idx_users_status ON users(status);
```

### Verification Tokens Table
```sql
-- Extend existing email verification token functionality
-- Ensure 24-hour expiration and single-use validation
```

## Implementation Components

### Core Components to Implement/Modify:

1. **UserStatus enum** - New user state enumeration
2. **User entity** - Add status field and validation
3. **UserRegisteredEvent** - Event class for registration
4. **EmailVerificationService** - Event listener and verification logic
5. **AuthServiceImpl.register()** - Core registration implementation
6. **RegisterRequest** - Enhanced password validation
7. **UserService** - User creation and status management
8. **PasswordValidator** - Strong password validation utility

### Testing Strategy

1. **Unit Tests:**
   - Password validation rules
   - User creation with PENDING status
   - Event publishing verification
   - Email verification token validation

2. **Integration Tests:**
   - Complete registration flow
   - Event handling and async processing
   - Database state transitions
   - Error handling scenarios

## Error Handling

**Registration Failures:**
- Validation errors → 400 with detailed field errors
- Duplicate email → 409 with clear message
- System errors → 500 with generic message

**Email Processing Failures:**
- Log errors but don't fail registration
- Implement retry mechanism for email delivery
- Provide resend verification email endpoint (future)

## Security Considerations

- Hash passwords using BCrypt with appropriate rounds
- Generate cryptographically secure verification tokens
- Validate token expiration and single-use constraints
- Rate limiting on registration endpoint (future enhancement)
- Input sanitization and validation on all fields

## Future Enhancements

- Resend verification email endpoint
- Account lockout after multiple failed attempts
- Password strength meter on frontend
- Admin user management interface
- Bulk role assignment capabilities