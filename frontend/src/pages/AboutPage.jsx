import StoreLayout from '../components/layout/StoreLayout.jsx';

export default function AboutPage() {
  return (
    <StoreLayout>
      <div className="border-b border-fetch-ink/[0.06] bg-[#FBFAF8]">
        <div className="section-container py-12 sm:py-16">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">About</p>
          <h1 className="mt-3 max-w-[14ch] text-[2.4rem] font-semibold leading-[1.03] tracking-[-0.03em] text-fetch-ink sm:text-[3rem]">
            Premium essentials for modern dogs.
          </h1>
          <p className="mt-5 max-w-[58ch] text-[16px] leading-[1.85] text-fetch-muted sm:text-[17px]">
            Lupine &amp; Leash is built around a simple idea: everyday dog accessories can be comfortable, durable, and beautifully
            understated — designed to fit naturally into the way you live and walk together.
          </p>
        </div>
      </div>
      <div className="section-container grid gap-12 py-14 lg:grid-cols-[minmax(0,0.42fr)_minmax(0,0.58fr)] lg:gap-16 lg:py-20">
        <div>
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Our focus</p>
          <h2 className="mt-3 text-[1.8rem] font-semibold leading-[1.1] tracking-[-0.025em] text-fetch-ink">Made with intention</h2>
        </div>
        <div className="space-y-8 text-[16px] leading-[1.85] text-fetch-muted">
          <p>We care about the details that show up every day: comfortable fits, practical hardware, easy handling, and a calm visual palette that feels premium without being loud.</p>
          <p>As the collection grows, we’ll keep refining materials, sizing guidance, and care information so customers can choose confidently — especially for a dropshipping model where clear expectations matter.</p>
          <div className="border-t border-fetch-ink/[0.08] pt-8">
            <h3 className="text-[15px] font-semibold text-fetch-ink">What you can expect</h3>
            <ul className="mt-4 space-y-3">
              {['Thoughtful product presentation', 'Clear shipping and returns information', 'Honest support and order updates', 'A calm, premium shopping experience'].map((item) => (
                <li key={item} className="flex gap-3"><span className="mt-[0.65rem] h-1 w-1 shrink-0 rounded-full bg-fetch-ink/25" />{item}</li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </StoreLayout>
  );
}
