# 12 — Development Roadmap

## Overview
Phased, dependency-aware plan for Fetch. **Documentation (Phase 0) is complete in this deliverable; implementation begins only after approval.** Phases may overlap slightly but must respect dependencies (especially payments before paid fulfillment).

## Phase 0 — Requirements and planning (COMPLETE — this delivery)
- **Objectives:** Analyze reference direction, define scope, architecture, DB, APIs, risks.
- **Tasks:** Create `docs/01`–`docs/12`, diagrams, open questions.
- **Dependencies:** None.
- **Expected outputs:** `docs/*.md`.
- **Acceptance criteria:** All required documents exist; no code generated; contradictions reviewed; open decisions listed.
- **Testing:** Document review (consistency, completeness).

## Phase 1 — Visual design validation and design system
- **Objectives:** Validate tokens against the actual reference image; finalize typography, components, imagery rules.
- **Tasks:** Add reference image to repo; produce Figma/mockups or high-fidelity component specs; confirm hero composition and responsive behavior.
- **Dependencies:** Phase 0 approval; access to reference image.
- **Files/modules expected:** `docs/reference/*`, optional `design/` or Figma links in docs.
- **Acceptance criteria:** Approved palette/type/spacing; key screens (home, PLP, PDP, cart, checkout, admin table/form) specified.
- **Testing:** Accessibility contrast checks; stakeholder review.

## Phase 2 — Project setup and architecture decisions
- **Objectives:** Initialize repos/monorepo structure, tooling, env conventions, auth mechanism decision.
- **Tasks:** Create React app (JS/JSX), Tailwind, Router, TanStack Query, Axios; create Spring Boot Maven app, packages/layers, security skeleton, config profiles; decide JWT vs session; add `.env.example` patterns (no secrets).
- **Dependencies:** Phase 1 direction helpful; Phase 0 required.
- **Files/modules:** `frontend/`, `backend/`, `docs/` updates.
- **Acceptance criteria:** Apps run locally; lint/format baseline; health endpoint; CORS configured for local dev.
- **Testing:** Smoke tests, CI skeleton.

## Phase 3 — Frontend foundation
- **Objectives:** App shell matching design system.
- **Tasks:** Layouts, AnnouncementBar, Navbar, Footer, routing, theme tokens, shared UI primitives (buttons, inputs, states).
- **Dependencies:** Phase 2.
- **Files/modules:** `frontend/src/components/layout/*`, `pages/*` placeholders, `styles/theme`.
- **Acceptance criteria:** Responsive shell consistent with reference; accessible navigation.
- **Testing:** Component/snapshot/manual responsive checks.

## Phase 4 — Backend foundation
- **Objectives:** API skeleton, security baseline, error handling.
- **Tasks:** Package structure, validation, exception handler, logging/correlation IDs, base entities/DTO conventions, OpenAPI stub optional.
- **Dependencies:** Phase 2.
- **Files/modules:** `backend/src/main/java/.../config,security,common,controller`.
- **Acceptance criteria:** Versioned `/api/v1` responds; standardized errors; auth plumbing stubbed.
- **Testing:** MockMvc security/validation tests.

## Phase 5 — Database implementation
- **Objectives:** Implement MVP schema with migrations.
- **Tasks:** Add Flyway/Liquibase; create tables for users, catalog, cart, orders, payments, shipments/tracking, webhook events, suppliers/mappings (MVP set); seed dev data.
- **Dependencies:** Phase 4; DB design finalized.
- **Files/modules:** `backend/src/main/resources/db/migration/*`, JPA entities/repositories.
- **Acceptance criteria:** Migrations run cleanly; constraints/indexes in place.
- **Testing:** Repository/integration tests with Testcontainers.

## Phase 6 — Authentication and customer accounts
- **Objectives:** Secure customer auth and profile/address management.
- **Tasks:** Register/login/logout/refresh (per decision), password hashing, `/users/me`, addresses CRUD; frontend auth flows/guards.
- **Dependencies:** Phases 3–5.
- **Files/modules:** auth controllers/services, security config, account pages.
- **Acceptance criteria:** Users can register/login; protected routes enforced; no secret leakage.
- **Testing:** Auth integration tests, rate-limit plan verification.

## Phase 7 — Product catalog
- **Objectives:** Browsable catalog with variants.
- **Tasks:** Categories/products/variants/images APIs; admin CRUD (basic); storefront home/shop/collection/PDP/search integration.
- **Dependencies:** Phases 3–6.
- **Files/modules:** catalog module + storefront pages/components.
- **Acceptance criteria:** PDP shows variants/images; search/filter work; admin can manage products.
- **Testing:** API + UI tests; data validation.

