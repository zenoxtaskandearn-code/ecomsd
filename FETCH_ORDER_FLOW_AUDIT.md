# FETCH Order Flow Audit (Lupine & Leash codebase)

**Project audited:** `C:\Users\amitr\OneDrive\Desktop\Dropshoting` (brand/UI: Lupine & Leash, packages/IDs still `com.fetch.backend` / Fetch naming in places)  
**Date:** 2026-11-11  
**Scope:** Full-stack audit of customer checkout, payments, order lifecycle, tracking, admin fulfillment, CJdropshipping integration, security, and financial/order reliability.

> This audit is based on the current repository state and runtime observations during this session. It focuses on correctness, financial safety, security, and dropshipping reliability. External systems (Render env, CJ account/API behavior, PayPal dashboard/webhooks, production DB contents) require manual verification.

---

## 1. Executive summary

### Overall status
- **Core commerce flow exists (partially production-ready):** catalog, auth, cart, order creation, PayPal create/capture + webhook handling, order history/detail, admin order management, and CJ submit/sync primitives are implemented.
- **Major gaps for the requested “complete, reliable ecommerce + dropshipping operation”:** no dedicated order state machine with separate payment/order/fulfillment states & history, limited idempotency/financial safeguards (especially CJ purchase/price/stock rechecks), no CJ supplier-product mapping entity/workflow, no configurable manual-approval-by-default fulfillment mode with cost/stock/price gates, no complete financial/cost-breakdown model, no returns/refunds/disputes workflow, no customer notification/email system, and several security/reliability hardening items missing.
- **Deployment/integration risk observed:** production backend was previously behind latest code (causing stale API behavior) and CJ fulfillment from Render is impacted by CJ IP/rate limiting; end-to-end CJ/PayPal webhook automation in prod is not fully verified.

### Top priorities (fix first)
1. **Financial & fulfillment safety:** enforce manual approval by default, idempotent CJ submission, strict paid/stock/price/shipping checks before any supplier spend, and explicit handling of unknown CJ charge/payment behavior.
2. **Payment/order correctness:** strengthen server-side amount/currency verification, idempotency keys, webhook/order-state consistency, and clear separation of payment vs order vs fulfillment status.
3. **State, auditability, and history:** add robust order state transitions, status history, tracking events, and supplier/payment audit records.
4. **Customer/admin completeness:** reliable tracking timeline from verified data, notifications, returns/refunds/disputes, and complete admin financial/fulfillment controls.
5. **Security hardening:** access controls for order references, webhook verification guarantees, rate limiting, validation, and secret rotation/management.

---

## 2. What is already working (or substantially implemented)

### 2.1 Customer/auth/catalog/cart
- **Authentication:** register/login/refresh/logout flows with JWT access tokens + refresh tokens (`AuthService`, JWT config/filter, Spring Security). Passwords hashed with BCrypt.
- **Catalog:** categories/products/variants/images, public product listing/detail APIs, CJ import (My Products preview/select/import, ACTIVE + deduped by `cjProductId`), admin catalog management including variant pricing/cost fields and admin-only supplier/internal fields.
- **Cart:** server-side cart per authenticated user, add/update/remove, availability checks against active variant/product, totals computed from current variant prices on cart response (`CartService`).
- **Customer account:** order history and order detail pages consume backend APIs when enabled; shipping address snapshot stored on order.

### 2.2 Checkout & order creation
- **Checkout UI/API flow:** collects contact/shipping details, creates server order (`POST /api/v1/orders`) with server-calculated totals from cart/variants, then initiates PayPal payment (`/payments/paypal/create`, `/payments/paypal/capture`) (`CheckoutPage`, `OrderService`, `PaymentService`, `PaymentController`).
- **Order persistence:** `orders` + `order_items` with snapshots (product name, variant title, SKU, unit/line totals), address snapshots (JSONB), unique `order_number`, initial `PENDING_PAYMENT`, placed timestamp.
- **Duplicate-click mitigation (partial):** order number generation retries on unique constraint collision (`DataIntegrityViolationException`, up to 5 attempts). Cart converted to `CONVERTED` after order creation.

### 2.3 Payments (PayPal)
- **Server-side integration:** PayPal config endpoint, create order, capture order, and webhook endpoint implemented (`PayPalClient`, `PaymentService`, `PayPalWebhookController`).
- **Verification/capture:** capture performed server-side; order marked `PAID` only when capture status is `COMPLETED` (`applyCaptured` sets payment `CAPTURED`, order `PAID`, `paidAt`).
- **Webhook handling:** signature verification via PayPal verify-webhook-signature API (requires `PAYPAL_WEBHOOK_ID`), idempotency by `(provider,event_id)` in `webhook_events`, processes capture-completed events to reconcile payment/order.
- **Secrets handling:** PayPal client secret not exposed to frontend; frontend receives only public config/client id when configured.

