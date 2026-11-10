import { useState } from 'react';
import { ChevronDown } from 'lucide-react';

export function AccordionItem({ question, children, defaultOpen = false }) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="border-b border-fetch-ink/[0.08]">
      <button
        type="button"
        className="flex w-full items-start justify-between gap-4 py-5 text-left"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
      >
        <span className="text-[16px] font-medium leading-[1.45] tracking-[-0.01em] text-fetch-ink">{question}</span>
        <ChevronDown
          size={18}
          strokeWidth={1.6}
          className={`mt-0.5 shrink-0 text-fetch-ink/45 transition-transform duration-300 ${open ? 'rotate-180' : ''}`}
          aria-hidden="true"
        />
      </button>
      <div className={`grid transition-all duration-300 ease-out ${open ? 'grid-rows-[1fr] pb-5 opacity-100' : 'grid-rows-[0fr] opacity-0'}`}>
        <div className="overflow-hidden">
          <div className="max-w-[62ch] text-[15px] leading-[1.85] text-fetch-muted">{children}</div>
        </div>
      </div>
    </div>
  );
}

export default function Accordion({ items }) {
  return (
    <div className="border-t border-fetch-ink/[0.08]">
      {items.map((item, i) => (
        <AccordionItem key={item.question} question={item.question} defaultOpen={i === 0}>
          {item.answer}
        </AccordionItem>
      ))}
    </div>
  );
}
