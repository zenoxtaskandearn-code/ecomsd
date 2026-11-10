# FETCH Order Flow Implementation Report (current state)

**Date:** 2026-11-11  
**Repository:** `C:\Users\amitr\OneDrive\Desktop\Dropshoting` (Lupine & Leash / Fetch codebase)

> This report documents work completed in this iteration and the remaining gaps against the requested end-to-end ecommerce, payment, tracking, CJ fulfillment, financial, and reliability requirements. It is **not** a claim that all requested phases/scenarios are complete.

---

## 1. Work completed

### Phase 1 — Audit (completed)
- **Created:** `FETCH_ORDER_FLOW_AUDIT.md` with findings on working/partial/missing/risky areas, duplicate/payment/fulfillment risks, financial-loss risks, security gaps, and prioritized plan.

### Safety & payment correctness (implemented, committed/pushed)
- **CJ manual-approval default:** `CJ_AUTO_SUBMIT_ON_PAID` defaulted to **`false`** (`CjAutoSubmitter`, `render.yaml`). Auto-submit on paid/create remains disabled unless explicitly enabled.
- **CJ submit guards:** rejects cancelled/refunded orders, requires server-confirmed payment for standard submit flow, and remains idempotent when `cjOrderId` already exists (sync-on-existing).
- **PayPal verification hardening:** capture and webhook reconciliation verify captured amount/currency against server-side order totals; mismatches are recorded/acknowledged without marking orders paid/fulfilled. Existing captured/paid orders remain idempotent.

### Phase 2 foundations — state/history & fulfillment wiring (implemented, committed/pushed)
- **DB migration:** `V13__order_status_history_and_status_fields.sql` adds `orders.payment_status`, `orders.fulfillment_status`, and `order_status_history` (domain/from/to/source/note/metadata/timestamps) with backfill/indexes.
- **Domain/service:** `OrderStatusHistory` entity/repository and `OrderStatusService` for recording ORDER/PAYMENT/FULFILLMENT transitions and syncing derived statuses.
- **Order/Payment wiring:** order creation records initial status history and sets separate payment/fulfillment states; PayPal capture/failure updates record payment/order/fulfillment history with server-confirmed states.
- **CJ fulfillment safety & wiring:** `CjFulfillmentService` enforces pre-submit checks (paid/allowed states, all items have active variants with CJ variant mapping, shipping address completeness for country/city/line1), sets REVIEW_REQUIRED/PROCESSING/SUBMITTED fulfillment states with history, records failures, and sync maps CJ/tracking states to order/fulfillment history (without auto-cancelling paid orders on supplier cancellation signals).
- **Admin transitions:** admin status updates and manual shipments now record status/fulfillment history and include basic safeguards (cannot mark PAID without captured payment; cannot reopen cancelled/refunded orders via simple status change).

### Build status
- Backend compiles successfully: `mvn -DskipTests compile` (Java 17). No automated scenario tests added/run in this environment.

### Previously delivered/reused (relevant)
- Server-side order creation from cart with snapshots; PayPal create/capture + webhook signature verification and event-id idempotency; enriched customer tracking; CJ import/submit/sync/webhook/scheduler primitives; admin catalog/order/dashboard improvements.

---

## 2. Existing features reused (not rebuilt)
- React/Tailwind/Lucide frontend, Spring Boot/Security/JPA/Flyway/Maven backend, PostgreSQL schema/migrations.
- Auth (JWT + refresh), catalog/cart/checkout flows, PayPal REST integration, CJ client/service/webhook foundations, admin/customer pages and APIs.

---

## 3. Files created
- `FETCH_ORDER_FLOW_AUDIT.md`
- `FETCH_ORDER_FLOW_IMPLEMENTATION_REPORT.md`

## 4. Files modified (this iteration)
- `backend/src/main/java/com/fetch/backend/order/cj/CjAutoSubmitter.java`
- `backend/src/main/java/com/fetch/backend/order/cj/CjFulfillmentService.java`
- `render.yaml`

## 5. Database changes
- **None in this iteration.** (Prior migrations V1–V12 exist; CJ/order/payment/shipment fields already present.)

## 6. Key API endpoints (existing, relevant)
- Auth: `POST /api/v1/auth/register|login|refresh`, logout
- Catalog/cart: `GET /api/v1/categories`, `GET /api/v1/products`, `GET /api/v1/products/{slug}`, cart endpoints
- Orders (customer): `POST /api/v1/orders/preview`, `POST /api/v1/orders`, `GET /api/v1/orders`, `GET /api/v1/orders/{orderNumber}`
- Payments: `GET /api/v1/payments/paypal/config`, `POST /api/v1/payments/paypal/create`, `POST /api/v1/payments/paypal/capture`, `POST /api/v1/webhooks/paypal`
- CJ/Admin: `POST /api/v1/webhooks/cj`, admin order/product/catalog/CJ import/submit/sync endpoints (role-protected)
- Health: `/actuator/health`

## 7. Environment variables (relevant)
- **Core:** `SPRING_PROFILES_ACTIVE`, `DB_*`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `VITE_API_BASE_URL`
- **PayPal:** `PAYPAL_MODE`, `PAYPAL_BASE_URL`, `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_WEBHOOK_ID`
- **CJ:** `CJ_API_ENABLED`, `CJ_API_KEY`, `CJ_WEBHOOK_SECRET`, `CJ_AUTO_SUBMIT_ON_PAID` (now default **false**), `CJ_AUTO_SUBMIT_ON_CREATE`, `CJ_FULFILLMENT_SYNC_ENABLED`, `CJ_FULFILLMENT_SYNC_DELAY_MS`, `CJ_FULFILLMENT_SYNC_INITIAL_DELAY_MS`, `CJ_FULFILLMENT_SYNC_BATCH_SIZE`, `CJ_ORDER_CREATE_PATH`, `CJ_FROM_COUNTRY_CODE`, `CJ_LOGISTIC_NAME`

