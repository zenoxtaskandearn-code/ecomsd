# Phase 1 — Visual Design Validation & Design System (Pre-implementation)

**Status:** In progress (awaiting reference image file for pixel/token validation).  
**Scope:** No application code, no migrations, no dependencies. Design/spec validation only.

## 1. Objective
Validate the Fetch visual language against the **primary reference screenshot** and finalize implementation-ready design tokens/specs for key screens before Phase 2 setup.

## 2. Reference assets
| Asset | Status |
|---|---|
| Primary screenshot | **Missing from workspace** — add to `docs/reference/` (see `docs/reference/README.md`) |
| User description | **Available** — used as interim source of truth |

## 3. Reconciliation checklist (complete when image is added)
| Area | Reference expectation (description) | Token/spec to confirm | Status |
|---|---|---|---|
| Hero background | Soft lavender | `brand.lavender` hex, gradient? (avoid AI gradients) | Pending image |
| Product/warm tones | Warm beige/cream | `neutral.cream`, `neutral.beige` | Pending |
| Contrast | Soft, calm | Text/bg contrast ratios (WCAG AA) | Pending |
| Typography | Minimal, clean, editorial | Font family, weights, scale, letter-spacing | Pending |
| Hero composition | Left text, right dog, organic white shape behind dog | Grid/flex, shape SVG/mask, responsive stack order | Pending |
| CTA | Rounded dark button | `cta.primary` fill, radius, padding, hover/focus | Pending |
| Announcement bar | Thin, top | Height, typography, social icon treatment | Pending |
| Navbar | Minimal; logo left, links center, search/cart right | Spacing, icon sizes, mobile drawer | Pending |
| Collection section | Light neutral bg, title + small “View All”, clean cards | Card anatomy, image ratio, hover restraint | Pending |
| Photography | Large, natural, editorial | Aspect ratios, art direction rules | Pending |
| “Human” quality | No generic AI/template patterns | Review against anti-pattern list in `03_DESIGN_SYSTEM.md` | Pending |

## 4. Proposed implementation tokens (Tailwind/CSS variables — tune after image)
> **Interim values** for Phase 2 scaffolding only; must be visually matched to the reference.

```css
:root {
  --brand-lavender: #EBE4F2;
  --brand-lavender-deep: #D9CDE8;
  --neutral-paper: #FAF8F5;
  --neutral-cream: #F6F1EA;
  --neutral-beige: #EEE5DB;
  --neutral-white: #FFFFFF;
  --text-primary: #1F1F1C;
  --text-secondary: #5A5A55;
  --text-muted: #878782;
  --border-subtle: #E6E1DA;
  --cta-primary: #262624;
  --cta-primary-text: #FFFFFF;
  --focus-ring: #262624;
}
```

Tailwind `theme.extend.colors` should map semantic names (e.g., `brand.lavender`, `cta.primary`) for consistency.

## 5. Typography specification (to finalize)
- **Families:** 1 primary sans for UI/body/headings (editorial, not generic). Optional: none for MVP.
- **Weights:** 400, 500, 600.
- **Scale:** Display 48/1.1, H1 36/1.15, H2 26/1.2, H3 18/1.3, Body 16/1.65, Small 14/1.5.
- **Loading:** self-hosted/licensed in Phase 2/3; define `font-display: swap`.
- **Accessibility:** minimum 16px body on mobile; sufficient contrast.

## 6. Key screen specifications (acceptance targets)

### 6.1 Homepage
- **Announcement bar:** single-line message, configurable; dismiss behavior optional (post-MVP). Subtle icons only if approved.
- **Nav:** sticky optional (confirm with reference); clear active/focus states; cart count badge when > 0.
- **Hero:** 2-column desktop (text left ~40–45%, visual right), stacked mobile with image treatment preserved; organic white shape as SVG/clip-path or positioned asset; headline ≤ ~8 words, supporting text 1–2 short lines; one primary CTA.
- **Featured collection:** light neutral section; heading + “View All” text link; 3–4 product cards desktop, responsive grid; no fake ratings/badges.
- **States:** skeleton for product fetch; empty/error copy.

### 6.2 Shop / Collection (PLP)
- Filters + sort; grid/list consistency; pagination or “Load more” (pick one for MVP).
- Card: image, product name, price, variant swatch/count only if real.

### 6.3 Product Detail (PDP)
- Gallery with thumbnails; variant selector; quantity; add-to-cart; shipping/returns summary links.
- **No reviews block unless real verified data exists.**

### 6.4 Cart & Checkout
- Cart: editable quantities, remove, summary; honest shipping/tax messaging.
- Checkout: single-page sections or steps; PayPal as payment action; server totals authoritative; clear failure recovery.

### 6.5 Account / Orders
- Simple tables/cards; status labels mapped to business states; tracking link when present.

### 6.6 Admin
- Utilitarian tables/forms using same tokens; clear status badges; destructive actions confirmed.

### 6.7 Policy pages
- Readable prose width, consistent typography, last-updated/configurable contact.

## 7. Responsive behavior (must match reference intent)
| Breakpoint | Hero | Nav | Grid |
|---|---|---|---|
| < 640px | Stack; text first or per reference; CTA full-width optional | Hamburger/drawer | 1–2 cols |
| 640–1023px | Adjust proportions | Condensed links or drawer | 2–3 cols |
| ≥ 1024px | Reference-like 2-column composition | Full nav | 3–4 cols |

## 8. Accessibility checklist (Phase 1/3 gate)
- [ ] Color contrast AA for text/UI components (verify after final tokens)
- [ ] Visible `:focus-visible` rings
- [ ] Semantic landmarks (`header`, `nav`, `main`, `footer`)
- [ ] Form labels/errors associated; PayPal/checkout errors announced
- [ ] Icon buttons have accessible names
- [ ] `prefers-reduced-motion` respected

## 9. Imagery & content rules
- Use authentic/brand-approved photography; avoid AI-looking artifacts.
- No fake testimonials, reviews, ratings, press logos, or urgency/countdown unless real.
- Product copy factual; shipping times aligned with dropshipping reality.

## 10. Open items requiring your input
1. **Add the reference screenshot** to `docs/reference/` and confirm approval of tuned tokens.
2. **Brand assets:** final logo (SVG), favicon, social/share image (later) — Fetch remains configurable.
3. **Font choice:** approve a specific licensed/self-hosted family once compared to reference.
4. **Hero image asset:** real dog/product photo direction and rights.
5. **Open decisions from planning:** guest checkout?, auth mechanism (JWT vs sessions)?, US tax handling at MVP?, returns workflow?, supplier(s) and verified APIs?, PayPal production timeline?, transactional email provider?

## 11. Phase 1 exit criteria (sign-off)
- [ ] Reference image added and reviewed
- [ ] Tokens (color/type/spacing/radius) finalized in `03_DESIGN_SYSTEM.md`
- [ ] Key screen specs approved (or Figma link attached)
- [ ] Accessibility contrast verified
- [ ] Decision log updated for open questions
- **After sign-off:** proceed to **Phase 2 — Project setup and architecture decisions** (scaffold React JS/JSX + Tailwind + Spring Boot/Maven, env conventions, auth decision) — **no business logic/payment capture until later phases**.
