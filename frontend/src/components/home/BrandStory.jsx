export default function BrandStory() {
  return (
    <section className="bg-white py-20 sm:py-24 lg:py-28" id="about">
      <div className="section-container">
        <div className="grid gap-10 lg:grid-cols-[minmax(0,0.38fr)_minmax(0,0.62fr)] lg:items-start lg:gap-16">
          <div>
            <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Our approach</p>
            <div className="mt-5 h-px w-12 bg-fetch-ink/15" aria-hidden="true" />
          </div>
          <div className="max-w-[640px]">
            <h2 className="text-[2.1rem] font-semibold leading-[1.08] tracking-[-0.03em] text-fetch-ink sm:text-[2.6rem]">
              Designed for everyday walks, made with intention.
            </h2>
            <p className="mt-6 text-[16px] leading-[1.85] text-fetch-muted sm:text-[17px]">
              Lupine &amp; Leash creates premium dog accessories that balance comfort, durability, and a calm, modern aesthetic.
              We focus on thoughtful materials, considered details, and pieces that feel at home in your daily routine —
              not loud, not overdesigned, just quietly well made.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