## 8. Integration status
- **PayPal:** server-side create/capture + webhook signature verification and idempotent event handling implemented; production E2E (webhook delivery, all payment states/refunds/disputes) requires env/webhook configuration and verification. Sandbox capture previously confirmed for a test order.
- **CJdropshipping:** official-API import/submit/sync/webhook/scheduler primitives implemented and safer defaults applied (manual approval). **Not fully production-reliable yet** due to external constraints observed: Render egress IP rate-limited by CJ, logistics/route/payment/confirmation requirements vary, and end-to-end auto-submit/sync from prod needs correct env, allowlisted/static egress IP, webhook registration, and verification.

## 9. Tests executed (this iteration)
- **Automated tests added/run:** none added in this iteration.
- **Build/compile:** `mvn -DskipTests compile` — **PASS** (backend). Frontend build previously verified in session.
- **Live production checks (read-only):** health `UP`; public catalog/product endpoints responsive and serving enriched fields; protected endpoints correctly require auth (401). Full authenticated admin/customer and CJ/PayPal E2E flows not executable without credentials and external configuration.
- **Required scenarios (18):** **not fully executed/verified** (require mocked CJ, PayPal Sandbox flows, concurrency/security cases, and environment access).

## 10. Remaining work to fully satisfy the requested spec (must be completed)
- **State machine & idempotency (complete):** enforce a full validated state machine for all required states/transitions, persist/use status history everywhere, and add robust idempotency/outbox-style guards for checkout/cart conversion, PayPal create/capture/retries/timeouts, CJ submit/retry/timeout reconciliation, refunds, and concurrent/race cases (including duplicate-purchase prevention).
- **CJ fulfillment (complete):** explicit manual-approval workflow in admin, supplier-order/fulfillment records, and full pre-submit gates including real-time/current **stock** and **supplier price recheck/thresholds**, shipping method availability and **shipping cost limits**, destination support, expected-vs-actual cost checks, and fraud/manual-review flags; implement safe handling of CJ order confirmation/payment requirements per official API capabilities (no undocumented spend) and reliable tracking sync/webhooks.
- **Financials:** per-order `OrderCost`/breakdown (customer subtotal/shipping/discount/tax/fees/refunds, supplier product/shipping/duties/other/conversions, business costs), correct net/gross/contribution/estimated profit calculations with currency handling, and admin financial views/exports (CSV).
- **Notifications:** queue/retry/dedup notification system and templates/events for order/payment/processing/fulfillment/shipment/tracking/delivery/cancel/refund (document env vars if not configured).
- **Returns/refunds/disputes:** entities/APIs/UI for customer return requests and admin review/processing; PayPal refund initiation/status via supported APIs with duplicate-refund protection; handling for chargebacks/disputes/lost/damaged/incorrect/cancelled cases.
- **Security & reliability hardening:** rate limiting/throttling, comprehensive validation, audit logs for sensitive actions/events, stricter webhook verification/policies (CJ fail-safe), object-level access controls/secure tracking references, and error/retry/transaction safety review.
- **Testing:** implement and run all 18 required scenarios with **mocked CJ** and **PayPal Sandbox** (including concurrency/idempotency/security), record results, and fix defects; verify responsive/admin+customer workflows.

## 11. Manual/external configuration required (cannot be completed from code alone)
- Deploy/verify latest on Render with correct env vars/secrets and CORS/base URLs (migrations V13+ will apply on deploy).
- **Rotate exposed secrets:** CJ API key/credentials, DB, JWT, PayPal (were exposed in chat/logs).
- CJ: static/allowlisted egress IP/proxy for Render (`74.220.48.219` previously rate-limited), correct API key/openId/webhook secret, warehouse/logistics/from-country/order-create/payment/confirmation settings per official docs, webhook registration, and sync verification for submitted orders (e.g. FT-652897).
- PayPal: sandbox/live credentials, `PAYPAL_WEBHOOK_ID`, webhook events/subscription, and end-to-end payment/refund/webhook validation.
- Data quality: populate/verify variant supplier/fulfillment costs and product enrichment; reconcile/clean test or inconsistent records as needed.

## 12. Deployment instructions (current)
1. Ensure PostgreSQL, backend, and frontend services configured (Render blueprint or equivalent) with correct env vars/secrets and `VITE_API_BASE_URL` set before frontend build.
2. Deploy latest `main` (backend runs Flyway migrations; frontend built/published). Verify `/actuator/health` = UP.
3. Configure PayPal and CJ credentials/webhooks/endpoints; keep `CJ_AUTO_SUBMIT_ON_PAID=false` until manual-review gates and CJ reliability are fully verified, then enable automatic fulfillment only if safe.
4. Validate auth, checkout/payment (server-confirmed), order visibility, admin controls, CJ submit/sync/tracking, and financials before processing real orders/money.

---

**Bottom line:** Phase 1 audit and a critical fulfillment-safety default change are completed and committed. The system has substantial foundations, but **additional phased implementation, hardening, external configuration, and scenario testing are required** before it meets all acceptance criteria for a fully reliable, financially safe production dropshipping operation.
