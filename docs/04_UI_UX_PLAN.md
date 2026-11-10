# 04 — UI/UX Plan

## 1. Global layout
### Persistent elements (storefront)
- **Announcement bar:** Thin top strip (shipping/promo message, configurable). Subtle social icons only if brand-approved; no clutter.
- **Navbar:** Minimal — brand logo left, primary nav links center (Shop, Collections/Categories, About, Contact/Help as IA dictates), search and cart icons right; account entry point. Mobile: hamburger/drawer with accessible focus management.
- **Footer:** Navigation, policy links, contact/support, newsletter signup **only if real provider/process exists** (otherwise omit until ready), copyright/configurable brand. No fake trust badges.
- **Cart drawer (optional) + cart page:** Quick review; full cart page for editing/checkout CTA.
- **Global states:** Toast/inline feedback for add-to-cart/errors; loading skeletons where helpful.

## 2. Page plans

### 2.1 Homepage (`/`)
| Section | Content/behavior |
|---|---|
| Hero | Lavender background; left: short headline, concise supporting text, rounded dark CTA; right: large dog image with organic white/soft shape behind (composition per reference). Responsive stacking. |
| Featured collection | Light neutral background; collection title + small “View All” link; curated product grid with clean cards (image, name, price, optional variant hint). |
| Brand/value proposition | Editorial block: materials, comfort, design philosophy — factual, restrained. |
| Secondary collection/category highlights | 1–2 purposeful sections; not repetitive. |
| Footer CTA (optional) | Simple, non-pushy. |

### 2.2 Shop (`/shop`)
- Page title, result count, sort (relevance/newest/price) as supported.
- Filters: category, price range, key attributes; mobile filter drawer.
- Product grid with pagination/load more; empty state if no results.
- Clear applied filters.

### 2.3 Collection pages (`/collections/:slug`)
- Collection hero/header (image optional, restrained), description, filtered product grid.
- Breadcrumbs.

### 2.4 Product details (`/products/:slug`)
- Breadcrumbs, gallery (main + thumbnails), title, price, variant selectors (size/color), quantity selector, primary add-to-cart.
- Accordion/sections: Description, Materials & care, Shipping/returns summary (links to policies), Sizing guidance if applicable.
- Related products (curated/rule-based) — only if data supports; avoid filler.
- **No reviews/testimonials UI with fake content.** If reviews feature is added later, show only verified real reviews.

### 2.5 Search (`/search?q=`)
- Search input, suggestions optional post-MVP; results grid; empty/no-results state with helpful actions.

### 2.6 Cart (`/cart` and/or drawer)
- Line items: image, name, variant, unit price, quantity controls, remove.
- Order summary: subtotal; shipping/taxes note (“calculated at checkout” or configured estimate).
- Prominent checkout CTA; continue shopping link.

### 2.7 Checkout (`/checkout`)
- Steps/sections: contact, shipping address (US-focused fields), shipping method (if configured), order review, PayPal payment.
- Server-validated totals; clear error handling for payment failures.
- Guest vs. account flow per open decision.
- Link to policies; trust through clarity, not fake badges.

### 2.8 Login (`/login`)
- Email/password, forgot password link, register link. Validation and accessible errors.

### 2.9 Registration (`/register`)
- Minimal required fields; password rules; policy acknowledgments if required.

### 2.10 Customer account (`/account`)
- Overview: recent orders, profile shortcuts.
- Subsections/routes: profile, addresses, security/password (as implemented).

### 2.11 Order history (`/account/orders`)
- List with order #, date, status, total; link to detail.

### 2.12 Order tracking / detail (`/account/orders/:id` and confirmation)
- Status timeline (simplified), items snapshot, shipping address (masked as appropriate), tracking number/link when available, support contact.

### 2.13 Contact (`/contact`)
- Contact form (submits to backend/email integration when configured) or clear mailto/support instructions; response expectations. No fake live chat.

### 2.14 About (`/about`)
- Brand story aligned to premium pet lifestyle positioning; authentic tone.

### 2.15 Blog (`/blog`, `/blog/:slug`)
- MVP: simple index/detail or “coming soon” content strategy if no CMS — **do not invent posts**. Architecture supports future CMS.

### 2.16 FAQ (`/faq`)
- Accordion Q&A: shipping times, returns, sizing, payments — aligned with real policies.

### 2.17 Shipping policy (`/policies/shipping`)
- Dropshipping-transparent: processing + transit estimates, US delivery notes, tracking availability.

### 2.18 Returns and refunds (`/policies/returns`)
- Clear eligibility, windows, process, refund timing (PayPal/original method considerations).

### 2.19 Privacy policy (`/policies/privacy`)
- Data collected, PayPal/orders, cookies/analytics (when added), rights/contact — legal review recommended before launch.

### 2.20 Terms and conditions (`/policies/terms`)
- Purchase terms, liability boundaries appropriate to jurisdictional review — legal review recommended.

### 2.21 Admin dashboard (`/admin`)
- **Separate shell/layout** with side/top nav; protected routes.
- **Dashboard home:** key counts (orders needing action, recent payments/fulfillment issues) — accurate and minimal.
- **Products/categories:** tables + forms, variant/image management.
- **Orders:** filterable list, detail view, status transitions (guarded), tracking entry, internal notes.
- **Customers:** read-oriented list/detail (privacy-aware).
- **Suppliers/fulfillment:** mappings, queue, manual submit/fallback actions.
- **Settings (limited MVP):** configurable brand/contact/shipping messages as appropriate.

## 3. Cross-cutting UX requirements
- **Accessibility:** labels, errors announced, focus traps in modals/drawers, skip link, contrast checks.
- **Performance UX:** skeletons/spinners for async; avoid layout shift.
- **Error handling:** human-readable messages; retry paths; preserve form input where safe.
- **Trust:** clear policies, honest shipping times, visible support path.

## 4. Navigation IA (proposed)
`Home | Shop | Collections (dropdown or page) | About | FAQ/Help | Contact` + utility icons. Final IA validated in design phase.
