# 14 — Authentication & Authorization Design (JWT, Proper MVP)

## 1. Decision
- **Chosen:** **JWT access token + refresh token** (stateless API) with **RBAC** for `CUSTOMER`, `ADMIN`, `STAFF`.
- **Rationale:** Fits a React SPA + Spring Boot REST API, supports separate customer/admin areas in the same auth system, scales horizontally without server session affinity, and works cleanly with CORS. Tradeoff: requires careful refresh/rotation, revocation strategy, and secure client token handling.
- **Not using cookie sessions as primary API auth in MVP** (can be revisited), which reduces CSRF complexity for bearer-token API calls; still enforce strict CORS and protect against XSS.

## 2. Security principles
- Never store plaintext passwords; use **BCrypt** (Spring Security `PasswordEncoder`).
- Never return `password_hash`, refresh secrets (if hashed/stored), or internal security data in API responses.
- Never trust frontend role/payment claims; derive identity/roles from validated tokens and DB state.
- Rate limit auth endpoints (`/register`, `/login`, `/refresh`, forgot-password later).
- Use HTTPS in non-local environments; keep secrets in env/secret manager.
- Log auth failures/auditable admin actions without leaking credentials or full tokens.

## 3. Token model (recommended)
| Token | Purpose | Suggested TTL | Transport | Notes |
|---|---|---:|---|---|
| **Access token** (JWT) | Authorize API requests | 10–15 min | `Authorization: Bearer <token>` | Short-lived; contains minimal claims |
| **Refresh token** | Obtain new access token | 7–30 days (configurable) | Request body (MVP) or secure HttpOnly cookie (hardening option) | Rotate on refresh; support revocation |

### Access token claims (minimal)
- `sub`: user id (or stable subject)
- `email`: optional (convenient, not sensitive beyond account)
- `role`: `CUSTOMER` / `ADMIN` / `STAFF`
- `iat`, `exp`, `jti` (recommended for traceability/revocation patterns)

Avoid putting PII/addresses/permissions bloat in JWT.

### Refresh token strategy (proper MVP)
- Generate cryptographically strong random refresh tokens.
- **Persist refresh tokens server-side** (recommended for MVP revocation/rotation): e.g. `refresh_tokens` table with `id`, `user_id`, `token_hash` (hash, not plaintext), `expires_at`, `revoked_at`, `created_at`, `replaced_by_token_id`, `user_agent/ip` (optional, privacy-aware).
- **Rotate on every refresh:** issue new refresh token, revoke old one; if an already-revoked/rotated token is reused, treat as suspicious and revoke the token family/session per policy.
- On logout, revoke the active refresh token(s) for that session (or all sessions if “logout everywhere” is added later).

## 4. API endpoints (contract target)
| Method | Path | Auth | Purpose | Key behavior |
|---|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Create customer | Validate, unique email, hash password, create `CUSTOMER` user, return user summary + tokens (or require login — choose consistently) |
| POST | `/api/v1/auth/login` | Public | Authenticate | Verify credentials, return access+refresh; generic failure message (no account enumeration nuance beyond safe UX) |
| POST | `/api/v1/auth/refresh` | Public (refresh token) | Refresh session | Validate/rotate refresh token, return new access+refresh; 401/403 on invalid/expired/revoked |
| POST | `/api/v1/auth/logout` | Authenticated or refresh-based | Revoke session | Revoke refresh token; client clears tokens |
| GET | `/api/v1/users/me` | `CUSTOMER`/`ADMIN`/`STAFF` | Current user | Return safe profile + role |
| PATCH | `/api/v1/users/me` | Authenticated | Update profile | Whitelisted fields only |

**Admin access:** admins sign in through the same auth endpoints and receive role claims; `/api/v1/admin/**` requires `ADMIN` (and optionally `STAFF` for scoped operations).