### 2.4 Order tracking & admin (current implementation)
- **Customer tracking (improved):** order API/UI now expose timeline events with timestamps/states, fulfillment/supplier info, expected delivery estimate, and shipment details (carrier/tracking/url/status/shipped/delivered/last tracked/source) when available.
- **Admin orders:** list/detail, status update, manual shipment add, CJ submit/sync actions, CJ status/error visibility (`AdminOrderController`, admin UI).
- **CJ fulfillment primitives:** official CJ API client (auth, My Products, product detail, create order, order detail, tracking, stock query), auto-submit hooks on paid/created, submit/sync services, CJ webhook ingestion/idempotency/sign-check logic, scheduler for periodic sync (`CjDropshippingClient`, `CjAutoSubmitter`, `CjFulfillmentService`, `CjFulfillmentScheduler`, `CjWebhookController`).
- **Admin dashboard metrics:** expanded stats including sales/revenue/orders/fulfillment/CJ counts and estimated profit based on variant cost fields when present.

### 2.5 Schema foundations present
- Tables/entities for users, addresses, refresh tokens, categories/products/variants/images, carts/cart_items, orders/order_items, payments, shipments, suppliers/supplier_products, webhook_events, contact_messages, and CJ-related fields on orders/shipments/products/variants (Flyway V1–V12).

---

## 3. What is partially implemented

- **Customer checkout:** basic flow works, but shipping/tax/duties/discounts are effectively static/MVP (`shippingAmount`/`taxAmount`/`discountAmount` set to 0), no multi-country shipping rules, no real-time shipping/tax calculation, limited address validation, and limited stock reservation/commit semantics.
- **Idempotency & duplicate protection:** order-number collision retry and webhook event-id idempotency exist; however cart→order conversion, payment create/capture retries, CJ submit, and some client actions lack robust idempotency keys/guards for all double-submit/refresh/timeout cases.
- **Order/customer tracking:** rich timeline/UI from stored data is implemented, but accuracy depends on CJ submit/sync success and available carrier data; no full `OrderStatusHistory`/`TrackingEvent` persistence model.
- **CJ integration:** core API calls, mapping fields on products/variants, submit/sync/webhook/scheduler exist; but no dedicated supplier-order/fulfillment domain model, no strict pre-submit revalidation (current stock/price/shipping/destination/cost thresholds), no manual-approval-by-default gate, and no verified CJ “payment/confirm” handling beyond API behavior already coded.
- **Admin management:** useful order/product/CJ operations exist, but not the full lifecycle/financial/exception management required (filters/views for all states, review queue, refund workflows, exports, audit/payment history views, per-order cost breakdown).
- **Security:** JWT stateless auth, role checks for `/api/v1/admin/**`, CORS config, webhook signature checks present; but protections (rate limiting, stricter object-level checks in all paths, anti-fraud/review flags, comprehensive audit logging) are incomplete.
- **Profit/costing:** estimated profit and admin cost fields exist; complete per-order financial breakdown (customer fees/refunds, supplier actuals, conversions, net/gross distinctions) is not implemented.

---

## 4. What is completely missing (relative to requirements)

- **Dedicated order lifecycle/state machine:** explicit state machine with validated transitions, separate **payment status**, **order status**, and **fulfillment status**, and persistent **order status history** for all changes.
- **Comprehensive domain entities/tables (as required):** e.g. `SupplierOrder`, `Fulfilment`, `TrackingEvent`, `OrderStatusHistory`, `Refund`, `ReturnRequest`, `OrderCost`, `Notification`, `AdminNote`, `AuditLog` (and/or equivalents) with full relationships and workflows. (Current schema has `supplier_products` mapping table, `payments`, `shipments`, `webhook_events`, CJ fields, but not the full set/behavior.)
- **CJ supplier-product mapping workflow:** a complete, enforced mapping/management flow ensuring every fulfillable order item resolves to correct CJ product/variant/warehouse/shipping method with availability/cost metadata and validation.
- **Configurable fulfillment modes with safety gates:** **MANUAL_APPROVAL as default** and **AUTOMATIC_FULFILMENT** with all required pre-submit checks (paid, mapping, in-stock, supplier price change threshold, shipping available/cost limits, destination supported, not already submitted, no fraud/manual-review flag). Current behavior auto-submits on paid by default in configuration logic (with some checks) rather than the required manual-approval-first safety model.
- **Supplier payment/confirmation handling (verified/explicit):** explicit investigation-backed handling of CJ order confirmation/payment requirements and safe admin-visible states when CJ requires manual payment/confirmation; no undocumented payment attempts (must be ensured/documented).
- **Customer notifications:** email/notification service, templates, queue/retry/dedup for all required events (order/payment/processing/fulfillment/shipped/tracking/delivery/cancel/refund) and documented env vars when not configured.
- **Returns, refunds, disputes:** customer return requests (reason/description/images/status), admin review/approval/rejection/notes, supported PayPal refund initiation & tracking, and handling for disputes/chargebacks/lost/damaged/incorrect items/cancellations.
- **Complete financial breakdown & reporting:** per-order customer payment breakdown (incl. processing fees/refunds/taxes), supplier actuals (product/shipping/duties/other/conversion), business costs, net/gross/contribution/estimated profit calculations with correct currency handling; admin views and CSV export.
- **Order tracking access hardening:** secure, verifiable order-reference tracking access (e.g. tokenized/verified link) for non-authenticated contexts if exposed; explicit protections ensuring no cross-customer access in all tracking/order endpoints.
- **Testing artifacts & full scenario coverage:** automated/integration tests for the 18 required scenarios (mocked CJ, PayPal Sandbox), responsive verification, and documented results.

