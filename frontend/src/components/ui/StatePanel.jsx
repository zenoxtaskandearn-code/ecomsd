import { AlertCircle, SearchX } from 'lucide-react';
import Button from './Button.jsx';

export function EmptyState({ title = 'Nothing here yet', message, actionLabel, onAction }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center sm:py-20">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-fetch-ink/[0.04] text-fetch-ink/45">
        <SearchX size={22} strokeWidth={1.5} aria-hidden="true" />
      </div>
      <h3 className="mt-5 text-[1.1rem] font-semibold tracking-[-0.02em] text-fetch-ink">{title}</h3>
      {message && <p className="mt-2 max-w-[38ch] text-[15px] leading-[1.7] text-fetch-muted">{message}</p>}
      {actionLabel && onAction && (
        <div className="mt-6">
          <Button variant="secondary" onClick={onAction}>
            {actionLabel}
          </Button>
        </div>
      )}
    </div>
  );
}

export function ErrorState({ title = 'Something went wrong', message, actionLabel = 'Try again', onAction }) {
  return (
    <div className="flex flex-col items-center justify-center border border-fetch-ink/[0.08] bg-[#FBFAF8] py-14 text-center sm:py-16">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-fetch-ink/[0.04] text-fetch-ink/50">
        <AlertCircle size={22} strokeWidth={1.5} aria-hidden="true" />
      </div>
      <h3 className="mt-5 text-[1.1rem] font-semibold tracking-[-0.02em] text-fetch-ink">{title}</h3>
      {message && <p className="mt-2 max-w-[40ch] text-[15px] leading-[1.7] text-fetch-muted">{message}</p>}
      {onAction && (
        <div className="mt-6">
          <Button variant="secondary" onClick={onAction}>
            {actionLabel}
          </Button>
        </div>
      )}
    </div>
  );
}
