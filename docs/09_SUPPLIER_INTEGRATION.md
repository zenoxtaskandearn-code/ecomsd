# 09 — Supplier Integration (Dropshipping)

## 1. Purpose
Define how Fetch will coordinate product sourcing and order fulfillment with **international dropshipping suppliers** while acknowledging that **APIs, data quality, and capabilities vary** and may not be available at launch.

## 2. Guiding principles
- **Do not assume supplier API capabilities.** Build adapters and feature flags per supplier.
- **Manual fallback is a first-class workflow** for MVP.
- **Never submit supplier fulfillment before** internal order is paid (or explicitly authorized per business rule) and validated.
- Maintain **auditability** (who/what submitted fulfillment, when, response/errors).

## 3. Integration modes
| Mode | Description | MVP |
|---|---|---|
| Manual | Admin exports/shares order details; places supplier order manually; enters tracking/status in admin | Yes (default) |
| File/API-assisted | Periodic import/export (CSV) or limited API calls where verified | Possible post-MVP |
| Full API | Automated product sync, order submission, tracking updates/webhooks | Only if supplier supports and verified |

## 4. Product import
- **Goal:** Populate catalog with supplier-sourced items mapped to internal `products`/`product_variants`.
- **Methods:** manual admin creation, CSV import tool (post-MVP or Phase 11 light), or supplier API if available.
- **Data mapping:** supplier SKU → internal variant SKU; titles/descriptions rewritten for brand voice; images curated for premium/authentic look (**avoid low-quality or misleading supplier assets**).
- **Configurable fields:** cost, shipping origin hints, processing time notes (internal).

## 5. Product synchronization
- **Inventory/availability:** snapshots or periodic checks; if no API, manual status updates (`active`, `unavailable`, `backorder_note`).
- **Pricing:** supplier cost/retail sync only if reliable; otherwise manual repricing with margin rules documented externally.
- **Conflicts:** last-known data displayed with caution; checkout revalidates and may block purchase.

## 6. Supplier inventory and pricing
- Treat supplier stock as **best-effort information**.
- Store `supplier_products.cost_amount`, `last_synced_at`, and optional availability flags.
- Business process for handling sell-outs: cancel/refund, offer alternatives, or delayed fulfillment — aligned with policies.

## 7. Product variants
- Map variant options (size/color) explicitly; avoid ambiguous one-to-many mappings.
- Ensure internal variant SKU is stable and used in `order_items` snapshots.

## 8. Order submission to supplier
- Trigger: admin action or automated job **after** order is `paid` and passes fraud/business checks (manual review queue optional).
- Payload (conceptual): internal order ref, ship-to address, line items with supplier SKUs, shipping method notes.
- **Idempotency:** prevent duplicate submissions (submission ID/state machine).
- Record request/response summaries (redacted) in fulfillment logs/table extension.

## 9. Fulfillment status
Suggested states (subset for MVP): `not_submitted`, `submitted`, `supplier_confirmed` (if known), `shipped`, `delivered`, `exception/cancelled`. Map supplier statuses to internal states conservatively.

## 10. Tracking updates
- **API/webhook:** ingest tracking events if supplier supports; verify authenticity.
- **Polling:** scheduled checks if API allows (rate limits respected).
- **Manual:** admin enters carrier, tracking number, URL; customer sees updates on order page.
- Handle missing/partial tracking gracefully in UI.

## 11. API limitations (expected)
- No sandbox or incomplete docs.
- Rate limits, downtime, schema changes.
- No real-time inventory; delayed tracking.
- Regional restrictions or address validation differences (US addresses required).
- **Mitigation:** adapter isolation, retries with backoff, circuit-breaker patterns post-MVP, alerting, manual queue.

## 12. Manual fallback workflow (MVP required)
1. Order becomes `paid` → appears in **Fulfillment Queue**.
2. Admin verifies address/items and margin/policy checks.
3. Admin places order with supplier (external portal/email/process).
4. Admin marks `submitted` and records supplier reference.
5. When tracking arrives, admin enters shipment details → status `shipped`.
6. On delivery confirmation (supplier/carrier/manual), mark `delivered`.
7. Exceptions (cancelled by supplier, lost package) trigger support/refund workflow.

## 13. Data and security
- Store only necessary supplier credentials server-side; rotate secrets.
- Do not expose supplier cost/margin data to customer APIs.
- Log fulfillment actions for audit.

## 14. Open questions
- Which supplier(s) will be used first and what integration methods they offer?
- SLAs for processing and shipping to US customers?
- Who handles returns (customer → operator → supplier) and restocking (likely N/A for dropshipping)?
- Are branded packing slips/gift notes possible?