---

## 5. What is incorrectly implemented / risky vs requirements

- **Fulfillment safety default:** CJ auto-submit on paid is enabled by default in code/config paths (`CJ_AUTO_SUBMIT_ON_PAID` default true) rather than **MANUAL_APPROVAL by default**. This increases financial risk if costs/stock/mapping/shipping are not revalidated.
- **Price/amount trust boundary:** order totals are computed server-side from current variant prices at checkout/order creation (good), but **cart item unit price snapshot is stored/used inconsistently** and there is limited enforcement that captured PayPal amount/currency exactly matches the order total in all paths; frontend still initiates flows (must never be trusted for success/amounts).
- **Pre-fulfillment revalidation gaps:** before CJ submission, the system does not comprehensively recheck current supplier stock, supplier price changes/thresholds, shipping availability/cost limits, destination support, or expected-vs-actual cost deltas as specified.
- **State modeling:** single `order.status` string mixes concerns; payment/fulfillment states and history are not fully separated/persisted (e.g. CJ/shipments inferred in places), and transitions are not enforced by a strict state machine.
- **Tracking integrity:** timeline/ETA in UI includes derived estimates; must ensure only **verified** statuses/timestamps from order/carrier/CJ data are presented as factual (no invented tracking states).
- **Financial correctness:** profit logic is estimate-based from variant cost fields; no complete per-order actual cost/fee/refund/conversion model, so revenue/profit distinctions required by spec are not yet reliable.
- **Admin order detail exposure:** admin detail returns raw order/entity data including internal fields/snapshots in some responses—acceptable for admin, but must remain strictly protected and carefully shaped.

---

## 6. Duplicate order/payment/fulfillment risks

- **Order creation:** retry on order-number collision exists, but concurrent double-click/refresh/retry from client can still create multiple orders if requests pass validation independently (no checkout/order idempotency key tied to cart/client intent).
- **Cart conversion:** cart marked `CONVERTED` during order creation without a strong guard preventing multiple conversions/orders from the same active cart state under concurrency.
- **PayPal create/capture:** server creates/reuses payment records partially, but lacks explicit idempotency keys and strong compare-and-set guards across repeated create/capture calls, client retries, and timeout/retry scenarios.
- **Webhooks:** event-id dedupe is implemented (good), but processing must remain safe under at-least-once delivery, out-of-order events, and partial failures; payload hash/recording and linkage can be strengthened.
- **CJ submission (critical):** idempotent if `cjOrderId` already set and submitted via current path, but **race conditions/timeouts** (submit request succeeds at CJ but response handling fails/retries) can risk duplicate supplier actions without a stronger idempotency/outbox/submission-state guard and reconciliation.
- **Admin/manual retries:** retry flows must guarantee no duplicate CJ purchases/refunds; not fully enforced for all cases.
- **Refunds:** duplicate refund prevention and refund state tracking are not fully implemented.

---

## 7. Financial loss risks

- **Auto-fulfillment without full gates:** submitting to CJ without mandatory manual approval and without revalidating stock, supplier price deltas, shipping cost/availability, destination, or fraud/review flags can cause unintended spend or unprofitable fulfillment.
- **Supplier price/shipping variance:** CJ costs/shipping can change or vary by route/warehouse/method/weight; current model does not lock/verify expected vs actual costs before spend.
- **Insufficient cost visibility:** profit shown as estimate; missing supplier/fulfillment/fee/refund/conversion data can hide losses.
- **Payment/amount mismatch risk:** if currency/amount checks are not enforced everywhere (create/capture/webhook/reconcile), under/over-capture or reconciliation errors are possible.
- **No reserve/hold for exceptions:** out-of-stock, address/shipping failures, or post-payment fulfillment failures require controlled manual review and clear financial handling to avoid losses/chargebacks.
- **Disputes/returns/refunds:** lack of implemented refund/return/dispute workflows increases operational and financial exposure.

