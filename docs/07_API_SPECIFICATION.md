# 07 — API Specification

## 1. Conventions
- **Base path:** `/api/v1` (recommended).
- **Format:** JSON request/response; UTF-8.
- **Auth:** Bearer token (JWT) or HTTP-only session cookie — **implementation decision in Phase 2**; spec below marks requirements as `Public`, `Customer`, or `Admin`. Do not send secrets from frontend.
- **Errors:** consistent envelope, e.g. `{ "error": { "code": "STRING", "message": "...", "details": [...], "correlationId": "..." } }`.
- **Validation:** Bean Validation; 400 with field errors.
- **Pagination:** `page`, `size` (capped), return `content`, `totalElements`, `totalPages`.
- **Idempotency:** payment/create-capture and webhooks use idempotency keys/event IDs.

## 2. Authentication & users

| Method | URL | Purpose | Auth | Request body | Response | Validation / errors |
|---|---|---|---|---|---|---|
| POST | `/auth/register` | Create customer account | Public | `{ email, password, firstName, lastName, phone? }` | `{ user, tokens/session }` (no password) | Email format/unique; password policy; 409 conflict |
| POST | `/auth/login` | Authenticate | Public | `{ email, password }` | tokens/session + user summary | 401 invalid credentials; rate limit |
| POST | `/auth/logout` | Invalidate session/token | Customer | optional | 204 | 401 |
| POST | `/auth/refresh` | Refresh access token (if JWT) | Public/token | `{ refreshToken }` | new tokens | 401/403 |
| POST | `/auth/forgot-password` | Start reset | Public | `{ email }` | 202 generic (no enumeration) | rate limit |
| POST | `/auth/reset-password` | Complete reset | Public | `{ token, newPassword }` | 204 | token expiry/invalid |
| GET | `/users/me` | Current profile | Customer | — | user profile | 401 |
| PATCH | `/users/me` | Update profile | Customer | allowed fields | updated profile | validation |

## 3. Addresses

| Method | URL | Purpose | Auth | Request | Response | Notes |
|---|---|---|---|---|---|---|
| GET | `/users/me/addresses` | List addresses | Customer | — | list | |
| POST | `/users/me/addresses` | Add address | Customer | address DTO | created | US country/postal validation rules configurable |
| PUT | `/users/me/addresses/{id}` | Update | Customer | address DTO | updated | ownership check |
| DELETE | `/users/me/addresses/{id}` | Delete | Customer | — | 204 | ownership |

## 4. Catalog (public)

| Method | URL | Purpose | Auth | Request | Response | Notes |
|---|---|---|---|---|---|---|
| GET | `/categories` | List active categories | Public | — | tree/list | cacheable |
| GET | `/categories/{slug}` | Category detail | Public | — | category | 404 |
| GET | `/products` | List/search products | Public | `q, category, minPrice, maxPrice, sort, page, size` | paged products | only active products |
| GET | `/products/{slug}` | Product detail | Public | — | product + variants + images | 404 |
| GET | `/products/{slug}/related` | Related products | Public | — | list | optional MVP |

## 5. Cart

| Method | URL | Purpose | Auth | Request | Response | Notes |
|---|---|---|---|---|---|---|
| GET | `/cart` | Get current cart | Customer (or guest token strategy) | — | cart + items + totals | totals informational; checkout revalidates |
| POST | `/cart/items` | Add item | same | `{ variantId, quantity }` | updated cart | validate variant active |
| PATCH | `/cart/items/{itemId}` | Update qty | same | `{ quantity }` | updated cart | 0 => remove behavior defined |
| DELETE | `/cart/items/{itemId}` | Remove item | same | — | updated cart | |
| DELETE | `/cart` | Clear cart | same | — | 204 | |

## 6. Checkout and orders (customer)

