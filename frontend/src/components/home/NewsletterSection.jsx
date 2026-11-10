import { useState } from 'react';

export default function NewsletterSection() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const onSubmit = (e) => {
    e.preventDefault();
    if (!email.trim()) return;
    // Visual foundation only: connect to email provider in a later phase.
    setSubmitted(true);
  };

  return (
    <section className="bg-fetch-dark py-16 text-white sm:py-20 lg:py-24" id="blog">
      <div className="section-container">
        <div className="mx-auto grid max-w-[980px] gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:items-end lg:gap-14">
          <div className="text-center lg:text-left">
            <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-white/45">Newsletter</p>
            <h2 className="mt-3 text-[2rem] font-semibold leading-[1.08] tracking-[-0.03em] sm:text-[2.4rem]">
              Join the Lupine &amp; Leash list
            </h2>
            <p className="mx-auto mt-4 max-w-[40ch] text-[15px] leading-[1.8] text-white/65 lg:mx-0 sm:text-[16px]">
              Occasional notes on new essentials, care tips, and thoughtful updates. No spam.
            </p>
          </div>
          <div className="lg:pb-1">
            {submitted ? (
              <p className="text-center text-[15px] text-white/85 lg:text-left" role="status">
                Thanks — we’ll be in touch when the list is live.
              </p>
            ) : (
              <form className="flex flex-col gap-3 sm:flex-row sm:items-center" onSubmit={onSubmit}>
                <label htmlFor="newsletter-email" className="sr-only">
                  Email address
                </label>
                <input
                  id="newsletter-email"
                  type="email"
                  name="email"
                  autoComplete="email"
                  placeholder="Email address"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="h-12 w-full min-w-0 rounded-pill border border-white/12 bg-white/[0.07] px-5 text-[15px] text-white placeholder:text-white/40 transition duration-300 focus:border-white/25 focus:bg-white/[0.09] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white/40"
                />
                <button
                  type="submit"
                  className="inline-flex h-12 shrink-0 items-center justify-center rounded-pill bg-white px-7 text-[15px] font-medium text-fetch-dark transition duration-300 hover:bg-fetch-cream focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
                >
                  Subscribe
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
