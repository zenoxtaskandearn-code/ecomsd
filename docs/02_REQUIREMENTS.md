# 02 — Requirements

## 1. Functional requirements

### 1.1 Storefront (customer-facing)
| ID | Requirement | Priority |
|---|---|---|
| FR-SF-01 | Display responsive homepage with hero, featured collections/products, brand story/value props, and footer. | MVP |
| FR-SF-02 | List products by collection/category with pagination or progressive loading. | MVP |
| FR-SF-03 | Provide product detail page with gallery, description, materials/care (when available), variant selection, price, availability messaging, and add-to-cart. | MVP |
| FR-SF-04 | Provide search by product name/SKU/tags (backend-backed). | MVP |
| FR-SF-05 | Provide basic filters (category, price band, variant attributes where modeled). | MVP |
| FR-SF-06 | Show cart with line items, quantities, subtotal, and clear shipping/tax messaging (estimated where applicable). | MVP |
| FR-SF-07 | Support checkout flow: contact/shipping info, order review, PayPal approval/capture via backend. | MVP |
| FR-SF-08 | Show order confirmation with order reference and next steps (no sensitive payment data). | MVP |
| FR-SF-09 | Provide policy/content pages: About, Contact, FAQ, Shipping, Returns/Refunds, Privacy, Terms. | MVP |
| FR-SF-10 | Blog: MVP may be static/placeholder content structure; full CMS later. | Post-MVP |

### 1.2 Customer accounts
| ID | Requirement | Priority |
|---|---|---|
| FR-AC-01 | Register with email/password; validate input; verify email optional for MVP but recommended post-MVP. | MVP |
| FR-AC-02 | Login/logout; secure session/token handling. | MVP |
| FR-AC-03 | Manage profile and saved addresses. | MVP |
| FR-AC-04 | View order history and order detail/tracking status. | MVP |
| FR-AC-05 | Password reset flow (email delivery depends on configured provider; design required). | MVP/Post-MVP detail |

### 1.3 Cart and checkout
| ID | Requirement | Priority |
|---|---|---|
| FR-CT-01 | Add/update/remove cart items; validate variant and quantity. | MVP |
| FR-CT-02 | Recalculate totals server-side before payment creation. | MVP |
| FR-CT-03 | Create a pending order record before/at PayPal order creation with idempotency safeguards. | MVP |
| FR-CT-04 | Prevent checkout of unavailable or invalid items based on latest known status. | MVP |
| FR-CT-05 | Support guest checkout only if explicitly approved; otherwise require login (open decision). | Decision |

### 1.4 Payments (PayPal)
| ID | Requirement | Priority |
|---|---|---|
| FR-PY-01 | Create PayPal order server-side for an internal pending order. | MVP |
| FR-PY-02 | Capture/verify payment server-side; never mark paid from frontend alone. | MVP |
| FR-PY-03 | Handle PayPal webhooks with signature verification and idempotent processing. | MVP |
| FR-PY-04 | Record payment attempts, statuses, PayPal IDs, and raw event references safely. | MVP |
| FR-PY-05 | Support refund workflows (admin-initiated; PayPal API when available). | MVP/Post-MVP ops |

### 1.5 Orders and fulfillment
| ID | Requirement | Priority |
|---|---|---|
| FR-OR-01 | Maintain order states: pending_payment, payment_failed, paid, processing, fulfillment_submitted, shipped, delivered, cancelled, refunded (exact enum finalized in implementation). | MVP |
| FR-OR-02 | Store immutable order snapshots (items, prices, shipping address) at purchase time. | MVP |
| FR-OR-03 | Support supplier fulfillment submission via integration adapter or manual admin workflow. | MVP |
| FR-OR-04 | Store and display tracking number/carrier/status updates when available. | MVP |
| FR-OR-05 | Handle cancellations/refunds according to policy and payment state. | MVP |

### 1.6 Admin
| ID | Requirement | Priority |
|---|---|---|
| FR-AD-01 | Role-based admin access (admin/staff roles as needed). | MVP |
| FR-AD-02 | Manage products, variants, images, categories, and visibility. | MVP |
| FR-AD-03 | View/search orders; update fulfillment/tracking; add internal notes. | MVP |
| FR-AD-04 | View customer list/order context with privacy-appropriate access. | MVP |
| FR-AD-05 | View supplier mappings and fulfillment queue/status. | MVP |
| FR-AD-06 | Basic dashboard metrics (orders, revenue indicators, pending fulfillment) — derived carefully. | MVP |

