const LIFESTYLE_IMAGE = '/lifestyle-dog.png';

export default function LifestyleSection() {
  return (
    <section className="bg-white py-16 sm:py-20 lg:py-24">
      <div className="section-container">
        <div className="grid items-center gap-10 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)] lg:gap-16">
          <div className="aspect-[5/4] overflow-hidden bg-[#F0EEE9]">
            <img
              src={LIFESTYLE_IMAGE}
              alt="Dog enjoying an outdoor walk with premium accessories"
              className="h-full w-full object-cover transition duration-700 ease-[cubic-bezier(0.22,1,0.36,1)] hover:scale-[1.015]"
              loading="lazy"
              decoding="async"
            />
          </div>
          <div className="max-w-[460px] lg:pl-2">
            <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Life outdoors</p>
            <h2 className="mt-3 text-[2rem] font-semibold leading-[1.08] tracking-[-0.03em] text-fetch-ink sm:text-[2.45rem]">
              Made for walks you both look forward to.
            </h2>
            <p className="mt-5 text-[16px] leading-[1.85] text-fetch-muted sm:text-[17px]">
              From neighborhood strolls to weekend adventures, our essentials are built to feel comfortable in hand and
              easy on your dog — with a look that stays timeless.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
