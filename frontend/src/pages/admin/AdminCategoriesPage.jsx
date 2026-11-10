import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import AdminLayout, { AdminCard, AdminButton } from '../../components/admin/AdminLayout.jsx';
import { adminCategories } from '../../utils/adminDemo.js';
import { apiEnabled } from '../../services/api.js';
import * as adminApi from '../../services/adminApi.js';

const inputClass = 'h-9 w-full rounded-lg border border-[#d1d5db] bg-white px-3 text-[13px] focus:border-[#9ca3af] focus:outline-none';

export default function AdminCategoriesPage() {
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState([]);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ name: '', slug: '', description: '', active: true, sortOrder: 0 });

  const load = async () => {
    setLoading(true);
    try { setRows(apiEnabled ? await adminApi.listCategories() : adminCategories.map((c,i)=>({ ...c, id:i+1, active:true }))); }
    catch { setRows(adminCategories); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  const save = async (e) => {
    e.preventDefault(); setSaving(true); setError('');
    try {
      if (apiEnabled) await adminApi.saveCategory(form, editing?.id);
      setEditing(null); setForm({ name:'', slug:'', description:'', active:true, sortOrder:0 }); await load();
    } catch (e2) { setError(e2?.response?.data?.error?.message || 'Could not save category.'); }
    finally { setSaving(false); }
  };

  return (
    <AdminLayout title="Categories" subtitle="Organize products and storefront collections" actions={<AdminButton onClick={() => { setEditing('new'); setForm({ name:'', slug:'', description:'', active:true, sortOrder:0 }); }}><Plus size={15} /> Add category</AdminButton>}>
      {editing && (
        <AdminCard title={editing === 'new' ? 'Add category' : 'Edit category'} description="Create or update a storefront category">
          <form className="grid max-w-[640px] gap-4 sm:grid-cols-2" onSubmit={save}>
            <label className="block"><span className="text-[13px] font-medium text-[#374151]">Name</span><input required className={`${inputClass} mt-1.5`} value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} /></label>
            <label className="block"><span className="text-[13px] font-medium text-[#374151]">Slug</span><input required className={`${inputClass} mt-1.5`} value={form.slug} onChange={(e) => setForm((f) => ({ ...f, slug: e.target.value }))} placeholder="walking-kits" /></label>
            <label className="block sm:col-span-2"><span className="text-[13px] font-medium text-[#374151]">Description</span><input className={`${inputClass} mt-1.5`} value={form.description||''} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} /></label>
            <label className="block flex items-center gap-2 text-[13px] sm:col-span-2"><input type="checkbox" checked={form.active !== false} onChange={(e)=>setForm(f=>({...f,active:e.target.checked}))} /> Active</label>
            {error && <p className="text-[13px] text-red-700 sm:col-span-2">{error}</p>}
            <div className="flex gap-2 sm:col-span-2"><AdminButton type="submit" loading={saving}>Save category</AdminButton><AdminButton variant="secondary" onClick={() => setEditing(null)} disabled={saving}>Cancel</AdminButton></div>
            {!apiEnabled && <p className="text-[12px] text-[#6b7280] sm:col-span-2">Demo mode: connect backend to persist categories.</p>}
          </form>
        </AdminCard>
      )}
      <AdminCard title="All categories">
        {loading ? <div className="space-y-2">{[1,2,3].map((i) => <div key={i} className="h-12 animate-pulse rounded-xl bg-[#f3f4f6]" />)}</div> : (
          <div className="overflow-x-auto"><table className="min-w-full text-left text-[13px]"><thead><tr className="border-b border-[#eef0f2] text-[#6b7280]"><th className="py-2 pr-4 font-medium">Name</th><th className="py-2 pr-4 font-medium">Slug</th><th className="py-2 pr-4 font-medium">Status</th><th className="py-2 font-medium"></th></tr></thead>
            <tbody>{rows.map((c) => (<tr key={c.id || c.slug} className="border-b border-[#f3f4f6] last:border-0"><td className="py-3 pr-4 font-medium text-[#111827]">{c.name}</td><td className="py-3 pr-4 text-[#6b7280]">{c.slug}</td><td className="py-3 pr-4"><span className={`rounded-full px-2 py-1 text-[12px] ${c.active === false ? 'bg-[#f3f4f6] text-[#374151]' : 'bg-[#ecfdf3] text-[#166534]'}`}>{c.active === false ? 'Inactive' : 'Active'}</span></td><td className="py-3"><AdminButton variant="ghost" className="h-8 px-2.5" onClick={() => { setEditing(c); setForm({ name:c.name||'', slug:c.slug||'', description:c.description||'', active:c.active!==false, sortOrder:c.sortOrder||0 }); }}>Edit</AdminButton></td></tr>))}</tbody>
          </table></div>
        )}
      </AdminCard>
    </AdminLayout>
  );
}
