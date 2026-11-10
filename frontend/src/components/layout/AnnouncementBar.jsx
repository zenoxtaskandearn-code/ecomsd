import { Instagram, Facebook } from 'lucide-react';

export default function AnnouncementBar() {
  return (
    <div className="bg-fetch-beige text-fetch-ink">
      <div className="section-container relative flex h-[30px] items-center justify-center">
        <p className="text-center text-[12px] leading-none tracking-[0.01em] text-fetch-ink/90">
          Hassle-free returns · 30-day returns
        </p>
        <div className="absolute right-5 hidden items-center gap-3 sm:right-6 sm:flex lg:right-8" aria-label="Social links">
          <a href="#" className="text-fetch-ink/70 transition-colors hover:text-fetch-ink" aria-label="Instagram">
            <Instagram size={14} strokeWidth={1.75} />
          </a>
          <a href="#" className="text-fetch-ink/70 transition-colors hover:text-fetch-ink" aria-label="Facebook">
            <Facebook size={14} strokeWidth={1.75} />
          </a>
        </div>
      </div>
    </div>
  );
}
