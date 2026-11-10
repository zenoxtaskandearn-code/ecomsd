import ContentPage from '../components/content/ContentPage.jsx';
import Accordion from '../components/content/Accordion.jsx';

const items = [
  {
    question: 'Where do you ship?',
    answer: (
      <p>
        Lupine &amp; Leash primarily serves customers in the United States. Shipping availability, carriers, and delivery estimates are
        shown at checkout once fulfillment and shipping rules are fully configured.
      </p>
    ),
  },
  {
    question: 'How long does delivery take?',
    answer: (
      <p>
        Delivery times can vary because products may be fulfilled through international dropshipping partners. We’ll
        provide the clearest available processing and transit estimates at checkout and in your order updates. Please
        review the Shipping Policy for the latest details.
      </p>
    ),
  },
  {
    question: 'How do I choose the right size?',
    answer: (
      <p>
        Each product page will include sizing guidance and fit notes where available. If you’re between sizes or need help,
        contact us before ordering and we’ll help you choose the best option.
      </p>
    ),
  },
  {
    question: 'What payment methods do you accept?',
    answer: (
      <p>
        We plan to accept PayPal at checkout. Payment processing is handled securely through PayPal; Lupine &amp; Leash does not store
        your full payment card details.
      </p>
    ),
  },
  {
    question: 'Can I return an item?',
    answer: (
      <p>
        Returns and refunds depend on product condition, timing, and the reason for return. Please read our Returns &amp;
        Refunds Policy for eligibility and instructions before sending an item back.
      </p>
    ),
  },
  {
    question: 'How can I track my order?',
    answer: (
      <p>
        Once your order ships and tracking becomes available, you’ll be able to view tracking details in your account and
        through order updates. Some shipments may have limited tracking depending on the fulfillment method.
      </p>
    ),
  },
];

export default function FaqPage() {
  return (
    <ContentPage
      eyebrow="Help"
      title="Frequently asked questions"
      intro="Clear answers about ordering, shipping, sizing, and returns. If you need more help, our contact page is always open."
    >
      <Accordion items={items} />
    </ContentPage>
  );
}
