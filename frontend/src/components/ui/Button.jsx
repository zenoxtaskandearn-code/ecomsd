import Spinner from './Spinner.jsx';

const variants = {
  primary:
    'bg-fetch-dark text-white hover:bg-[#1a1c2b] active:bg-[#0c0d16] focus-visible:outline-fetch-dark',
  secondary:
    'border border-fetch-ink/15 bg-white text-fetch-ink hover:border-fetch-ink/25 hover:bg-[#fafaf8] focus-visible:outline-fetch-dark/30',
  ghost:
    'text-fetch-ink/75 hover:text-fetch-ink hover:bg-fetch-ink/[0.04] focus-visible:outline-fetch-dark/20',
};

export default function Button({
  children,
  variant = 'primary',
  loading = false,
  disabled = false,
  type = 'button',
  className = '',
  ...props
}) {
  const isDisabled = disabled || loading;
  return (
    <button
      type={type}
      disabled={isDisabled}
      className={`inline-flex h-11 items-center justify-center gap-2 rounded-pill px-6 text-[15px] font-medium tracking-[0.01em] transition duration-200 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-55 ${variants[variant] || variants.primary} ${className}`}
      {...props}
    >
      {loading && <Spinner size={16} />}
      <span>{children}</span>
    </button>
  );
}