## 2. Non-functional requirements
| ID | Requirement | Target |
|---|---|---|
| NFR-01 | **Performance:** Storefront pages load quickly on typical US mobile/desktop connections; optimize images and API payloads. | Core Web Vitals-aware MVP |
| NFR-02 | **Availability:** Handle deploys and transient failures gracefully; no single point of manual secret exposure. | Best-effort MVP |
| NFR-03 | **Scalability:** Stateless backend where practical; database indexed for catalog/orders queries. | Growth-ready |
| NFR-04 | **Maintainability:** Modular React and layered Spring Boot; clear boundaries; JavaScript (no TypeScript). | Mandatory |
| NFR-05 | **Accessibility:** Semantic HTML, keyboard navigation, focus states, contrast-checked palette, form labels/errors. | MVP baseline |
| NFR-06 | **Responsiveness:** Mobile-first behavior; layouts adapt to phone/tablet/desktop. | MVP |
| NFR-07 | **Observability:** Structured logs (no secrets/PII leakage), payment/webhook audit trail. | MVP |
| NFR-08 | **Configurability:** Brand name, contact info, feature flags, and integration endpoints via config/env. | MVP |

## 3. Customer requirements
- Clear, honest product information (materials, dimensions, variant differences) when data exists.
- Transparent shipping expectations for international dropshipping to the US.
- Simple, trustworthy checkout and recognizable payment (PayPal).
- Ability to track order status and contact support.
- Policies that match actual operations (returns, refunds, delivery windows).

## 4. Admin requirements
- Secure access separated from customer accounts.
- Efficient order processing and fulfillment visibility.
- Ability to correct data (tracking, status) with auditability.
- Safe workflows for refunds/cancellations aligned with PayPal state.

## 5. Security requirements
- Strong password hashing (e.g., BCrypt/Argon2 via Spring Security defaults where appropriate).
- Role-based authorization for admin APIs and UI routes.
- Input validation (Bean Validation + frontend validation).
- CORS restricted to known origins.
- CSRF strategy appropriate to auth mechanism (documented in security plan).
- Rate limiting on auth/sensitive endpoints (infrastructure/app-level plan).
- PayPal credentials and webhook secrets only in backend environment/secret store.
- Webhook signature verification and idempotency keys/event deduplication.
- PII minimization in logs; secure handling of addresses/contact data.

## 6. Business constraints
- India-based operator, US primary customers — shipping times, duties/taxes messaging, and support hours must be realistic.
- Dropshipping means limited control over packaging, speed, and stock accuracy.
- No initial warehouse inventory.
- Supplier and PayPal production capabilities/accounts may not yet be approved.

## 7. Assumptions
| ID | Assumption |
|---|---|
| A-01 | MVP uses USD pricing display for US customers; tax calculation may be simplified/manual until a tax engine is chosen. |
| A-02 | Shipping cost rules are configurable; real-time carrier rates may be deferred. |
| A-03 | Email/SMS providers (transactional notifications) will be selected later; interfaces designed now. |
| A-04 | Initial catalog size is manageable for standard MySQL indexing and admin CRUD. |
| A-05 | Product imagery will be authentic/brand-appropriate; **no fake testimonials/reviews**. |

## 8. Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Supplier stock/price mismatch | Overselling, refunds | High | Snapshots at order time, availability checks, manual review queue, clear policies |
| Long/variable shipping times | Customer dissatisfaction, disputes | High | Transparent delivery estimates, tracking updates, proactive communication design |
| PayPal disputes/chargebacks | Financial loss | Medium | Clear descriptors/policies, proof of fulfillment/tracking, audit trail |
| Webhook duplication/out-of-order events | Incorrect order state | Medium | Idempotency, event store, state machine guards |
| Frontend-trusted payment confirmation | Fraudulent “paid” orders | High if mishandled | Server-side capture/verification only |
| Scope creep (marketplace features in MVP) | Delay | Medium | Phased roadmap, strict MVP acceptance criteria |
| Generic AI-looking UI | Weak brand trust | Medium | Design system tied to reference, editorial photography rules, restrained components |

## 9. Open decisions (require product/owner input)
- Guest checkout vs. mandatory account for MVP.
- Tax handling at MVP (none, flat estimate, external provider later).
- Returns workflow: self-service portal vs. support-mediated.
- Whether to store card data never (PayPal-only) — **recommended: PayPal-only, no card vault in scope**.
- Supplier selection and which integration features are contractually/API-supported.
- Transactional email provider and domain authentication (SPF/DKIM).
