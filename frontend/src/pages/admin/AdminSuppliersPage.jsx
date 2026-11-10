import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import AdminLayout, { AdminCard, AdminButton } from '../../components/admin/AdminLayout.jsx';
import { adminSuppliers } from '../../utils/adminDemo.js';
import { apiEnabled } from '../../services/api.js';
import * as adminApi from '../../services/adminApi.js';

const inputClass = 'h-9 w-full rounded-lg border border-[#d1d5db] bg-white px-3 text-[13px] focus:border-[#9ca3af] focus:outline-none';

export default function AdminSuppliersPage() {
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState([]);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ name:'', code:'', contactEmail:'', status:'ACTIVE', apiMode:'MANUAL', notes:'' });

  const load = async () => {
    setLoading(true);
    try { setRows(apiEnabled ? await adminApi.listSuppliers() : adminSuppliers); }
    catch { setRows(adminSuppliers); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const save = async (e) => {
    e.preventDefault();
    if (!apiEnabled) { setEditing(null); return; }
    setSaving(true);
    try { await adminApi.saveSupplier(form, editing?.id); setEditing(null); setForm({ name:'', code:'', contactEmail:'', status:'ACTIVE', apiMode:'MANUAL', notes:'' }); await load(); }
    finally { setSaving(false); }
  };

  return (
    <AdminLayout title="Suppliers" subtitle="Dropship partners and fulfillment mappings (manual fallback supported)" actions={<AdminButton onClick={()=>{ setEditing('new'); setForm({ name:'', code:'', contactEmail:'', status:'ACTIVE', apiMode:'MANUAL', notes:'' }); }}><Plus size={15}/> Add supplier</AdminButton>}>
      {editing && (
        <AdminCard title={editing==='new'?'Add supplier':'Edit supplier'}>
          <form className="grid max-w-[720px] gap-4 sm:grid-cols-2" onSubmit={save}>
            <label className="block"><span className="text-[13px] font-medium text-[#374151]">Name</span><input required className={`${inputClass} mt-1.5`} value={form.name} onChange={(e)=>setForm(f=>({...f,name:e.target.value}))} /></label>
            <label className="block"><span className="text-[13px] font-medium text-[#374151]">Code</span><input required className={`${inputClass} mt-1.5`} value={form.code} onChange={(e)=>setForm(f=>({...f,code:e.target.value}))} /></label>
            <label className="block"><span className="text-[13px] font-medium text-[#374151]">Contact email</span><input type="email" className={`${inputClass} mt-1.5`} value={form.contactEmail||''} onChange={(e)=>setForm(f=>({...f,contactEmail:e.target.value}))} /></label>
            <label className="block"><span className="text-[13px] font-medium text-[#374151]">API mode</span><select className={`${inputClass} mt-1.5`} value={form.apiMode||'MANUAL'} onChange={(e)=>setForm(f=>({...f,apiMode:e.target.value}))}><option value="MANUAL">MANUAL</option><option value="API">API (adapter-ready)</option></select></label>
            <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Notes</span><input className={`${inputClass} mt-1.5`} value={form.notes||''} onChange={(e)=>setForm(f=>({...f,notes:e.target.value}))} /></label>
            <div className="flex gap-2 sm:col-span-2"><AdminButton type="submit" loading={saving}>Save supplier</AdminButton><AdminButton variant="secondary" onClick={()=>setEditing(null)} disabled={saving}>Cancel</AdminButton></div>
            {!apiEnabled && <p className="text-[12px] text-[#6b7280] sm:col-span-2">Demo mode: connect backend to persist suppliers.</p>}
          </form>
        </AdminCard>
      )}
      <AdminCard title="Suppliers">
        {loading ? <div className="space-y-2">{[1,2].map(i=><div key={i} className="h-12 animate-pulse rounded-xl bg-[#f3f4f6]" />)}</div> : (
          <div className="overflow-x-auto"><table className="min-w-full text-left text-[13px]"><thead><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="py-2 pr-4 font-medium">Name</th><th className="py-2 pr-4 font-medium">Code</th><th className="py-2 pr-4 font-medium">Mode</th><th className="py-2 pr-4 font-medium">Status</th><th className="py-2 font-medium"></th></tr></thead>
            <tbody>{rows.map((s,idx)=>(<tr key={s.id||idx} className="border-b border-[#f3f4f6] last:border-0"><td className="py-3 pr-4 font-medium text-[#111827]">{s.name}</td><td className="py-3 pr-4 text-[#6b7280]">{s.code}</td><td className="py-3 pr-4">{s.apiMode || s.mode}</td><td className="py-3 pr-4">{s.status}</td><td className="py-3"><AdminButton variant="ghost" className="h-8 px-2.5" onClick={()=>{ setEditing(s); setForm({ name:s.name||'', code:s.code||'', contactEmail:s.contactEmail||'', status:s.status||'ACTIVE', apiMode:s.apiMode||s.mode||'MANUAL', notes:s.notes||'' }); }}>Edit</AdminButton></td></tr>))}</tbody></table></div>
        )}
      </AdminCard>
    </AdminLayout>
  );
}
