# 13 — Backend Implementation Plan

## 1. Status
- **Purpose:** Concrete, implementation-ready backend plan for Fetch (Spring Boot + Maven + MySQL), aligned with existing docs (`05`–`12`) and the frontend already built in `frontend/`.
- **Scope of this document:** Planning only. **No backend code, no migrations, no dependency installation, no infrastructure changes** created by this plan.
- **Prerequisite:** Confirm key open decisions in Section 2 before Phase 2/4 implementation.

## 2. Decisions to confirm before implementation
| Decision | Recommended default | Impact |
|---|---|---|
| **Customer/admin auth** | **JWT access + refresh** (stateless API) with RBAC claims; admin uses same auth system with `ADMIN/STAFF` roles | CSRF approach differs from cookie sessions; refresh rotation/revocation strategy required |
| **Guest checkout (MVP)** | **Require account** for MVP (simpler cart/order ownership); design cart/order tables to allow guest later | Frontend checkout flow and `/orders` ownership rules |
| **US tax (MVP)** | **No automated tax calculation**; display clear “tax calculated/finalized per policy” messaging; make `tax_amount` configurable (0 for MVP) | Totals calculation and compliance messaging |
| **Returns workflow** | **Support-mediated** (contact + admin) for MVP | No RMA portal yet |
| **Currency** | **USD only** for MVP | `currency=USD`, money formatting/validation |
| **Migrations tool** | **Flyway** (simple SQL migrations, good Spring Boot fit) | `db/migration/V*__*.sql` |
| **API versioning** | **`/api/v1`** prefix for all REST endpoints | Frontend Axios base URL + route contracts |
| **CORS (local dev)** | Allow `http://localhost:5173` (and configured frontend origins) with credentials only if needed | Security config |
| **Product model (MVP)** | Catalog supports products + variants; shop UI can start with single-variant products mapped cleanly | Entities/DTOs and frontend PDP/cart integration |
| **PayPal mode** | **Sandbox first**; all secrets in env/secret manager; production disabled until approved | Config profiles and webhook setup |

**Owner action:** Reply with approvals/changes to these defaults. If unspecified, implementation should follow the recommended defaults and record deviations.

## 3. Proposed backend stack
- **Language/runtime:** Java 17 or 21 (LTS; choose 21 if team/tooling supports).
- **Framework:** Spring Boot 3.x (current stable at implementation time), Maven.
- **Core dependencies (planned):** `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`, `mysql-connector-j`, `flyway-core` (+ MySQL support as required), `spring-boot-starter-actuator` (health/ops), JWT library (e.g., JJWT or Nimbus; pick one and standardize), `spring-boot-starter-test`.
- **Optional/later:** MapStruct (if mapping becomes noisy), Bucket4j/Redis rate limiting, Spring Mail, OpenAPI/springdoc.
- **Constraints:** No PayPal secrets in frontend; no frontend-trusted payment state; no TypeScript in frontend; no fake supplier API assumptions.

## 4. Repository/module layout (planned)
```text
backend/
  pom.xml
  .env.example                    # names only, no secrets
  src/main/java/com/fetch/backend/
    FetchBackendApplication.java
    config/                       # CORS, Jackson, Flyway/JPA, PayPal/properties
    security/                     # SecurityFilterChain, JWT filter, user details, RBAC
    common/                       # ApiError, exceptions, correlation ID, pagination, BaseEntity
    auth/
    user/
    catalog/                      # categories, products, variants, images
    cart/
    order/
    payment/
    fulfillment/                  # suppliers, mappings, manual/adapter workflow
    webhook/
    admin/                        # admin-specific controllers/DTOs if separated
  src/main/resources/
    application.yml
    application-local.yml
    application-prod.yml
    db/migration/                 # Flyway SQL (created in Phase 5 implementation)
  src/test/java/...               # unit/integration/security tests
```

**Layering rule:** `controller -> service -> repository`; controllers use request/response DTOs; services enforce business rules and transactions; repositories expose persistence; integrations (PayPal/supplier) behind interfaces.

## 5. Configuration and environments
- **Profiles:** `local`, `test`, `prod` (names flexible; keep secrets external).
- **Required env/config keys (examples; no values committed):** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (or equivalent), `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`, `CORS_ALLOWED_ORIGINS`, `PAYPAL_BASE_URL`, `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_WEBHOOK_ID`, `APP_BASE_URL`.
- **Error contract:** consistent JSON envelope, e.g. `{"timestamp","path","correlationId","error":{"code","message","details":[...]}}`.
- **Actuator:** expose `health` (and minimal info) appropriately per environment; never expose secrets.

## 6. Database/migration plan (Phase 5 implementation order)
Implement **Flyway** migrations in dependency order. Proposed sequence (adjust during implementation if needed):

| Migration | Tables / purpose |
|---|---|
| `V1__users_addresses.sql` | `users`, `addresses`; indexes/uniques (`users.email`) |
| `V2__catalog.sql` | `categories`, `products`, `product_variants`, `product_images`; slugs/SKUs/PK-FK/indexes |
| `V3__suppliers.sql` | `suppliers`, `supplier_products` (manual-first; `api_mode`) |
| `V4__carts.sql` | `carts`, `cart_items`; uniqueness `(cart_id, product_variant_id)` |
| `V5__orders.sql` | `orders`, `order_items` with **snapshot columns**; order number/status/date indexes |
| `V6__payments_shipments_webhooks.sql` | `payments`, `shipments`, `webhook_events` (unique provider event id, idempotency fields) |
| `V7__seed_dev_optional.sql` | **Local/dev only** seed data (never production secrets); optional |

