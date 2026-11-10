import ProductCard from './ProductCard.jsx';

export default function ProductGrid({ products, columns = 4 }) {
  const colClass =
    columns === 3
      ? 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3'
      : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4';

  return (
    <div className={`grid gap-x-6 gap-y-11 sm:gap-x-7 sm:gap-y-12 ${colClass}`}>
      {products.map((product) => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
}
