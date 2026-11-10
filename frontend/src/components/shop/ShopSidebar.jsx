import Button from '../ui/Button.jsx';

const availabilityOptions = [
  { value: 'all', label: 'All products' },
  { value: 'in', label: 'In stock' },
  { value: 'out', label: 'Out of stock' },
];

export default function ShopSidebar({
  categories,
  selectedCategory,
  onCategoryChange,
  availability,
  onAvailabilityChange,
  onClear,
  onApply,
  applying = false,
  className = '',
}) {
  return (
    <aside className={className}>
      <div className="border-b border-fetch-ink/[0.08] pb-5">
        <h2 className="text-[15px] font-semibold tracking-[-0.01em] text-fetch-ink">Filters</h2>
        <p className="mt-1 text-[13px] leading-5 text-fetch-muted">Refine the collection by category and availability.</p>
      </div>

      <div className="border-b border-fetch-ink/[0.08] py-5">
        <h3 className="text-[12px] font-medium uppercase tracking-[0.16em] text-fetch-ink/50">Category</h3>
        <div className="mt-3 space-y-2">
          <label className="flex cursor-pointer items-center gap-3 text-[14px] text-fetch-ink/80">
            <input
              type="radio"
              name="category"
              checked={!selectedCategory}
              onChange={() => onCategoryChange('')}
              className="h-4 w-4 border-fetch-ink/20 text-fetch-dark focus:ring-fetch-dark/20"
            />
            <span>All categories</span>
          </label>
          {categories.map((cat) => (
            <label key={cat.slug} className="flex cursor-pointer items-center gap-3 text-[14px] text-fetch-ink/80">
              <input
                type="radio"
                name="category"
                checked={selectedCategory === cat.slug}
                onChange={() => onCategoryChange(cat.slug)}
                className="h-4 w-4 border-fetch-ink/20 text-fetch-dark focus:ring-fetch-dark/20"
              />
              <span>{cat.name}</span>
            </label>
          ))}
        </div>
      </div>

      <div className="py-5">
        <h3 className="text-[12px] font-medium uppercase tracking-[0.16em] text-fetch-ink/50">Availability</h3>
        <div className="mt-3 space-y-2">
          {availabilityOptions.map((opt) => (
            <label key={opt.value} className="flex cursor-pointer items-center gap-3 text-[14px] text-fetch-ink/80">
              <input
                type="radio"
                name="availability"
                checked={availability === opt.value}
                onChange={() => onAvailabilityChange(opt.value)}
                className="h-4 w-4 border-fetch-ink/20 text-fetch-dark focus:ring-fetch-dark/20"
              />
              <span>{opt.label}</span>
            </label>
          ))}
        </div>
      </div>

      <div className="mt-2 flex flex-col gap-2">
        <Button onClick={onApply} loading={applying} className="w-full">
          Apply filters
        </Button>
        <Button variant="ghost" onClick={onClear} disabled={applying} className="w-full">
          Clear
        </Button>
      </div>
    </aside>
  );
}
