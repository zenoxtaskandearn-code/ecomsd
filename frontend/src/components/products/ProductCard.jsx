import { Link } from 'react-router-dom';

export default function ProductCard({ product }) {
  return (
    <article className="group">
      <Link to={`/product/${product.slug}`} className="block focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-fetch-dark/30">
        <div className="aspect-[4/5] overflow-hidden bg-[#F0EEE9]">
          <img
            src={product.image}
            alt={product.name}
            className="h-full w-full object-cover transition duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] group-hover:scale-[1.03]"
            loading="lazy"
            decoding="async"
          />
        </div>
        <div className="pt-4">
          <h3 className="text-[15px] font-medium leading-[1.35] tracking-[-0.01em] text-fetch-ink transition-colors duration-300 group-hover:text-fetch-ink/80">
            {product.name}
          </h3>
          {product.variant && <p className="mt-1 text-[13px] leading-5 text-fetch-muted">{product.variant}</p>}
          <p className="mt-2 text-[14px] font-medium tabular-nums text-fetch-ink">${product.price.toFixed(2)}</p>
        </div>
      </Link>
    </article>
  );
}
