const SUSTAINABILITY_IMAGE =
  'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=1200&q=80'; // Placeholder: replace with brand-supplied sustainability/lifestyle asset

const points = [
  {
    title: 'Considered materials',
    text: 'We prioritize durable, comfortable materials chosen for everyday use and a refined finish.',
  },
  {
    title: 'Thoughtful design',
    text: 'Clean forms, practical details, and fittings selected to work well on real walks.',
  },
  {
    title: 'Long-lasting essentials',
    text: 'Pieces designed to stay in rotation — reducing the need to replace what should last.',
  },
];

export default function SustainabilitySection() {
  return (
    <section className="bg-fetch-lavender-soft/35 py-16 sm:py-20 lg:py-24">
      <div className="section-container">
        <div className="grid gap-10 lg:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)] lg:items-start lg:gap-16">
          <div>
            <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Sustainability</p>
            <h2 className="mt-3 text-[2rem] font-semibold leading-[1.08] tracking-[-0.03em] text-fetch-ink sm:text-[2.45rem]">
              A lighter approach to premium pet essentials.
            </h2>
            <ul className="mt-9 space-y-0 border-t border-fetch-ink/[0.08]">
              {points.map((item, index) => (
                <li key={item.title} className="grid gap-1 border-b border-fetch-ink/[0.08] py-5 sm:grid-cols-[2.75rem_minmax(0,1fr)] sm:gap-4 sm:py-6">
                  <span className="text-[12px] font-medium tabular-nums text-fetch-ink/35">0{index + 1}</span>
                  <div>
                    <h3 className="text-[16px] font-semibold tracking-[-0.01em] text-fetch-ink">{item.title}</h3>
                    <p className="mt-1.5 text-[15px] leading-[1.75] text-fetch-muted">{item.text}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
          <div className="aspect-[4/3] overflow-hidden bg-[#F0EEE9] lg:sticky lg:top-24">
            <img
              src={SUSTAINABILITY_IMAGE}
              alt="Dog accessories styled in natural light"
              className="h-full w-full object-cover transition duration-700 ease-[cubic-bezier(0.22,1,0.36,1)] hover:scale-[1.015]"
              loading="lazy"
              decoding="async"
            />
          </div>
        </div>
      </div>
    </section>
  );
}
