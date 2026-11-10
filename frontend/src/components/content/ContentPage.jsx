import { useEffect, useState } from 'react';
import StoreLayout from '../layout/StoreLayout.jsx';
import { Skeleton } from '../ui/Skeleton.jsx';

export default function ContentPage({ eyebrow, title, intro, children, updated }) {
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    const t = setTimeout(() => setLoading(false), 300);
    return () => clearTimeout(t);
  }, []);

  return (
    <StoreLayout>
      <div className="border-b border-fetch-ink/[0.06] bg-[#FBFAF8]">
        <div className="section-container py-12 sm:py-14">
          {eyebrow && <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">{eyebrow}</p>}
          <h1 className="mt-3 max-w-[16ch] text-[2.35rem] font-semibold leading-[1.04] tracking-[-0.03em] text-fetch-ink sm:text-[2.9rem]">{title}</h1>
          {intro && <p className="mt-4 max-w-[62ch] text-[16px] leading-[1.85] text-fetch-muted sm:text-[17px]">{intro}</p>}
          {updated && <p className="mt-4 text-[13px] text-fetch-muted/90">Last updated: {updated}</p>}
        </div>
      </div>
      <div className="section-container py-12 sm:py-14 lg:py-16">
        {loading ? (
          <div className="max-w-[760px] space-y-4">
            <Skeleton className="h-5 w-[90%]" />
            <Skeleton className="h-5 w-[82%]" />
            <Skeleton className="h-5 w-[70%]" />
            <Skeleton className="mt-8 h-32 w-full" />
          </div>
        ) : (
          <div className="max-w-[760px]">{children}</div>
        )}
      </div>
    </StoreLayout>
  );
}

export function PolicySection({ title, children }) {
  return (
    <section className="border-t border-fetch-ink/[0.08] py-8 first:border-t-0 first:pt-0">
      <h2 className="text-[1.15rem] font-semibold tracking-[-0.02em] text-fetch-ink">{title}</h2>
      <div className="mt-3 space-y-3 text-[15px] leading-[1.85] text-fetch-muted sm:text-[16px]">{children}</div>
    </section>
  );
}
