export default function Spinner({ size = 18, className = '', label = 'Loading' }) {
  return (
    <span
      role="status"
      aria-label={label}
      className={`inline-block shrink-0 animate-spin rounded-full border-2 border-current/25 border-t-current ${className}`}
      style={{ width: size, height: size }}
    />
  );
}
