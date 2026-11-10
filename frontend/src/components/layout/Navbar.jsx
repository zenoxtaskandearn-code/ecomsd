import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { Search, ShoppingBag, Menu, X, User } from 'lucide-react';
import { useCart } from '../../context/CartContext.jsx';
import { useAuth } from '../../context/AuthContext.jsx';

const navLinks = [
  { label: 'Shop Essentials', to: '/shop' },
  { label: 'Supplies', to: '/shop?category=lifestyle' },
  { label: 'About Us', to: '/about' },
  { label: 'Blog', to: '/blog' },
  { label: 'Contact', to: '/contact' },
];

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const { count } = useCart();
  const { isAuthenticated } = useAuth();

  return (
    <header className="bg-fetch-lavender-soft">
      <div className="section-container">
        <div className="flex h-[68px] items-center justify-between gap-4">
          <Link to="/" className="shrink-0 text-[1.65rem] font-semibold tracking-tightish text-fetch-ink">
            Lupine &amp; Leash
          </Link>

          <nav className="hidden items-center gap-7 md:flex" aria-label="Primary">
            {navLinks.map((item) => (
              <NavLink
                key={item.label}
                to={item.to}
                className={({ isActive }) =>
                  `text-[14px] transition-colors hover:text-fetch-ink ${
                    isActive ? 'text-fetch-ink' : 'text-fetch-ink/80'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-2 sm:gap-3">
            <Link
              to="/search"
              className="inline-flex h-9 w-9 items-center justify-center rounded-full text-fetch-ink/80 transition-colors hover:bg-white/40 hover:text-fetch-ink"
              aria-label="Search"
            >
              <Search size={19} strokeWidth={1.6} />
            </Link>
            <Link
              to={isAuthenticated ? '/account' : '/login'}
              className="inline-flex h-9 w-9 items-center justify-center rounded-full text-fetch-ink/80 transition-colors hover:bg-white/40 hover:text-fetch-ink"
              aria-label={isAuthenticated ? 'Account' : 'Sign in'}
            >
              <User size={19} strokeWidth={1.6} />
            </Link>
            <Link
              to="/cart"
              className="relative inline-flex h-9 w-9 items-center justify-center rounded-full text-fetch-ink/80 transition-colors hover:bg-white/40 hover:text-fetch-ink"
              aria-label={`Shopping cart${count ? `, ${count} items` : ''}`}
            >
              <ShoppingBag size={19} strokeWidth={1.6} />
              {count > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-fetch-dark px-1 text-[10px] font-medium leading-none text-white">
                  {count > 99 ? '99+' : count}
                </span>
              )}
            </Link>
            <button
              type="button"
              className="inline-flex h-9 w-9 items-center justify-center rounded-full text-fetch-ink/80 transition-colors hover:bg-white/40 hover:text-fetch-ink md:hidden"
              aria-label={open ? 'Close menu' : 'Open menu'}
              aria-expanded={open}
              onClick={() => setOpen((v) => !v)}
            >
              {open ? <X size={20} strokeWidth={1.6} /> : <Menu size={20} strokeWidth={1.6} />}
            </button>
          </div>
        </div>

        {open && (
          <nav className="border-t border-fetch-ink/10 py-4 md:hidden" aria-label="Mobile">
            <ul className="flex flex-col gap-3">
              {navLinks.map((item) => (
                <li key={item.label}>
                  <NavLink
                    to={item.to}
                    className="block text-[15px] text-fetch-ink/85"
                    onClick={() => setOpen(false)}
                  >
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        )}
      </div>
    </header>
  );
}
