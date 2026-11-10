// Frontend-only placeholder catalog. Replace with API-driven data/services in later phases.

export const categories = [
  { slug: 'walking-kits', name: 'Walking kits' },
  { slug: 'collars', name: 'Collars' },
  { slug: 'leashes', name: 'Leashes' },
  { slug: 'harnesses', name: 'Harnesses' },
  { slug: 'travel', name: 'Travel accessories' },
  { slug: 'lifestyle', name: 'Lifestyle' },
];

const img = (url) => `${url}?auto=format&fit=crop&w=900&q=80`;

export const products = [
  {
    id: 'p1',
    slug: 'classic-beige-collar',
    name: 'Classic Beige Collar',
    price: 38,
    category: 'collars',
    collection: 'walking-kits',
    variant: 'Beige · Brass hardware',
    options: { color: 'Classic Beige', hardware: 'Brass' },
    description: 'A refined everyday collar with a comfortable fit, smooth finish, and understated hardware.',
    details: ['Adjustable fit', 'Comfort-lined interior', 'Solid brass hardware', 'Designed for daily walks'],
    inStock: true,
    image: img('https://images.unsplash.com/photo-1601758228041-f3b2795255f1'),
  },
  {
    id: 'p2',
    slug: 'everyday-walking-leash',
    name: 'Everyday Walking Leash',
    price: 44,
    category: 'leashes',
    collection: 'walking-kits',
    variant: 'Beige · 5 ft',
    options: { color: 'Classic Beige', length: '5 ft' },
    description: 'A balanced leash with a comfortable hand feel and clean profile for city or neighborhood walks.',
    details: ['5 ft length', 'Secure clasp', 'Comfort grip', 'Pairs with Classic Beige Collar'],
    inStock: true,
    image: img('https://images.unsplash.com/photo-1548199973-03cce0bbc87b'),
  },
  {
    id: 'p3',
    slug: 'walking-kit-set',
    name: 'Walking Kit Set',
    price: 76,
    category: 'walking-kits',
    collection: 'walking-kits',
    variant: 'Collar + leash',
    options: { color: 'Classic Beige', set: 'Collar + Leash' },
    description: 'A coordinated collar-and-leash set in classic beige, made for effortless daily routines.',
    details: ['Matching collar and leash', 'Classic beige finish', 'Everyday comfort', 'Gift-ready presentation'],
    inStock: true,
    image: img('https://images.unsplash.com/photo-1587300003388-59208cc962cb'),
  },
  {
    id: 'p4',
    slug: 'premium-treat-pouch',
    name: 'Premium Treat Pouch',
    price: 32,
    category: 'lifestyle',
    collection: 'walking-kits',
    variant: 'Sand beige',
    options: { color: 'Sand Beige' },
    description: 'A compact pouch for treats and small essentials, designed to keep walks simple and organized.',
    details: ['Easy-access opening', 'Compact profile', 'Wipeable interior', 'Clip-friendly design'],
    inStock: true,
    image: img('https://images.unsplash.com/photo-1517849845537-4d257902454a'),
  },
  {
    id: 'p5',
    slug: 'comfort-step-in-harness',
    name: 'Comfort Step-In Harness',
    price: 52,
    category: 'harnesses',
    collection: 'walking-kits',
    variant: 'Sand beige · Adjustable',
    options: { color: 'Sand Beige', fit: 'Adjustable' },
    description: 'An easy step-in harness designed for comfort, control, and a clean everyday look.',
    details: ['Step-in design', 'Adjustable straps', 'Lightweight feel', 'Secure hardware'],
    inStock: true,
    image: img('https://images.unsplash.com/photo-1552053831-71594a27632d'),
  },
  {
    id: 'p6',
    slug: 'travel-water-bowl',
    name: 'Travel Water Bowl',
    price: 24,
    category: 'travel',
    collection: 'travel',
    variant: 'Warm neutral',
    options: { color: 'Warm Neutral' },
    description: 'A portable bowl for hydration on the go, made to pack easily for day trips and travel.',
    details: ['Packable form', 'Easy to clean', 'Travel friendly', 'Durable material'],
    inStock: true,
    image: img('https://images.unsplash.com/photo-1534361960057-19889db9621e'),
  },
  {
    id: 'p7',
    slug: 'city-collar-sand',
    name: 'City Collar — Sand',
    price: 36,
    category: 'collars',
    collection: 'walking-kits',
    variant: 'Sand · Matte hardware',
    options: { color: 'Sand', hardware: 'Matte' },
    description: 'A lightweight collar with a soft sand tone and minimal hardware for a modern everyday look.',
    details: ['Lightweight profile', 'Adjustable fit', 'Matte hardware', 'Comfort edge finish'],
    inStock: true,
    image: img('https://images.unsplash.com/photo-1561037404-61cd46aa615b'),
  },
  {
    id: 'p8',
    slug: 'weekend-travel-kit',
    name: 'Weekend Travel Kit',
    price: 68,
    category: 'travel',
    collection: 'travel',
    variant: 'Pouch + bowl',
    options: { set: 'Pouch + Bowl' },
    description: 'A simple travel pairing for short trips — practical essentials in a calm, coordinated palette.',
    details: ['Travel pouch', 'Portable bowl', 'Easy to pack', 'Coordinated neutrals'],
    inStock: false,
    image: img('https://images.unsplash.com/photo-1544568100-847a948585b9'),
  },
];

export function getProductBySlug(slug) {
  return products.find((p) => p.slug === slug) || null;
}

export function getProductsByCollection(collection) {
  return products.filter((p) => p.collection === collection);
}

export function getProductsByCategory(category) {
  return products.filter((p) => p.category === category);
}

export const featuredWalkEssentials = products.filter((p) => p.collection === 'walking-kits').slice(0, 4);
export const bestSellers = [products[0], products[1], products[4]].filter(Boolean);
