// Frontend demo data only. Replace with authenticated API data in later phases.
export const demoOrders = [
  {
    orderNumber: 'FT-10482',
    placedAt: '2026-10-18',
    status: 'Shipped',
    total: 82.0,
    email: 'alex@example.com',
    items: [
      { name: 'Classic Beige Collar', variant: 'Beige · Brass hardware', qty: 1, price: 38 },
      { name: 'Everyday Walking Leash', variant: 'Beige · 5 ft', qty: 1, price: 44 },
    ],
    shipping: { name: 'Alex Morgan', line1: '245 Market Street', city: 'San Francisco', state: 'CA', postalCode: '94105', country: 'United States' },
    tracking: { carrier: 'Carrier (demo)', number: 'TRK-DEMO-10482', status: 'In transit', eta: 'Estimated 5–10 business days (dropshipping demo)' },
    timeline: [
      { label: 'Order placed', at: '2026-10-18', done: true },
      { label: 'Payment verified', at: '2026-10-18', done: true, note: 'Demo status — real verification is server-side.' },
      { label: 'Fulfillment submitted', at: '2026-10-19', done: true },
      { label: 'Shipped', at: '2026-10-21', done: true },
      { label: 'Delivered', at: 'Pending', done: false },
    ],
  },
  {
    orderNumber: 'FT-10311',
    placedAt: '2026-09-02',
    status: 'Delivered',
    total: 52.0,
    email: 'alex@example.com',
    items: [{ name: 'Comfort Step-In Harness', variant: 'Sand beige · Adjustable', qty: 1, price: 52 }],
    shipping: { name: 'Alex Morgan', line1: '245 Market Street', city: 'San Francisco', state: 'CA', postalCode: '94105', country: 'United States' },
    tracking: { carrier: 'Carrier (demo)', number: 'TRK-DEMO-10311', status: 'Delivered', eta: 'Delivered (demo)' },
    timeline: [
      { label: 'Order placed', at: '2026-09-02', done: true },
      { label: 'Payment verified', at: '2026-09-02', done: true },
      { label: 'Shipped', at: '2026-09-04', done: true },
      { label: 'Delivered', at: '2026-09-09', done: true },
    ],
  },
];

export function getDemoOrder(orderNumber) {
  return demoOrders.find((o) => o.orderNumber === orderNumber) || null;
}

export const demoAddresses = [
  {
    id: 'addr-1',
    label: 'Default shipping',
    name: 'Alex Morgan',
    line1: '245 Market Street',
    city: 'San Francisco',
    state: 'CA',
    postalCode: '94105',
    country: 'United States',
    isDefault: true,
  },
];
