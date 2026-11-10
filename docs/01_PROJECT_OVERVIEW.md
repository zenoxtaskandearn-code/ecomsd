# 01 — Project Overview

## Document status
- **Purpose:** Define the business concept, goals, scope, and direction for the Fetch ecommerce platform.
- **Audience:** Product, design, engineering, and future operators.
- **Status:** Planning (pre-implementation). No code started.
- **Last updated:** 2026-11-09

## Reference image note
The user designated an attached screenshot as the **primary visual design reference**. The image file was not available in the local workspace at planning time (`C:\Users\amitr\OneDrive\Desktop\Dropshoting` was empty). This plan therefore treats the **user’s detailed visual description** as the authoritative reference: premium pet lifestyle brand, soft lavender hero background, warm beige/cream product tones, natural elegant presentation, minimal clean typography, calm modern editorial aesthetic, organic/flowing shapes, large product photography, soft contrast, left-aligned hero text with dog image on the right and organic white shape behind the dog, rounded dark CTA, thin announcement bar, minimal navbar (logo left, links center, search/cart right), and light neutral collection background with clean product cards.

**Action before design sign-off:** Place the reference image in the repository (e.g., `docs/reference/`) and validate `03_DESIGN_SYSTEM.md` and `04_UI_UX_PLAN.md` against it.

## Business idea
**Fetch** (temporary, configurable brand name) is a premium, international dropshipping ecommerce storefront for dog accessories and pet lifestyle products. The brand should feel like an independent, established pet lifestyle company — editorial, calm, trustworthy, and product-focused — not a generic dropshipping template.

## Target audience
| Segment | Description | Needs |
|---|---|---|
| Primary | US-based dog owners shopping for premium accessories | Quality perception, clear sizing/material info, trustworthy checkout, shipping transparency |
| Secondary | Gift buyers in the US | Easy discovery, giftable presentation, clear delivery expectations |
| Future | Other English-speaking markets (post-MVP) | Localized shipping/currency/tax — out of MVP scope |

## Business model
- **Model:** International dropshipping ecommerce (B2C).
- **Operator:** Based in India.
- **Primary market:** United States.
- **Fulfillment:** Supplier ships directly to the customer. **No physical warehouse/inventory held by operator initially.**
- **Catalog:** Premium dog accessories and pet lifestyle products (harnesses, collars, leashes, beds, travel accessories, grooming/lifestyle items as curated).
- **Revenue:** Product margin between supplier cost/shipping and retail price, minus payment fees, refunds, chargebacks, and marketing costs.

## Core features (planned)
1. **Storefront:** Home, shop/collections, product detail, search, content pages (About, Contact, Blog placeholder strategy), policy pages.
2. **Commerce:** Product variants, cart, guest-friendly checkout path (policy decision — MVP may require account or support guest; documented as open decision), PayPal checkout.
3. **Customers:** Registration, login, account profile, addresses, order history, order tracking view.
4. **Orders:** Order lifecycle from pending payment to delivered/cancelled/refunded; supplier fulfillment coordination.
5. **Payments:** PayPal Orders API integration, webhook verification, sandbox-first testing.
6. **Admin:** Dashboard for products, categories, orders, customers (limited), supplier mappings, fulfillment status, refunds (operational), basic reporting.
7. **Operations:** Shipping/tracking updates (supplier-driven or manual fallback), audit logging, error handling.

## Project goals
- Launch a **credible, premium MVP** that can accept real customer orders in the US after accounts/APIs are verified.
- Establish a **maintainable architecture** (React + Spring Boot + MySQL) that can grow without rewrites.
- Implement **secure payment and order handling** (no frontend-only payment trust).
- Support **supplier fulfillment realities** (delays, partial tracking, API limitations) with manual fallbacks.
- Keep branding, policies, and integrations **configurable**.

## MVP scope
### In scope (MVP target)
- Responsive marketing storefront aligned to the reference visual language.
- Configurable brand name/content foundations (Fetch placeholder).
- Product catalog with categories, images, variants, pricing display, and availability messaging based on supplier snapshots/manual status.
- Search and basic filtering (category, price range, key attributes as feasible).
- Cart (persistent for authenticated users; guest cart strategy defined in requirements).
- Checkout with address capture, shipping method placeholder/rules as configured, and PayPal sandbox flow.
- Customer accounts: register/login, profile, addresses, order history.
- Order management: create pending order, capture/verify payment, paid/processing/fulfilled/shipped/delivered/cancelled/refunded states (subset enforced at MVP).
- Admin: secure login, product/category CRUD (or controlled import workflow), order list/detail, fulfillment/tracking entry, basic status updates.
- Policy pages: Shipping, Returns/Refunds, Privacy, Terms, FAQ, Contact.
- Observability basics: application logs, payment/webhook audit records.

### Out of scope (MVP, planned later)
- Multi-currency, multi-language, and international tax automation beyond documented placeholders.
- Real-time supplier inventory/pricing sync unless a verified supplier API supports it reliably.
- Subscriptions, bundles, loyalty programs, advanced promotions engine (simple coupon structure may be stubbed/designed).
- Native mobile apps.
- Marketplace integrations (Amazon, Etsy, etc.).
- Advanced fraud scoring, 3PL warehouse management, split shipments across multiple suppliers (design-aware but not fully automated).
- User-generated reviews publishing workflow in production (schema may exist; **no fake reviews/testimonials**).

## Future expansion
- Verified supplier API integrations (import, sync, order submission, tracking webhooks/polling).
- Multi-supplier routing and split fulfillment.
- Enhanced analytics (GA4/ads pixels), email marketing, abandoned cart flows.
- CMS/blog authoring.
- Reviews with moderation (real customers only).
- International expansion, localized pricing/shipping/tax.
- Returns portal and RMA workflow.
- Performance optimizations (CDN, image pipeline, caching).

## Success criteria (planning-level)
- Documentation is complete, consistent, and implementation-ready after approval.
- MVP can be built in phases without architectural rewrites.
- Payment and fulfillment flows have explicit failure handling.
- Design direction is premium, human, and consistent with the reference description.

## Key assumptions
- PayPal business/sandbox credentials will be obtained and configured via environment secrets; **production approval is not assumed**.
- Supplier capabilities vary; integrations will be designed behind interfaces with manual fallbacks.
- US shipping times/costs originate from supplier constraints and must be communicated clearly.
- The operator handles customer support, disputes, and policy compliance.

## Related documents
- `02_REQUIREMENTS.md`
- `03_DESIGN_SYSTEM.md`
- `04_UI_UX_PLAN.md`
- `05_SYSTEM_ARCHITECTURE.md`
- `06_DATABASE_DESIGN.md`
- `07_API_SPECIFICATION.md`
- `08_PAYMENT_INTEGRATION.md`
- `09_SUPPLIER_INTEGRATION.md`
- `10_SECURITY_PLAN.md`
- `11_TESTING_PLAN.md`
- `12_DEVELOPMENT_ROADMAP.md`