**Rules:** InnoDB/`utf8mb4`; `DECIMAL(12,2)` money; UTC timestamps; app-level enums; no cascade deletes that destroy commerce history; snapshots immutable after purchase.

## 7. Module-by-module implementation plan

### 7.1 Foundation (Phases 2, 4)
- Maven project, base package, `application*.yml`, correlation ID filter, global exception handler, validation error mapping.
- Security skeleton: public routes (`/api/v1/auth/**`, catalog reads, contact/content, PayPal webhook with verification), authenticated customer routes, `/api/v1/admin/**` role-guarded.
- CORS for frontend origins; password encoder (BCrypt).

### 7.2 Auth & users (Phase 6)
- `POST /api/v1/auth/register|login|refresh|logout` (per JWT decision), `GET/PATCH /api/v1/users/me`.
- `users` + optional refresh-token storage strategy (DB table or stateless+rotation approach — choose and document).
- Frontend integration: replace demo `AuthContext` flows with real API calls; store tokens securely per chosen strategy; handle 401 refresh/logout.

### 7.3 Catalog (Phase 7)
- Public: categories, product list/search/filter, product detail by slug (include variants/images).
- Admin: CRUD for categories/products/variants/images (image upload/storage can start as URL metadata; file storage integration later).
- Match frontend needs: `ShopPage` filters/sort, `CollectionPage`, `SearchPage`, `ProductDetailsPage`, marquee/home data.

### 7.4 Cart & checkout pre-payment (Phase 8)
- Customer cart endpoints; server recalculates prices/totals from DB.
- Checkout preview/validation; create **pending internal order** with `order_items` snapshots and address snapshots before PayPal capture.
- **Never** trust frontend totals.

### 7.5 PayPal (Phase 9)
- Backend service/client: create PayPal order, capture/verify, map IDs to internal `orders`/`payments`.
- `POST /api/v1/webhooks/paypal`: verify signature, persist/check `webhook_events.event_id` (idempotent), reconcile state.
- Frontend integration: PayPal JS SDK/buttons use approved client-side config only; confirmation based on backend capture result.

### 7.6 Orders, tracking, admin ops (Phases 10–11)
- Customer order history/detail/tracking (ownership checks).
- Admin order list/detail, guarded status transitions, shipment/tracking CRUD, fulfillment queue.
- Supplier adapter interface + **manual fulfillment** default; audit notes/status.

### 7.7 Hardening (Phases 12–13)
- Rate limiting (auth/contact/checkout), pagination caps, PII-safe logging, dependency/security scan, test coverage for payment/webhook/order state machine.

## 8. Frontend integration contract (current UI -> future API)
| Frontend area (current) | Backend contract needed |
|---|---|
| `AuthContext` demo | `/auth/*`, `/users/me`, token handling, route guards on 401/403 |
| `catalog.js` mock | `/categories`, `/products`, `/products/{slug}` (+ search/filter query params) |
| `CartContext` local | `/cart`, `/cart/items` (or guest strategy decision) |
| `CheckoutPage` demo | `/checkout/preview`, `/orders`, `/payments/paypal/create|capture` |
| `Account*` demo data | `/orders`, `/orders/{orderNumber}`, `/users/me/addresses` |
| Admin demo pages | `/api/v1/admin/*` for dashboard stats, catalog CRUD, orders/fulfillment, customers, suppliers |

**Integration approach:** add `frontend/src/services/apiClient.js` (Axios + base URL/interceptors), then migrate contexts/pages incrementally; keep UI states (loading/error/empty/button loading) already built.

## 9. Testing plan (backend)
- **Unit:** pricing/totals, order/payment state guards, mappers/validators.
- **Integration:** `@SpringBootTest`/MockMvc + Testcontainers MySQL for auth/catalog/cart/order flows.
- **Security:** unauthenticated/forbidden/object-level access tests; admin RBAC.
- **Payment/webhook:** mocked PayPal client + idempotency/duplicate event/signature-failure cases.
- **CI (later):** build, tests, migration validation, dependency scan.

## 10. Recommended execution order
1. **Confirm Section 2 decisions** (auth, guest checkout, tax, migration tool, Java/Spring versions).
2. **Phase 2:** scaffold `backend/` Maven project + config/env templates (no secrets).
3. **Phase 4:** security/CORS/error/validation foundation + health endpoint.
4. **Phase 5:** Flyway migrations V1–V6 + local seed strategy.
5. **Phase 6:** auth/users + frontend auth integration.
6. **Phase 7:** catalog APIs + frontend catalog integration.
7. **Phase 8:** cart/checkout/order snapshots.
8. **Phase 9:** PayPal sandbox + webhooks (server truth).
9. **Phase 10–11:** orders/tracking/admin fulfillment (manual-first).
10. **Phase 13+:** hardening, deployment, production readiness.

## 11. Out of scope for first backend implementation
- Real supplier API automation before a verified supplier exists.
- Automated tax engine, multi-currency, multi-language.
- Full RMA/returns portal.
- Reviews moderation system with user-generated content (and **never fake reviews**).
- Production PayPal go-live before credentials/approval and operational readiness.

## 12. Deliverable checklist for backend planning approval
- [ ] Section 2 decisions confirmed
- [ ] Package/module structure approved
- [ ] Migration sequence approved
- [ ] Auth strategy (JWT + refresh details) approved
- [ ] PayPal sandbox/webhook approach approved
- [ ] Frontend integration order approved

**Next step after approval:** start **Phase 2/4 backend scaffolding** (create `backend/` project, Maven config, profiles, security/error foundation) — still with **no PayPal live secrets**, **no fake payment success**, and **no unverified supplier API assumptions**.
