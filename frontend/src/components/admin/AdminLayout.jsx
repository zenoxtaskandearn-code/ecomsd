import { NavLink, Link } from 'react-router-dom';
import { LayoutDashboard, Package, Tags, ShoppingCart, Users, Truck, LogOut, ExternalLink, Menu, X } from 'lucide-react';
import { useState } from 'react';
import { useAdminAuth } from '../../context/AdminAuthContext.jsx';

const nav = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/admin/products', label: 'Products', icon: Package },
  { to: '/admin/categories', label: 'Categories', icon: Tags },
  { to: '/admin/orders', label: 'Orders', icon: ShoppingCart },
  { to: '/admin/customers', label: 'Customers', icon: Users },
  { to: '/admin/suppliers', label: 'Suppliers', icon: Truck },
];

export function AdminCard({ title, description, action, children, className = '' }) {
  return (
    <section className={`rounded-2xl border border-[#e5e7eb] bg-white shadow-[0_1px_2px_rgba(16,24,40,0.04)] ${className}`}>
      {(title || action) && (
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-[#eef0f2] px-5 py-4 sm:px-6">
          <div>
            {title && <h2 className="text-[15px] font-semibold tracking-[-0.01em] text-[#111827]">{title}</h2>}
            {description && <p className="mt-0.5 text-[13px] leading-5 text-[#6b7280]">{description}</p>}
          </div>
          {action}
        </div>
      )}
      <div className={title || action ? 'p-5 sm:p-6' : 'p-5 sm:p-6'}>{children}</div>
    </section>
  );
}

export default function AdminLayout({ title, subtitle, actions, children }) {
  const { admin, logout } = useAdminAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const Sidebar = ({ onNavigate }) => (
    <div className="flex h-full flex-col bg-[#f6f6f7]">
      <div className="flex h-14 items-center border-b border-[#e3e3e3] px-4">
        <Link to="/admin" className="text-[15px] font-semibold tracking-[-0.02em] text-[#111827]">Lupine &amp; Leash Admin</Link>
      </div>
      <nav className="flex-1 space-y-0.5 p-2" aria-label="Admin">
        {nav.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            onClick={onNavigate}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2 text-[13px] font-medium transition ${
                isActive ? 'bg-white text-[#111827] shadow-[0_1px_2px_rgba(16,24,40,0.06)]' : 'text-[#4b5563] hover:bg-white/70 hover:text-[#111827]'
              }`
            }
          >
            <Icon size={17} strokeWidth={1.7} aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-[#e3e3e3] p-3">
        <Link to="/" className="mb-1 flex items-center gap-2 rounded-xl px-3 py-2 text-[13px] text-[#4b5563] hover:bg-white/70">
          <ExternalLink size={15} strokeWidth={1.7} /> View store
        </Link>
        <div className="px-3 py-2">
          <p className="truncate text-[13px] font-medium text-[#111827]">{admin?.name || 'Admin'}</p>
          <p className="truncate text-[12px] text-[#6b7280]">{admin?.email}</p>
        </div>
        <button type="button" onClick={logout} className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-[13px] text-[#4b5563] hover:bg-white/70">
          <LogOut size={15} strokeWidth={1.7} /> Sign out
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-[#f1f1f1] text-[#111827]">
      <div className="hidden lg:fixed lg:inset-y-0 lg:z-30 lg:flex lg:w-[240px] lg:flex-col lg:border-r lg:border-[#e3e3e3]">
        <Sidebar />
      </div>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button type="button" className="absolute inset-0 bg-black/20" aria-label="Close menu" onClick={() => setMobileOpen(false)} />
          <div className="absolute inset-y-0 left-0 w-[280px] border-r border-[#e3e3e3] shadow-xl">
            <Sidebar onNavigate={() => setMobileOpen(false)} />
          </div>
        </div>
      )}

      <div className="lg:pl-[240px]">
        <header className="sticky top-0 z-20 flex h-14 items-center justify-between border-b border-[#e3e3e3] bg-white/95 px-4 backdrop-blur sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <button type="button" className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-[#4b5563] hover:bg-[#f3f4f6] lg:hidden" aria-label="Open menu" onClick={() => setMobileOpen(true)}>
              <Menu size={18} strokeWidth={1.7} />
            </button>
            <div>
              {title && <h1 className="text-[16px] font-semibold tracking-[-0.02em] text-[#111827]">{title}</h1>}
              {subtitle && <p className="hidden text-[12px] text-[#6b7280] sm:block">{subtitle}</p>}
            </div>
          </div>
          <div className="flex items-center gap-2">{actions}</div>
        </header>
        <main className="p-4 sm:p-6 lg:p-8">
          <div className="mx-auto max-w-[1280px] space-y-5">{children}</div>
        </main>
      </div>
    </div>
  );
}

export function AdminButton({ children, variant = 'primary', loading, className = '', ...props }) {
  const styles = {
    primary: 'bg-[#1f2937] text-white hover:bg-[#111827] border-transparent',
    secondary: 'bg-white text-[#374151] border-[#d1d5db] hover:bg-[#f9fafb]',
    ghost: 'bg-transparent text-[#4b5563] border-transparent hover:bg-[#f3f4f6]',
  };
  return (
    <button
      type="button"
      disabled={loading || props.disabled}
      className={`inline-flex h-9 items-center justify-center gap-2 rounded-lg border px-3.5 text-[13px] font-medium transition disabled:opacity-55 ${styles[variant]} ${className}`}
      {...props}
    >
      {loading && <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current/25 border-t-current" />}
      {children}
    </button>
  );
}