## 5. Spring Security design (implementation plan)
- **Stateless security:** `SessionCreationPolicy.STATELESS`.
- **Filter chain:** JWT authentication filter validates bearer access token, sets `SecurityContext`.
- **Authorization rules (initial):**
  - Permit: `/api/v1/auth/**` (except endpoints requiring auth if any), public catalog/content/contact, `POST /api/v1/webhooks/paypal` (verified separately), actuator health as configured.
  - Authenticated: `/api/v1/users/**`, `/api/v1/cart/**`, `/api/v1/checkout/**`, `/api/v1/orders/**`, `/api/v1/payments/**` (customer-owned/payment flows as specified).
  - Role admin: `/api/v1/admin/**` -> `hasAnyRole('ADMIN','STAFF')` as appropriate.
- **CSRF:** typically disabled for stateless bearer-token APIs; document and compensate with CORS + token handling + XSS protections. (If refresh moves to HttpOnly cookie later, revisit CSRF for that endpoint.)
- **CORS:** allow configured frontend origins, required methods/headers (`Authorization`, `Content-Type`), no wildcard with credentials.
- **Password encoder:** `BCryptPasswordEncoder`.
- **Method/object security:** ownership checks in services (e.g., order belongs to current user).

## 6. Frontend integration plan (replace demo auth)
Current frontend has demo `AuthContext`/`localStorage` and `AdminAuthContext`. Proper integration:
1. **Unify auth client** around real `/api/v1/auth/*` and `/users/me` (admin/customer distinguished by `role`).
2. **Axios client** (`frontend/src/services/apiClient.js`): base URL from env (`VITE_API_BASE_URL`), attach access token, response interceptor to attempt **single refresh** on 401 (queue concurrent requests), then retry or logout.
3. **Token storage (MVP):** in-memory access token + persisted refresh token with care (or chosen storage strategy). Document XSS risk; harden later with HttpOnly cookie refresh if desired.
4. **Route guards:** replace demo guards with real auth state; admin routes require authenticated role `ADMIN/STAFF`.
5. **UI states already built:** keep button loading, form errors, skeletons/loaders, protected redirects.
6. **Account/admin pages:** switch demo data to API-backed hooks/services incrementally.

## 7. Data model additions
- Ensure `users.role` supports `CUSTOMER`, `ADMIN`, `STAFF`; `status` supports active/disabled.
- Add `refresh_tokens` table in migration plan (recommended) or document alternative.
- Optional later: `password_reset_tokens`, `email_verifications`, audit events.

## 8. Error handling (auth)
- `400` validation, `401` invalid/expired credentials or token, `403` insufficient role/disabled account, `409` email already registered, `429` rate limited.
- Use safe, consistent messages. Do not reveal whether a specific email exists in sensitive flows beyond normal registration conflict UX.

## 9. Testing checklist
- Register/login happy path and validation failures.
- Duplicate email conflict.
- Password hashing (never plaintext stored/returned).
- Access token required/expired/invalid.
- Refresh rotation success; revoked/reused refresh token rejected.
- RBAC: customer cannot access `/api/v1/admin/**`; admin can (as scoped).
- Object ownership: user cannot access another user’s orders/addresses/cart.
- CORS preflight/allowed origins.

## 10. Implementation tasks (when approved)
1. Add security/config dependencies and JWT utility/service.
2. Implement `refresh_tokens` migration + entity/repository (if using DB strategy).
3. Implement auth/user services, controllers, DTOs, validation.
4. Configure `SecurityFilterChain`, JWT filter, CORS, exception handling.
5. Add tests (MockMvc + Testcontainers).
6. Integrate frontend `apiClient` and replace demo auth contexts/guards.
7. Seed/create initial admin user securely (local/dev script or manual SQL procedure documented; **no hardcoded production credentials**).

## 11. Remaining defaults (pending your approval)
- **Guest checkout MVP:** require account (recommended).
- **Java:** 21 LTS (recommended) or 17.
- **US tax MVP:** no automated tax, clear messaging.
- **Migrations:** Flyway.

**Reply with:** “Approve backend scaffolding with JWT + [Java 21/17] + [guest checkout yes/no] + [tax approach]” and I’ll proceed to the next implementation phase you authorize (planning-safe) or start Phase 2/4 scaffolding if you explicitly approve coding.