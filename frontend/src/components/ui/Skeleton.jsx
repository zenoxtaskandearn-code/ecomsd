export function Skeleton({ className = '' }) {
  return <div className={`animate-pulse rounded-[2px] bg-fetch-ink/[0.06] ${className}`} aria-hidden="true" />;
}

export function ProductCardSkeleton() {
  return (
    <div>
      <Skeleton className="aspect-[4/5] w-full" />
      <div className="pt-4">
        <Skeleton className="h-4 w-[78%]" />
        <Skeleton className="mt-2 h-3 w-[48%]" />
        <Skeleton className="mt-3 h-4 w-16" />
      </div>
    </div>
  );
}

export function ProductGridSkeleton({ count = 8, columns = 4 }) {
  const colClass =
    columns === 3 ? 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3' : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4';
  return (
    <div className={`grid gap-x-6 gap-y-11 sm:gap-x-7 sm:gap-y-12 ${colClass}`}>
      {Array.from({ length: count }).map((_, i) => (
        <ProductCardSkeleton key={i} />
      ))}
    </div>
  );
}
