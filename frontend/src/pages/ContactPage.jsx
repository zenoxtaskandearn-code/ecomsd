import { useState } from 'react';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import Button from '../components/ui/Button.jsx';
import { submit } from '../services/contactApi.js';

const inputClass = 'h-11 w-full border border-fetch-ink/12 bg-white px-3.5 text-[15px] text-fetch-ink placeholder:text-fetch-muted/70 focus:border-fetch-ink/25 focus:outline-none';

export default function ContactPage() {
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ name: '', email: '', subject: '', message: '' });
  const update = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setSending(true); setError('');
    try {
      await submit(form);
      setSent(true);
    } catch (err) {
      setError(err?.response?.data?.error?.message || 'Could not send your message. Please try again.');
    } finally {
      setSending(false);
    }
  };

  return (
    <StoreLayout>
      <div className="border-b border-fetch-ink/[0.06] bg-[#FBFAF8]">
        <div className="section-container py-12 sm:py-16">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Contact</p>
          <h1 className="mt-3 text-[2.4rem] font-semibold leading-[1.03] tracking-[-0.03em] text-fetch-ink sm:text-[3rem]">We’re here to help</h1>
          <p className="mt-4 max-w-[56ch] text-[16px] leading-[1.8] text-fetch-muted">Questions about sizing, orders, or shipping? Send a message and we’ll get back to you.</p>
        </div>
      </div>
      <div className="section-container grid gap-10 py-14 lg:grid-cols-[minmax(0,0.38fr)_minmax(0,0.62fr)] lg:gap-16">
        <aside>
          <h2 className="text-[1.1rem] font-semibold tracking-[-0.02em] text-fetch-ink">Support details</h2>
          <dl className="mt-5 space-y-5 text-[14px] leading-[1.75] text-fetch-muted">
            <div><dt className="font-medium text-fetch-ink">Email</dt><dd className="mt-1">support@fetch.example (configure before launch)</dd></div>
            <div><dt className="font-medium text-fetch-ink">Response time</dt><dd className="mt-1">Typically within 1–2 business days (set realistic expectations for your operations).</dd></div>
            <div><dt className="font-medium text-fetch-ink">Order help</dt><dd className="mt-1">Include your order reference if you’re contacting us about an existing order.</dd></div>
          </dl>
        </aside>
        <section>
          {sent ? (
            <div className="border border-fetch-ink/[0.08] bg-[#FBFAF8] p-6 sm:p-8">
              <h2 className="text-[1.2rem] font-semibold tracking-[-0.02em] text-fetch-ink">Message received</h2>
              <p className="mt-2 text-[15px] leading-[1.75] text-fetch-muted">Thanks, {form.name || 'there'}. We’ve received your message.</p>
              <div className="mt-5"><Button variant="secondary" onClick={() => { setSent(false); setForm({ name:'', email:'', subject:'', message:'' }); }}>Send another message</Button></div>
            </div>
          ) : (
            <form className="space-y-4" onSubmit={onSubmit}>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Name</span><input required className={`${inputClass} mt-1.5`} value={form.name} onChange={update('name')} autoComplete="name" /></label>
                <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Email</span><input required type="email" className={`${inputClass} mt-1.5`} value={form.email} onChange={update('email')} autoComplete="email" /></label>
              </div>
              <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Subject</span><input className={`${inputClass} mt-1.5`} value={form.subject} onChange={update('subject')} /></label>
              <label className="block"><span className="text-[13px] font-medium text-fetch-ink/80">Message</span><textarea required rows={6} className={`${inputClass} h-auto py-3`} value={form.message} onChange={update('message')} /></label>
              {error && <p className="text-[14px] text-red-700">{error}</p>}
              <div className="pt-2"><Button type="submit" loading={sending}>Send message</Button></div>
            </form>
          )}
        </section>
      </div>
    </StoreLayout>
  );
}
