// Minimal HTML sanitizer for trusted-origin imported content (e.g. CJ descriptions).
// Removes scripts/embeds/event handlers and dangerous URL protocols. Not a full substitute for DOMPurify.
const BLOCKED_TAGS = /<\/?(?:script|style|iframe|object|embed|form|input|button|meta|link|base)[^>]*>/gi;
const EVENT_ATTRS = /\son[a-z]+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)/gi;
const DANGEROUS_URL = /\s(href|src)\s*=\s*(?:"\s*(?:javascript|vbscript|data):[^"]*"|'\s*(?:javascript|vbscript|data):[^']*')/gi;

function normalizeImgs(html) {
  return String(html).replace(/<img\b([^>]*)>/gi, (m, attrs) => {
    let a = attrs
      .replace(/\sstyle\s*=\s*(?:"[^"]*"|'[^']*')/gi, '')
      .replace(/\s(?:width|height)\s*=\s*(?:"[^"]*"|'[^']*'|\d+)/gi, '')
      .replace(/\sng-[a-z-]+\s*=\s*(?:"[^"]*"|'[^']*')/gi, '')
      .replace(/\sdraggable\s*=\s*(?:"[^"]*"|'[^']*')/gi, '')
      .replace(/\scontenteditable\s*=\s*(?:"[^"]*"|'[^']*')/gi, '');
    return `<img${a} loading="lazy" class="cj-desc-img" />`;
  });
}

export function sanitizeHtml(html) {
  if (html == null || html === '') return '';
  let s = String(html);
  s = s.replace(BLOCKED_TAGS, '');
  s = s.replace(EVENT_ATTRS, '');
  s = s.replace(DANGEROUS_URL, ' $1="#"');
  s = normalizeImgs(s);
  return s;
}

export function hasHtml(html) {
  return typeof html === 'string' && /<\/?[a-z][\s\S]*>/i.test(html);
}

