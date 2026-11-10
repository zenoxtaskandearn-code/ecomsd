# 11 — Testing Plan

## 1. Strategy
Testing is risk-based and phased. **Payments, order state, and fulfillment triggers** receive the highest rigor. MVP emphasizes integration/API correctness, security basics, and responsive UX; expand automation over time.

## 2. Frontend testing
| Type | Scope | Tools/approach (planned) |
|---|---|---|
| Unit | Utilities, formatters, validators, hooks logic | Vitest/Jest-compatible setup for JS (chosen in Phase 2) |
| Component | Critical UI (cart, checkout forms, product selectors, states) | React Testing Library |
| Visual/UX checks | Design system consistency, responsive layouts | Manual QA + screenshots against reference description/image when available |
| Accessibility | Keyboard flow, labels, focus, contrast | axe checks + manual audit |

**Key scenarios:** add-to-cart, variant selection, form validation errors, checkout error handling, loading/empty/error states, route guards.

## 3. Backend testing
| Type | Scope | Tools |
|---|---|---|
| Unit | Service logic (totals, state transitions, mappers) | JUnit 5, Mockito |
| Slice/integration | Controllers + validation, repository queries | Spring Boot Test, MockMvc, Testcontainers MySQL (recommended) |
| Security | AuthZ rules for customer/admin endpoints | Spring Security test support |

**Key scenarios:** pricing recalculation, idempotent payment capture, duplicate webhook ignored, invalid state transitions rejected, RBAC denials.

## 4. API testing
- Contract-style checks for request/response shapes and error envelopes.
- Postman/Insomnia collection or automated HTTP tests in CI.
- Cover all critical endpoints: auth, catalog, cart, checkout, PayPal create/capture (mocked provider), webhooks, admin order updates.

## 5. Database testing
- Migration smoke tests (when migrations introduced).
- Constraint/index verification for unique keys (email, slug, SKU, order number, webhook event ID).
- Transaction behavior for order+payment updates.

## 6. Payment sandbox testing (PayPal)
| Case | Expected |
|---|---|
| Successful approval + capture | Order becomes paid; payment recorded; cart handled |
| Buyer cancels approval | No paid order; clear UI state |
| Capture fails/declined | payment_failed; retry path |
| Duplicate capture request | Idempotent result; no double charge record |
| Webhook before/after capture | Consistent final state |
| Duplicate webhook event_id | Ignored after first process |
| Invalid webhook signature | Rejected (4xx) and logged |
| Amount mismatch attempt | Rejected server-side |
| Refund (sandbox) | Refund recorded; order/payment status updated per rules |

## 7. Supplier integration testing
- **Manual workflow:** admin queue usability, status transitions, tracking entry, audit notes.
- **Adapter tests:** mock supplier API responses (success, timeout, 4xx/5xx, duplicate submit).
- **Fallback:** ensure system remains operable when supplier integration disabled/unavailable.

## 8. Security testing
- OWASP Top 10 awareness: injection, broken auth, sensitive data exposure, XSS, CSRF (as applicable).
- Verify CORS origins, auth on admin routes, object-level authorization.
- Secrets scanning in repo/CI.
- Dependency vulnerability checks.

## 9. Responsive and cross-browser testing
- Breakpoints: mobile (small/large), tablet, desktop.
- Browsers: latest Chrome, Edge, Safari (where possible), Firefox — prioritize US customer usage patterns.
- Verify hero composition, nav, cart/checkout, forms, and tables (admin).

## 10. End-to-end testing
- MVP: critical happy paths automated selectively (e.g., Playwright/Cypress) — browse → cart → checkout (mocked PayPal or sandbox in controlled env) → order confirmation; admin login → view order → add tracking.
- Keep E2E suite small and stable; rely on integration tests for breadth.

## 11. Test environments
- **Local:** dev DB, PayPal sandbox, mocked supplier.
- **Staging:** production-like config, HTTPS, sandbox payments, seeded catalog.
- **Production:** live PayPal only after checklist; monitoring and rollback plan.

## 12. Quality gates (pre-release)
- All critical payment/order integration tests pass.
- No open critical/high security findings.
- Manual QA sign-off for reference-aligned UI and core flows.
- Backup/restore and logging verified.