## Phase 8 — Cart and checkout (pre-payment)
- **Objectives:** Server-validated cart and checkout draft.
- **Tasks:** Cart APIs/UI, checkout preview, address capture, order snapshot creation (pending), totals calculation.
- **Dependencies:** Phase 7.
- **Files/modules:** cart/order services, checkout page.
- **Acceptance criteria:** Totals computed server-side; unavailable items blocked; pending order created safely.
- **Testing:** Total calculation and edge-case tests.

## Phase 9 — PayPal sandbox integration
- **Objectives:** End-to-end sandbox payment.
- **Tasks:** PayPal client config (env), create/capture endpoints, frontend approval flow, webhook endpoint with verification + idempotency, payment records.
- **Dependencies:** Phase 8.
- **Files/modules:** `payment` module, webhook controller, checkout integration.
- **Acceptance criteria:** Sandbox purchase marks order paid only after server verification; webhooks deduped.
- **Testing:** Full sandbox matrix (Section 11.6 in testing plan).

## Phase 10 — Order management and customer tracking
- **Objectives:** Complete order lifecycle visibility.
- **Tasks:** Customer order history/detail, status timeline, confirmation emails hooks (when provider ready), admin order views/transitions, cancellations/refund rules.
- **Dependencies:** Phase 9.
- **Files/modules:** order/admin pages, shipment records.
- **Acceptance criteria:** Customers see accurate status; admins can manage orders with guards.
- **Testing:** State machine and RBAC tests.

## Phase 11 — Supplier integration and fulfillment
- **Objectives:** Manual-first fulfillment with adapter design.
- **Tasks:** Fulfillment queue, supplier/mapping admin, manual submit workflow, tracking entry, optional CSV/import helpers; adapter interfaces for future APIs.
- **Dependencies:** Phase 10.
- **Files/modules:** `fulfillment` module, admin supplier/fulfillment UI.
- **Acceptance criteria:** Paid orders can be fulfilled manually end-to-end with tracking; no auto-submit without rules.
- **Testing:** Workflow tests; mocked adapter tests.

## Phase 12 — Admin dashboard polish
- **Objectives:** Operational usability.
- **Tasks:** Dashboard summary metrics, filters/search, notes/audit visibility, settings for configurable content.
- **Dependencies:** Phases 6–11.
- **Files/modules:** admin routes/components.
- **Acceptance criteria:** Admin can run daily ops (catalog, orders, fulfillment) efficiently.
- **Testing:** Admin E2E smoke.

## Phase 13 — Testing, hardening, and security
- **Objectives:** Production-grade confidence.
- **Tasks:** Expand automated tests, security review, dependency scan, rate limiting, logging/alerts, accessibility fixes, performance pass (images/queries).
- **Dependencies:** Features complete enough to test.
- **Files/modules:** CI pipelines, test suites, configs.
- **Acceptance criteria:** Quality gates met (see `11_TESTING_PLAN.md`).
- **Testing:** Regression + sandbox E2E.

## Phase 14 — Deployment
- **Objectives:** Staging then production infrastructure.
- **Tasks:** Provision DB/hosting, domains/HTTPS, env secrets, CI/CD, backups, monitoring; deploy staging with sandbox.
- **Dependencies:** Phase 13.
- **Files/modules:** infra manifests/scripts (as chosen), runbooks in docs.
- **Acceptance criteria:** Staging mirrors prod topology; rollback documented.
- **Testing:** Smoke tests on staging.

## Phase 15 — Production readiness
- **Objectives:** Safe go-live.
- **Tasks:** PayPal live credentials/webhooks (when approved), legal/policy final review, support processes, supplier SOPs, incident runbooks, analytics consent plan.
- **Dependencies:** Phase 14; external approvals.
- **Files/modules:** ops docs/checklists.
- **Acceptance criteria:** Go-live checklist signed off; live transactions verified in controlled manner.
- **Testing:** Live smoke with minimal-risk order/refund test per policy.

## Phase 16 — Marketing and analytics (post-launch)
- **Objectives:** Growth instrumentation.
- **Tasks:** GA4/pixels (consent-aware), SEO refinements, email flows, content/blog CMS, A/B tests — only with real data/practices.
- **Dependencies:** Phase 15 stable.
- **Acceptance criteria:** Measurable, compliant tracking; no dark patterns.

## Dependency rationale (summary)
- **Setup → foundation → DB → auth → catalog → cart/checkout → PayPal → orders → fulfillment:** ensures data and security exist before money moves, and money is verified before supplier action.
- **Admin** spans phases but hardens after core flows exist.
- **Deployment/readiness** follow testing and security to reduce launch risk.
