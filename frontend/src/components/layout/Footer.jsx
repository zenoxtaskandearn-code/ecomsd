import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="border-t border-fetch-ink/[0.08] bg-[#F5F3EF]" id="contact">
      <div className="section-container grid gap-10 py-16 sm:py-20 md:grid-cols-[1.3fr_1fr_1fr_1fr] md:gap-8">
        <div>
          <p className="text-[1.7rem] font-semibold tracking-[-0.03em] text-fetch-ink">Lupine &amp; Leash</p>
          <p className="mt-4 max-w-[280px] text-[14px] leading-[1.75] text-fetch-muted">
            Premium accessories for modern dogs, designed with care and a lighter footprint.
          </p>
        </div>
        <div>
          <h3 className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Shop</h3>
          <ul className="mt-5 space-y-3 text-[14px] text-fetch-muted">
            <li><Link to="/shop" className="transition duration-300 hover:text-fetch-ink">Shop Essentials</Link></li>
            <li><Link to="/shop" className="transition duration-300 hover:text-fetch-ink">Supplies</Link></li>
            <li><Link to="/shop" className="transition duration-300 hover:text-fetch-ink">Walking kits</Link></li>
          </ul>
        </div>
        <div>
          <h3 className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Company</h3>
          <ul className="mt-5 space-y-3 text-[14px] text-fetch-muted">
            <li><Link to="/about" className="transition duration-300 hover:text-fetch-ink">About Us</Link></li>
            <li><Link to="/blog" className="transition duration-300 hover:text-fetch-ink">Blog</Link></li>
            <li><Link to="/contact" className="transition duration-300 hover:text-fetch-ink">Contact</Link></li>
          </ul>
        </div>
        <div>
          <h3 className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Help</h3>
          <ul className="mt-5 space-y-3 text-[14px] text-fetch-muted">
            <li><Link to="/faq" className="transition duration-300 hover:text-fetch-ink">FAQ</Link></li>
            <li><Link to="/policies/shipping" className="transition duration-300 hover:text-fetch-ink">Shipping</Link></li>
            <li><Link to="/policies/returns" className="transition duration-300 hover:text-fetch-ink">Returns</Link></li>
            <li><Link to="/policies/privacy" className="transition duration-300 hover:text-fetch-ink">Privacy</Link></li>
            <li><Link to="/policies/terms" className="transition duration-300 hover:text-fetch-ink">Terms</Link></li>
          </ul>
        </div>
      </div>
      <div className="border-t border-fetch-ink/[0.08]">
        <div className="section-container flex flex-col gap-2 py-6 text-[13px] text-fetch-muted/90 sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} Lupine &amp; Leash. All rights reserved.</p>
          <p>Premium pet accessories, thoughtfully made.</p>
        </div>
      </div>
    </footer>
  );
}