| Method | URL | Purpose | Auth | Request | Response | Notes |
|---|---|---|---|---|---|---|
| POST | `/checkout/preview` | Validate cart/address, compute totals | Customer | `{ shippingAddress, billingAddress?, shippingMethod? }` | `{ totals, warnings, internalOrderDraftRef? }` | server-side pricing |
| POST | `/orders` | Create pending order from cart | Customer | checkout DTO + idempotency header | `{ orderNumber, status, total, paypalClientContext? }` | locks snapshot; may merge with PayPal create |
| GET | `/orders` | My orders | Customer | pagination | paged orders | |
| GET | `/orders/{orderNumber}` | Order detail | Customer | — | order + items + shipment summary | ownership |
| POST | `/orders/{orderNumber}/cancel` | Request cancel (rules apply) | Customer | `{ reason? }` | updated status | only if not shipped/paid rules |

## 7. Payments (PayPal)

| Method | URL | Purpose | Auth | Request | Response | Notes |
|---|---|---|---|---|---|---|
| POST | `/payments/paypal/create` | Create PayPal order for internal order | Customer | `{ orderNumber }` + idempotency | `{ paypalOrderId }` | server calls PayPal |
| POST | `/payments/paypal/capture` | Capture after buyer approval | Customer | `{ orderNumber, paypalOrderId }` | `{ orderNumber, status, captureId }` | **authoritative paid state** |
| POST | `/webhooks/paypal` | Receive PayPal events | **PayPal (verified)** | PayPal payload | 200 quickly after verify/enqueue | signature verify; idempotent; no auth cookie |
| GET | `/payments/{id}` (admin or owner-scoped) | Payment detail | Admin/limited | — | payment (redacted) | no raw secrets |

## 8. Shipments/tracking (customer read)

| Method | URL | Purpose | Auth | Request | Response |
|---|---|---|---|---|---|
| GET | `/orders/{orderNumber}/tracking` | Tracking info | Customer | — | shipment(s)/status |

## 9. Admin APIs (all `Admin` role)

| Method | URL | Purpose | Request | Response |
|---|---|---|---|---|
| GET | `/admin/dashboard/summary` | KPI summary | — | counts/totals (defined carefully) |
| GET/POST | `/admin/categories` | List/create | category DTO | paged/created |
| PUT/DELETE | `/admin/categories/{id}` | Update/archive | DTO | updated |
| GET/POST | `/admin/products` | List/create | product DTO | paged/created |
| GET/PUT | `/admin/products/{id}` | Detail/update | DTO | product |
| POST | `/admin/products/{id}/variants` | Add variant | variant DTO | variant |
| PUT | `/admin/variants/{id}` | Update variant | DTO | variant |
| POST | `/admin/products/{id}/images` | Attach image metadata/upload flow | multipart or URL DTO | image record |
| GET | `/admin/orders` | List/search orders | filters, pagination | paged |
| GET | `/admin/orders/{orderNumber}` | Detail | — | full admin view |
| PATCH | `/admin/orders/{orderNumber}/status` | Transition status | `{ status, note? }` | updated (guarded) |
| POST | `/admin/orders/{orderNumber}/shipments` | Add/update tracking | shipment DTO | shipment |
| POST | `/admin/orders/{orderNumber}/fulfillment/submit` | Submit to supplier adapter/manual | `{ mode }` | result/queued |
| GET/POST | `/admin/suppliers` | Manage suppliers | DTO | list/created |
| GET/POST | `/admin/supplier-products` | Manage mappings | DTO | list/created |
| POST | `/admin/payments/{id}/refund` | Initiate refund (when supported) | `{ amount?, reason }` | refund/payment status |
| GET | `/admin/customers` | List customers | pagination/search | paged (privacy-aware) |

## 10. Contact/content (optional MVP endpoints)
| Method | URL | Purpose | Auth | Request | Response |
|---|---|---|---|---|---|
| POST | `/contact` | Submit contact form | Public | `{ name, email, subject, message }` | 202 | rate limit, spam mitigation plan |
| GET | `/content/{slug}` | CMS-lite policy/content blocks | Public | — | content | optional |

## 11. Standard error responses
- `400` validation, `401` unauthenticated, `403` forbidden, `404` not found, `409` conflict (duplicate/idempotency/state), `422` business rule (e.g., item unavailable), `429` rate limited, `500` unexpected (correlation ID logged).

## 12. Security notes
- Admin endpoints require RBAC and should be logged for sensitive actions.
- Webhook endpoint must not trust client auth; must verify PayPal signature and use raw body as required by verification method.
- Never return password hashes, full PayPal secrets, or unnecessary PII.
