import { useEffect, useState } from 'react';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import AccountShell from '../components/account/AccountShell.jsx';
import Button from '../components/ui/Button.jsx';
import { Skeleton } from '../components/ui/Skeleton.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { demoAddresses } from '../utils/accountDemo.js';
import { apiEnabled } from '../services/api.js';
import * as addressApi from '../services/addressApi.js';

const inputClass = 'h-11 w-full border border-fetch-ink/12 bg-white px-3.5 text-[15px] text-fetch-ink focus:border-fetch-ink/25 focus:outline-none';
const emptyAddr = { fullName: '', line1: '', line2: '', city: '', state: '', postalCode: '', countryCode: 'US', phone: '', isDefault: false, type: 'SHIPPING' };

export default function AccountAddressesPage() {
  const { user, updateProfile } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [profile, setProfile] = useState({ firstName: '', lastName: '', email: '', phone: '' });
  const [addresses, setAddresses] = useState([]);
  const [editing, setEditing] = useState(null); // null | 'new' | address
  const [addrForm, setAddrForm] = useState(emptyAddr);
  const [addrSaving, setAddrSaving] = useState(false);
  const [error, setError] = useState('');

  const loadAddresses = async () => {
    if (apiEnabled) setAddresses(await addressApi.list());
    else setAddresses(demoAddresses.map((a)=>({ ...a, fullName: a.name, countryCode: a.country === 'United States' ? 'US' : 'US' })));
  };

  useEffect(() => {
    setProfile({ firstName: user?.firstName || '', lastName: user?.lastName || '', email: user?.email || '', phone: user?.phone || '' });
    let mounted = true;
    (async () => { try { await loadAddresses(); } catch {} finally { if (mounted) setLoading(false); } })();
    return () => { mounted = false; };
  }, [user]);

  const onSaveProfile = async (e) => {
    e.preventDefault(); setSaving(true); setSaved(false); setError('');
    try { await updateProfile(profile); setSaved(true); setTimeout(() => setSaved(false), 1800); }
    catch (e2) { setError(e2?.response?.data?.error?.message || 'Could not save profile.'); }
    finally { setSaving(false); }
  };

  const saveAddress = async (e) => {
    e.preventDefault();
    if (!apiEnabled) { setEditing(null); return; }
    setAddrSaving(true); setError('');
    try {
      if (editing?.id) await addressApi.update(editing.id, addrForm); else await addressApi.create(addrForm);
      await loadAddresses(); setEditing(null); setAddrForm(emptyAddr);
    } catch (e2) { setError(e2?.response?.data?.error?.message || 'Could not save address.'); }
    finally { setAddrSaving(false); }
  };

  const removeAddress = async (id) => {
    if (!apiEnabled) return;
    if (!confirm('Remove this address?')) return;
    try { await addressApi.remove(id); await loadAddresses(); } catch (e) { setError(e?.response?.data?.error?.message || 'Could not remove address.'); }
  };

  return (
    <StoreLayout>
      <AccountShell title="Profile & addresses" description="Update your account details and saved shipping addresses.">
        {loading ? (
          <div className="space-y-4"><Skeleton className="h-12 w-full" /><Skeleton className="h-12 w-full" /><Skeleton className="h-40 w-full" /></div>
        ) : (
          <div className="space-y-10">
            {error && <p className="text-[14px] text-red-700">{error}</p>}
            <form className="max-w-[620px] border border-fetch-ink/[0.08] bg-[#FBFAF8] p-5 sm:p-6" onSubmit={onSaveProfile}>
              <h2 className="text-[1.05rem] font-semibold tracking-[-0.02em] text-fetch-ink">Profile</h2>
              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">First name</span><input className={`${inputClass} mt-1.5`} value={profile.firstName} onChange={(e) => setProfile((p) => ({ ...p, firstName: e.target.value }))} /></label>
                <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Last name</span><input className={`${inputClass} mt-1.5`} value={profile.lastName} onChange={(e) => setProfile((p) => ({ ...p, lastName: e.target.value }))} /></label>
                <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-fetch-ink/80">Email</span><input type="email" className={`${inputClass} mt-1.5`} value={profile.email} onChange={(e) => setProfile((p) => ({ ...p, email: e.target.value }))} /></label>
                <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-fetch-ink/80">Phone</span><input className={`${inputClass} mt-1.5`} value={profile.phone} onChange={(e) => setProfile((p) => ({ ...p, phone: e.target.value }))} /></label>
              </div>
              <div className="mt-5 flex items-center gap-3"><Button type="submit" loading={saving}>{saved ? 'Saved' : 'Save profile'}</Button>{saved && <span className="text-[14px] text-fetch-muted">Updated.</span>}</div>
            </form>

            <div>
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-[1.05rem] font-semibold tracking-[-0.02em] text-fetch-ink">Saved addresses</h2>
                <Button variant="secondary" className="h-10 px-4 text-[14px]" onClick={() => { setEditing('new'); setAddrForm({ ...emptyAddr, fullName: [profile.firstName, profile.lastName].filter(Boolean).join(' ') }); }}>Add address</Button>
              </div>
              {editing && (
                <form className="mt-4 border border-fetch-ink/[0.08] bg-[#FBFAF8] p-5" onSubmit={saveAddress}>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <label className="block sm:col-span-2"><span className="text-[13px] font-medium">Full name</span><input required className={`${inputClass} mt-1.5`} value={addrForm.fullName} onChange={(e)=>setAddrForm(f=>({...f,fullName:e.target.value}))} /></label>
                    <label className="block sm:col-span-2"><span className="text-[13px] font-medium">Address line 1</span><input required className={`${inputClass} mt-1.5`} value={addrForm.line1} onChange={(e)=>setAddrForm(f=>({...f,line1:e.target.value}))} /></label>
                    <label className="block sm:col-span-2"><span className="text-[13px] font-medium">Address line 2</span><input className={`${inputClass} mt-1.5`} value={addrForm.line2||''} onChange={(e)=>setAddrForm(f=>({...f,line2:e.target.value}))} /></label>
                    <label className="block"><span className="text-[13px] font-medium">City</span><input required className={`${inputClass} mt-1.5`} value={addrForm.city} onChange={(e)=>setAddrForm(f=>({...f,city:e.target.value}))} /></label>
                    <label className="block"><span className="text-[13px] font-medium">State</span><input required className={`${inputClass} mt-1.5`} value={addrForm.state} onChange={(e)=>setAddrForm(f=>({...f,state:e.target.value}))} /></label>
                    <label className="block"><span className="text-[13px] font-medium">ZIP code</span><input required className={`${inputClass} mt-1.5`} value={addrForm.postalCode} onChange={(e)=>setAddrForm(f=>({...f,postalCode:e.target.value}))} /></label>
                    <label className="block"><span className="text-[13px] font-medium">Country</span><select className={`${inputClass} mt-1.5`} value={addrForm.countryCode} onChange={(e)=>setAddrForm(f=>({...f,countryCode:e.target.value}))}><option value="US">United States</option></select></label>
                  </div>
                  <label className="mt-4 flex items-center gap-2 text-[14px]"><input type="checkbox" checked={!!addrForm.isDefault} onChange={(e)=>setAddrForm(f=>({...f,isDefault:e.target.checked}))} /> Default address</label>
                  <div className="mt-4 flex gap-2"><Button type="submit" loading={addrSaving}>Save address</Button><Button variant="secondary" onClick={()=>setEditing(null)} disabled={addrSaving}>Cancel</Button></div>
                  {!apiEnabled && <p className="mt-2 text-[12px] text-fetch-muted">Demo mode: connect backend to persist addresses.</p>}
                </form>
              )}
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                {addresses.map((addr) => (
                  <div key={addr.id} className="border border-fetch-ink/[0.08] p-5">
                    <div className="flex items-start justify-between gap-3">
                      <p className="text-[14px] font-medium text-fetch-ink">{addr.fullName || addr.name || 'Address'}</p>
                      {(addr.isDefault || addr.defaultAddress) && <span className="bg-fetch-lavender-soft px-2 py-1 text-[11px] font-medium uppercase tracking-[0.08em] text-fetch-ink/70">Default</span>}
                    </div>
                    <address className="mt-3 text-[14px] not-italic leading-[1.75] text-fetch-muted">
                      {addr.fullName || addr.name}<br />{addr.line1}{addr.line2 ? <><br />{addr.line2}</> : null}<br />{addr.city}, {addr.state} {addr.postalCode}<br />{addr.countryCode || addr.country || 'US'}
                    </address>
                    {apiEnabled && <div className="mt-4 flex gap-3 text-[13px]"><button className="underline decoration-fetch-ink/20" onClick={()=>{ setEditing(addr); setAddrForm({ fullName: addr.fullName||'', line1: addr.line1||'', line2: addr.line2||'', city: addr.city||'', state: addr.state||'', postalCode: addr.postalCode||'', countryCode: addr.countryCode||'US', phone: addr.phone||'', isDefault: !!(addr.isDefault||addr.defaultAddress), type: addr.type||'SHIPPING' }); }}>Edit</button><button className="text-fetch-muted hover:text-fetch-ink" onClick={()=>removeAddress(addr.id)}>Remove</button></div>}
                  </div>
                ))}
                {addresses.length === 0 && !editing && <p className="text-[14px] text-fetch-muted">No saved addresses yet.</p>}
              </div>
            </div>
          </div>
        )}
      </AccountShell>
    </StoreLayout>
  );
}
