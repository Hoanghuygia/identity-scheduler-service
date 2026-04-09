## Title
Handle `LOCKED`/`SUSPENDED` users in register email-exists validation

## Context
The register flow in `AuthServiceImpl.register` currently checks whether an email already exists. If it exists, it always throws:
- `AppException(ErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT, "Email already exists")`

Requested behavior:
- Only when the existing account status is `LOCKED` or `SUSPENDED`, return an `AppException` telling the user to contact admin at `admin@cty.com`.
- Keep current behavior for `PENDING` and `ACTIVE` users.

## Scope
In scope:
- Update branch logic in `src/main/java/com/example/authservice/auth/service/AuthServiceImpl.java` within `register(RegisterRequest request)`.

Out of scope:
- New error codes.
- Refactoring to `UserService`.
- Changes to login, verify-email, or audit/event flows.

## Chosen Approach
Approach 1 (inline branching in `register`) was selected.

Design:
1. Read existing user by email once.
2. If present:
   - If status is `UserStatus.SUSPENDED` or `UserStatus.LOCKED`, throw `AppException` with an explicit admin-contact message including `admin@cty.com`.
   - Otherwise (`PENDING`, `ACTIVE`, or other non-restricted states), keep the existing duplicate-email exception behavior.
3. If not present, keep the current registration flow unchanged.

## Detailed Behavior
### Existing email + restricted status
- Condition: `existingUser.getStatus() == UserStatus.SUSPENDED || existingUser.getStatus() == UserStatus.LOCKED`
- Action: throw `AppException`.
- Suggested HTTP status: `HttpStatus.FORBIDDEN`.
- Message: instruct user to contact admin via `admin@cty.com`.

### Existing email + non-restricted status
- Condition: existing user but not `SUSPENDED`/`LOCKED`.
- Action: keep current exception:
  - `ErrorCode.EMAIL_ALREADY_EXISTS`
  - `HttpStatus.CONFLICT`
  - `"Email already exists"`

### Email not found
- No change to current success path (create `PENDING` user, assign role, publish event, audit, return response).

## Logging
- Keep existing warning log for duplicate email behavior.
- Add a dedicated warning log for restricted account registration attempts (include email and status as structured key=value fields).

## Error Handling
- Continue using `AppException` and existing global exception handling.
- No controller-level changes.

## Testing Impact
Recommended updates/additions in auth service tests:
- Existing email with `SUSPENDED` -> throws `AppException` with message containing `admin@cty.com`.
- Existing email with `LOCKED` -> throws `AppException` with message containing `admin@cty.com`.
- Existing email with `PENDING` -> unchanged `EMAIL_ALREADY_EXISTS` conflict behavior.
- Existing email with `ACTIVE` -> unchanged `EMAIL_ALREADY_EXISTS` conflict behavior.
- Email not found -> unchanged success registration behavior.

## Risks and Mitigations
- Risk: inconsistent status code choice for restricted accounts.
  - Mitigation: use explicit `HttpStatus.FORBIDDEN` and keep message deterministic.
- Risk: accidental behavior change for non-restricted existing users.
  - Mitigation: keep duplicate-email branch intact and add focused tests.

## Acceptance Criteria
1. `register` throws admin-contact `AppException` when existing email belongs to `SUSPENDED` or `LOCKED` user.
2. `register` continues returning duplicate email conflict for `PENDING` and `ACTIVE` users.
3. No behavior changes in new-user registration flow.
