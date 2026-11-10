import { Link, useParams } from 'react-router-dom';
import StoreLayout from '../components/layout/StoreLayout.jsx';
import { EmptyState } from '../components/ui/StatePanel.jsx';

export default function BlogPostPage() {
  const { slug } = useParams();

  return (
    <StoreLayout>
      <div className="section-container py-12 sm:py-14">
        <p className="text-[13px] text-fetch-muted">
          <Link to="/blog" className="transition hover:text-fetch-ink">Journal</Link>
          <span className="mx-2 text-fetch-ink/25">/</span>
          <span className="text-fetch-ink/70">{slug}</span>
        </p>
        <div className="mt-8">
          <EmptyState
            title="Post not available"
            message="This journal post hasn’t been published yet. Blog content will load from a CMS/API when it’s connected — we don’t show invented articles."
            actionLabel="Back to journal"
            onAction={() => (window.location.href = '/blog')}
          />
        </div>
      </div>
    </StoreLayout>
  );
}
