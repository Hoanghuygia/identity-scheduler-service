# Me Endpoint Design

## Context

The authentication API already exposes `GET /api/v1/auth/me`, but the current implementation is a stub and does not enforce a trusted identity source in service logic. This endpoint must always derive identity from the validated token context, never from client-provided fields, and must not return sensitive information.

## Goals

- Implement `/me` as a real authenticated profile endpoint.
- Read `userId` only from security context populated by JWT authentication.
- Return only safe profile fields; never include tokens, password hash, or internal security metadata.
- Keep error handling aligned with existing global exception behavior.

## Non-Goals

- Changing JWT structure or authentication filter behavior.
- Adding new persistence fields or schema changes.
- Expanding `/me` into session-management or account-settings functionality.

## API Contract

- **Method/Path:** `GET /api/v1/auth/me`
- **Auth:** Bearer token required (`@SecurityRequirement(name = "bearerAuth")`)
- **Response wrapper:** `ApiResponse<CurrentUserResponse>`
- **Success message:** `Current user profile fetched successfully`

### Response DTO

Create a dedicated record `CurrentUserResponse` in `auth/dto`:

- `String userId`
- `String email`
- `String fullName`
- `UserStatus status`
- `boolean emailVerified`

No session or token fields are included.

## Trusted Identity and Data Flow

1. Controller delegates to `authService.me()` with no client-provided identity input.
2. Service reads authenticated user id via `SecurityContextUtil.currentUserId()`.
3. Service loads user by id from `UserService`.
4. Service maps allowed entity fields to `CurrentUserResponse`.
5. Controller returns `ApiResponse.success("Current user profile fetched successfully", data)`.

## Security and Privacy Rules

- Do not accept `userId` via request body, query, or path for `/me`.
- Do not expose sensitive fields, including:
  - `passwordHash`
  - any access/refresh token values
  - token expiry/session internals
  - provider subject identifiers
  - audit/internal operational metadata

## Error Handling

- If authentication is missing/invalid principal: `SecurityContextUtil.currentUserId()` raises `AppException` with `UNAUTHORIZED` and HTTP 401.
- If authenticated user id cannot be found in DB: service raises `AppException(USER_NOT_FOUND, 404, "User not found")`.
- All errors are handled by `GlobalExceptionHandler`.

## Implementation Changes

1. Add `CurrentUserResponse` record in `auth/dto`.
2. Change `AuthService.me()` return type from `AuthResponse` to `CurrentUserResponse`.
3. Implement `AuthServiceImpl.me()` with:
   - trusted user id lookup from security context
   - user fetch by id
   - safe field mapping
4. Update `AuthController.me()` response generic and message.

## Testing Strategy

- Add/update controller or service tests for `/me` success path:
  - authenticated request returns current user profile
  - response includes only defined safe DTO fields
- Add/update unauthorized path test:
  - missing/invalid auth returns 401 through existing security chain
- Add/update user-not-found path test if covered at service layer:
  - authenticated principal with missing DB user returns 404 `USER_NOT_FOUND`

## Risks and Mitigations

- **Risk:** Accidental reuse of `AuthResponse` could expose auth session fields.
  - **Mitigation:** Use dedicated `CurrentUserResponse` contract.
- **Risk:** Future developers reintroduce client-supplied identity.
  - **Mitigation:** Keep method signature free of identity inputs and enforce `SecurityContextUtil.currentUserId()` in service.

## Acceptance Criteria

- `/api/v1/auth/me` returns real data for the authenticated user.
- User identity is obtained only from token-backed security context.
- Response never contains sensitive auth/security data.
- Unauthenticated calls return 401 and nonexistent users return 404 with existing error handling conventions.
