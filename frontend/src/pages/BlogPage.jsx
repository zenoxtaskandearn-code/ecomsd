import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import { Skeleton } from '../components/ui/Skeleton.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';

// MVP strategy: no invented posts. Connect to a CMS/API later.
const posts = [];

export default function BlogPage() {
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    const t = setTimeout(() => setLoading(false), 350);
    return () => clearTimeout(t);
  }, []);

  return (
    <StoreLayout>
      <div className="border-b border-fetch-ink/[0.06] bg-[#FBFAF8]">
        <div className="section-container py-12 sm:py-14">
          <p className="text-[11px] font-medium uppercase tracking-[0.22em] text-fetch-ink/45">Journal</p>
          <h1 className="mt-3 text-[2.35rem] font-semibold leading-[1.04] tracking-[-0.03em] text-fetch-ink sm:text-[2.9rem]">The Lupine &amp; Leash journal</h1>
          <p className="mt-4 max-w-[60ch] text-[16px] leading-[1.85] text-fetch-muted">
            Notes on thoughtful walks, product care, and everyday routines with your dog. New posts will appear here when
            they’re published.
          </p>
        </div>
      </div>
      <div className="section-container py-12 sm:py-14">
        {loading ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i}><Skeleton className="aspect-[16/10] w-full" /><Skeleton className="mt-4 h-5 w-[80%]" /><Skeleton className="mt-2 h-4 w-full" /></div>
            ))}
          </div>
        ) : posts.length ? (
          <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
            {posts.map((post) => (
              <article key={post.slug}>
                <Link to={`/blog/${post.slug}`} className="group block">
                  <div className="aspect-[16/10] bg-[#F0EEE9]" />
                  <h2 className="mt-4 text-[1.15rem] font-semibold tracking-[-0.02em] text-fetch-ink group-hover:text-fetch-ink/75">{post.title}</h2>
                </Link>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            title="No posts published yet"
            message="We’re not inventing placeholder articles. When real journal content is ready, it will appear here with a CMS or content API integration."
            actionLabel="Explore shop essentials"
            onAction={() => (window.location.href = '/shop')}
          />
        )}
      </div>
    </StoreLayout>
  );
}
