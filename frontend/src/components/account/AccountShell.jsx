import { NavLink } from 'react-router-dom';
import { User, MapPin, Package, LogOut } from 'lucide-react';
import { useAuth } from '../../context/AuthContext.jsx';

const links = [
  { to: '/account', label: 'Overview', icon: User, end: true },
  { to: '/account/addresses', label: 'Profile & addresses', icon: MapPin },
  { to: '/account/orders', label: 'Order history', icon: Package },
];

export default function AccountShell({ title, description, children }) {
  const { user, logout } = useAuth();

  return (
    <div className="section-container py-10 lg:py-12">
      <div className="lg:grid lg:grid-cols-[240px_minmax(0,1fr)] lg:gap-10 xl:gap-14">
        <aside className="mb-8 lg:mb-0">
          <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-5 lg:sticky lg:top-24">
            <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Account</p>
            <p className="mt-2 text-[15px] font-medium text-fetch-ink">{user?.firstName || 'Customer'}</p>
            <p className="mt-0.5 break-all text-[13px] text-fetch-muted">{user?.email}</p>
            <nav className="mt-6 space-y-1" aria-label="Account">
              {links.map(({ to, label, icon: Icon, end }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={end}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 text-[14px] transition ${
                      isActive ? 'bg-white text-fetch-ink shadow-[0_0_0_1px_rgba(23,23,23,0.06)]' : 'text-fetch-ink/70 hover:bg-white/70 hover:text-fetch-ink'
                    }`
                  }
                >
                  <Icon size={16} strokeWidth={1.6} aria-hidden="true" />
                  {label}
                </NavLink>
              ))}
            </nav>
            <button
              type="button"
              onClick={logout}
              className="mt-5 flex w-full items-center gap-3 px-3 py-2.5 text-[14px] text-fetch-ink/65 transition hover:text-fetch-ink"
            >
              <LogOut size={16} strokeWidth={1.6} aria-hidden="true" />
              Sign out
            </button>
            <p className="mt-4 border-t border-fetch-ink/[0.08] pt-4 text-[12px] leading-5 text-fetch-muted">
              Demo account area — connect to secure backend auth and customer APIs in later phases.
            </p>
          </div>
        </aside>
        <section>
          <div className="mb-8 border-b border-fetch-ink/[0.08] pb-6">
            <h1 className="text-[2rem] font-semibold leading-[1.05] tracking-[-0.03em] text-fetch-ink sm:text-[2.35rem]">{title}</h1>
            {description && <p className="mt-2 max-w-[60ch] text-[15px] leading-[1.75] text-fetch-muted">{description}</p>}
          </div>
          {children}
        </section>
      </div>
    </div>
  );
}