---

## 8. Security vulnerabilities / gaps

- **Object-level access control:** customer order endpoints are scoped by authenticated user (good), but any “track by reference”/public or loosely protected access must be cryptographically verified; ensure no leakage via predictable identifiers or admin-only data in responses.
- **Webhook trust & robustness:** PayPal signature verification requires `PAYPAL_WEBHOOK_ID` (correct) and CJ uses HMAC with openId/secret (accepts if unverifiable in some cases per implementation) — CJ verification/handling must be hardened per official behavior and fail-safe policy; all webhooks must be authenticated, idempotent, and rate/size controlled.
- **CSRF/CORS/session model:** CSRF disabled with stateless JWT (acceptable for bearer-token APIs) but CORS must be tightly restricted to trusted origins in production; ensure no credential leakage.
- **Authorization coverage:** `/api/v1/admin/**` restricted to ADMIN/STAFF; must verify all sensitive actions (refunds, fulfillment, exports, notes, cost data) are protected and audited.
- **Input validation & abuse protection:** Bean Validation present in places, but system-wide strict validation, rate limiting/throttling (login, checkout, webhooks, tracking), payload size limits, and sanitization/error handling need strengthening.
- **Secrets management:** secrets must be kept in env/secret managers only; credentials previously exposed in chat/logs require rotation (operational).
- **Sensitive data exposure:** ensure admin-only/internal fields (supplier costs, internal notes, raw snapshots, CJ errors/details) never leak to customer/public APIs or logs; redact payment/provider responses appropriately.
- **Auditability:** comprehensive audit logs for admin actions, payment events, fulfillment submissions, refunds, and status changes are not fully implemented.

---

## 9. Architecture & data observations

- **Stack matches:** React/Vite/Tailwind frontend, Spring Boot/Java 17/Maven/Spring Security/JPA/Flyway backend, PostgreSQL in current deployment/migrations (note: requirement text states MySQL—codebase/migrations are PostgreSQL-oriented; any change must be deliberate and safe).
- **Order/fulfillment fields split:** core order/payment/shipment tables exist plus CJ columns on orders/shipments and supplier tables; however required normalized entities (supplier order, fulfillment, tracking events, status history, costs, refunds/returns, notifications, audit) are not fully modeled.
- **`supplier_products` mapping exists in schema** but a complete CJ variant/warehouse/shipping-method mapping and enforcement flow is not evident end-to-end.
- **No email/notification/return/refund controllers, entities, or services found** in the audited packages.

---

## 10. Implementation priorities (recommended order)

1. **Safety-critical correctness (Phase 2–4):** introduce/solidify state model (separate payment/order/fulfillment + history), strict idempotency (checkout/order/payment/CJ/refunds), server-side amount/currency verification, and **manual-approval-default fulfillment** with full pre-submit gates and no unintended spend.
2. **CJ fulfillment reliability (Phase 6–8):** complete CJ product/variant/warehouse/shipping mapping, supplier-order/fulfillment records, revalidation (stock/price/shipping/cost thresholds), safe retry/outbox behavior, and explicit CJ payment/confirmation handling per official capabilities.
3. **Tracking & customer experience (Phase 5, 9):** verified-only timeline/tracking events, secure order access, and notification pipeline (with dedupe/retry) for all key lifecycle events.
4. **Financials, refunds & admin ops (Phase 10):** per-order cost breakdown (fees/refunds/supplier actuals/conversions), accurate profit metrics, returns/refunds/disputes workflows, admin review/exception queues, and exports.
5. **Security & hardening (throughout, Phase 12):** rate limiting, comprehensive validation, audit logging, tightened webhook verification/policies, least-privilege checks, and secret rotation.
6. **Testing & documentation (Phase 11–12):** scenario tests with mocked CJ/PayPal Sandbox, idempotency/concurrency/security cases, and final implementation/report docs.

---

## 11. Notes / constraints

- Do **not** fulfill unpaid orders, expose secrets, trust frontend prices/payment success, auto-spend on CJ without verified gates, or drop/reset existing data/migrations.
- CJ/PayPal/Render behaviors depend on external configuration and official API constraints; some capabilities (e.g. CJ order payment/confirmation via API) must follow documented, supported operations and be surfaced to admin when manual action is required.
- Current live deployment state and external account settings must be verified alongside code changes before considering the system production-reliable.
