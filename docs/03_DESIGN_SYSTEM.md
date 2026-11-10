# 03 — Design System

## 1. Purpose
Define a consistent visual language for Fetch derived from the **reference description** (primary reference image not available in workspace; validate against image before implementation). The UI must feel **premium, calm, editorial, and human-designed** — never generic, template-like, or AI-styled.

## 2. Design principles
1. **Restraint:** Soft contrast, purposeful whitespace, no decorative noise.
2. **Hierarchy:** Clear typographic and spatial hierarchy; one primary action per context.
3. **Authenticity:** Realistic product photography, natural compositions, no fake reviews/testimonials/stock clichés.
4. **Consistency:** Shared tokens for color, type, spacing, radius, and components across all pages.
5. **Usability first:** Aesthetics support comprehension, accessibility, and conversion.

## 3. Color palette (proposed tokens — validate against reference image)
Use Tailwind theme extensions / CSS variables. Names are semantic; hex values are starting points to tune against the reference.

| Token | Role | Suggested value | Usage notes |
|---|---|---|---|
| `brand.lavender` | Hero/feature background | `#E8E1F0` to `#EDE6F2` | Soft lavender; avoid neon/purple AI gradients |
| `brand.lavenderDeep` | Subtle accents | `#D8CCE6` | Sparingly |
| `neutral.cream` | Warm surface | `#F7F2EC` | Product warmth, alternating sections |
| `neutral.beige` | Secondary warm | `#EFE6DC` | Cards/sections if needed |
| `neutral.white` | Base surface | `#FFFFFF` | Cards, nav, content areas |
| `neutral.paper` | Page background | `#FAF8F5` | Light neutral collection backgrounds |
| `text.primary` | Headings/body | `#1F1F1D` | Near-black, soft |
| `text.secondary` | Supporting text | `#5C5C58` | Descriptions |
| `text.muted` | Meta/captions | `#8A8A85` | Labels, hints |
| `border.subtle` | Dividers | `#E7E2DC` | Hairline borders |
| `cta.primary` | Primary button bg | `#2B2B28` | Rounded dark CTA (reference) |
| `cta.primaryText` | Primary button text | `#FFFFFF` | High contrast |
| `state.success` | Success | muted green (tuned) | Non-vibrant |
| `state.error` | Error | muted red (tuned) | Accessible contrast |
| `focus.ring` | Focus indicator | `#2B2B28` or accessible tint | Visible keyboard focus |

**Avoid:** harsh saturated palettes, multi-stop gradients, glassmorphism, random accent colors.

## 4. Typography
- **Direction:** Minimal, clean, modern editorial. Choose a high-quality sans (or restrained serif pairing only if it matches reference) with excellent readability.
- **Implementation:** Self-host or licensed web fonts configured in the frontend; keep font count low (1–2 families).
- **Suggested pairing (to finalize against reference):** a refined grotesk/sans for UI (e.g., Inter/Manrope/DM Sans class alternatives evaluated for tone) — **final choice must be validated visually**; avoid default “AI startup” feel by tuning scale/spacing.

### Type scale (example)
| Style | Size/line | Weight | Use |
|---|---|---|---|
| Display/Hero | 40–56px / 1.05–1.15 | 500–600 | Hero headline |
| H1 | 32–40px / 1.15 | 500–600 | Page titles |
| H2 | 24–28px / 1.2 | 500 | Section titles |
| H3 | 18–20px / 1.3 | 500 | Card/product titles |
| Body | 16px / 1.6 | 400 | Paragraphs |
| Small | 14px / 1.5 | 400 | Meta, helpers |
| Caption | 12–13px / 1.4 | 400–500 | Labels (sparingly) |

## 5. Spacing system
Use a consistent scale (Tailwind default 4px base is acceptable): `4, 8, 12, 16, 24, 32, 48, 64, 80, 96`.
- **Section padding:** generous but balanced (e.g., 64–96px desktop, 40–56px mobile).
- **Component gaps:** 16–24px typical; avoid huge empty voids.
- **Container:** max width ~1200–1280px for content; hero may use wider canvas with controlled text measure.

## 6. Border radius
- **Buttons/inputs:** rounded but not pill-excessive (e.g., 9999px only if matching reference CTA; otherwise 10–14px). Reference indicates **rounded dark CTA** — confirm shape.
- **Cards/images:** subtle radius (8–16px) or as reference; avoid bubbly cards everywhere.
- **Organic shapes:** used intentionally in hero/background compositions (SVG/masked images), not random decoration.

## 7. Button styles
| Variant | Style | Use |
|---|---|---|
| Primary | Dark solid (`cta.primary`), light text, clear hover/focus/disabled | Main CTA (Shop, Checkout, Pay) |
| Secondary | Outline or soft neutral fill, dark text | Secondary actions |
| Ghost/Text | Minimal, underline or subtle hover | View All, tertiary links |
| Icon button | Adequate hit area (44px min), Lucide icons | Search, cart, menu |

States: default, hover, focus-visible, active, disabled, loading. No gratuitous motion.

## 8. Icon styles
- **Library:** Lucide React (consistent stroke width, e.g., 1.5–2).
- **Usage:** Functional icons only (search, cart, user, menu, chevron, check, alert). No random decorative icons or emojis.
- **Size:** 18–24px typical; align to text baseline.

## 9. Product image requirements
- **Authenticity:** Natural lighting, consistent color grading, realistic textures/materials.
- **Composition:** Editorial framing; avoid overly perfect/artificial AI-looking renders.
- **Aspect ratios:** Consistent per grid (e.g., 4:5 or 1:1 — finalize in design); define rules for thumbnails vs. gallery.
- **Backgrounds:** Neutral/warm contexts aligned to palette; avoid clutter.
- **Performance:** Responsive `srcset`/modern formats in implementation; lazy loading below fold.
- **Admin guidance:** Document minimum resolution and cropping rules for operators.

## 10. Component design principles
- Reusable, accessible, and purpose-driven components (see frontend architecture).
- **LoadingState / ErrorState / EmptyState** for every async surface.
- Forms: clear labels, inline validation, helpful errors, no placeholder-only labeling.
- **No fake data:** no fabricated reviews, testimonials, ratings counts, or “bestseller” claims without real basis.

## 11. Responsive breakpoints (Tailwind-aligned)
| Name | Min width | Notes |
|---|---|---|
| `sm` | 640px | Large phones/small tablets |
| `md` | 768px | Tablets |
| `lg` | 1024px | Laptops |
| `xl` | 1280px | Desktop container comfort |
| `2xl` | 1536px | Optional wide layouts |

Mobile-first CSS; hero image/text stack order optimized for small screens.

## 12. Animation guidelines
- **Purposeful only:** micro-interactions (hover, focus, accordion, drawer).
- **Duration:** 150–250ms typical; easing subtle.
- **Respect motion preferences:** `prefers-reduced-motion`.
- **Avoid:** parallax excess, bouncy gimmicks, attention-seeking loops.

## 13. Page-level consistency checklist
- Same nav/footer, announcement behavior, spacing rhythm, button styles, typography.
- Policy pages use readable long-form layout (not marketing hero overload).
- Admin uses a utilitarian but branded shell (clear tables/forms) — distinct from storefront but consistent tokens.
