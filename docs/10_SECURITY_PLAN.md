# 10 — Security Plan

## 1. Objectives
Protect customer data, payment flows, admin operations, and system integrity for a US-customer-facing ecommerce platform operated from India.

## 2. Authentication
- **Customers:** email + strong password; hashing with BCrypt (Spring Security default) or Argon2id if selected; never store plaintext.
- **Session/token model:** choose one in Phase 2 — (A) JWT access + refresh with rotation, or (B) server-side sessions with secure cookies. Document tradeoffs (CSRF, revocation, scaling).
- **Password policy:** minimum length/complexity balanced with UX; breach/password reuse guidance post-MVP.
- **Forgot/reset:** time-limited, single-use tokens; generic responses to prevent account enumeration.
- **MFA:** post-MVP for admin (recommended).

## 3. Authorization
- **RBAC:** `CUSTOMER`, `ADMIN` (and optional `STAFF`). Enforce at API (method/security config) and UI route guards (defense in depth).
- **Object-level checks:** customers can access only their own orders/addresses/cart.
- **Admin audit:** log sensitive actions (status overrides, refunds, customer data views/exports if added).

## 4. Password and account security
- Rate limit login/forgot-password endpoints.
- Account lockout or progressive delays after repeated failures (tuned to avoid DoS).
- Secure password reset delivery via verified email provider (when configured).

## 5. API validation and safety
- Bean Validation on all DTOs; whitelist fields (no mass assignment).
- Centralized exception handling; no stack traces or sensitive details to clients in production.
- Size limits on uploads/requests; pagination caps.
- Idempotency for payment and webhook processing.

## 6. CORS
- Allow only known frontend origins (dev + prod domains).
- Do not use wildcard `*` with credentials.
- Restrict methods/headers as needed.

## 7. CSRF considerations
- If using **cookie-based sessions**, implement CSRF tokens (Spring Security) for state-changing requests; exempt verified webhook endpoints appropriately.
- If using **JWT in Authorization header** (no cookies for auth), CSRF risk profile differs but XSS protections remain critical.
- Decision documented with chosen auth mechanism.

## 8. Rate limiting
- Apply at API gateway/reverse proxy and/or application (Bucket4j/Redis later if needed).
- Prioritize: login, register, forgot-password, contact form, checkout/payment endpoints, webhooks (protect processing).

## 9. Payment security
- **PayPal-only** processing in MVP (no card numbers stored by Fetch).
- Create/capture/verify/refund **server-side only**.
- Webhook signature verification mandatory; reject unverified payloads.
- Amount/currency validation against internal order before capture/finalization.
- Reconcile via webhooks; handle duplicates safely.

## 10. Secrets management
- Environment variables or secret manager in deployment platform.
- Separate sandbox vs. production PayPal credentials.
- Never commit `.env` secrets; provide `.env.example` templates (without real values) in implementation.
- Rotate compromised keys; least-privilege access for operators.

## 11. Logging and monitoring
- Structured logs with correlation IDs; exclude passwords, tokens, full addresses where unnecessary, and all payment secrets.
- Audit tables/records for payments and webhook events.
- Alerts for elevated payment failures, webhook verification failures, and 5xx spikes (ops phase).

## 12. Database security
- Private network access; no public DB exposure.
- Least-privilege DB user for app; separate migration/admin credentials.
- Encrypted connections (TLS) where supported.
- Regular backups and tested restores; retention policy defined pre-launch.

## 13. Webhook verification
- PayPal: verify transmission signature per official method using webhook ID and headers.
- Future supplier webhooks: HMAC/shared secret verification + replay protection (timestamp/nonce) as supported.

## 14. Personal information protection
- Collect minimum necessary PII (shipping/contact).
- Define retention and deletion/support processes (customer requests) — legal review recommended for US/privacy expectations.
- Access controls for admin PII views; consider masking in lists.
- Secure transport (HTTPS) and secure storage practices.

## 15. Dependency and platform security
- Keep Spring/React dependencies updated; address critical CVEs.
- Use dependency scanning in CI (Phase 13).
- Harden headers (CSP as feasible, HSTS at edge, X-Content-Type-Options, frame protections).

## 16. Incident readiness (MVP-level)
- Runbooks for payment mismatch, webhook outage, and suspected account compromise.
- Contact path for PayPal/supplier/platform support.
- Ability to disable checkout or payment methods via configuration/feature flag.
