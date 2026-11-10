import ProductCard from './ProductCard.jsx';

export default function ProductMarquee({ products, duration = '38s', cardWidth = 'clamp(240px, 72vw, 300px)' }) {
  if (!products?.length) return null;

  const loop = [...products, ...products];

  return (
    <div className="fetch-marquee" style={{ '--marquee-duration': duration }}>
      <div className="fetch-marquee__track gap-6 py-1 sm:gap-7">
        {loop.map((product, index) => (
          <div
            key={`${product.id}-${index}`}
            className="shrink-0"
            style={{ width: cardWidth }}
            aria-hidden={index >= products.length ? true : undefined}
          >
            <ProductCard product={product} />
          </div>
        ))}
      </div>
    </div>
  );
}
