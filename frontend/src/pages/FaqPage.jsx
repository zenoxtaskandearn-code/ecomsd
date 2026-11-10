import ContentPage from '../components/content/ContentPage.jsx';
import Accordion from '../components/content/Accordion.jsx';

const items = [
  {
    question: 'Where do you ship?',
    answer: (
      <p>
        We currently focus on customers in the United States. Available shipping options, carriers, and delivery estimates depend on the
        product and fulfillment route and are shown during checkout and in your order details when available.
      </p>
    ),
  },
  {
    question: 'How long does delivery take?',
    answer: (
      <p>
        Processing usually begins after payment is confirmed. Many items are fulfilled through partners such as CJdropshipping and commonly
        arrive in about <strong>7–15 business days</strong>, but timing can vary by product, supplier, carrier, customs, and destination.
        Product/order pages show the best available estimate, and tracking updates appear in your account once your order ships.
      </p>
    ),
  },
  {
    question: 'How do I track my order?',
    answer: (
      <p>
        Sign in to your account and open <strong>Account → Orders → Track/Details</strong>. You’ll see your order timeline, fulfillment/supplier
        status, expected delivery estimate, and shipment details (carrier, tracking number/link, shipped/delivered/last-updated times) when
        available. Tracking may take 24–72 hours to update after shipment.
      </p>
    ),
  },
  {
    question: 'What payment methods do you accept?',
    answer: (
      <p>
        We accept <strong>PayPal</strong> at checkout. Payments are processed securely by PayPal and your order/payment status is confirmed on our
        servers. Lupine &amp; Leash does not store your full payment card details.
      </p>
    ),
  },
  {
    question: 'Will I pay customs duties or taxes?',
    answer: (
      <p>
        Depending on the fulfillment route and destination, customs duties, import taxes, or fees may apply and are typically the customer’s
        responsibility unless stated otherwise at checkout. Customs processing can also affect delivery time.
      </p>
    ),
  },
  {
    question: 'Can I return or cancel an order?',
    answer: (
      <p>
        Return eligibility depends on the product, condition, and timing (generally contact us within 30 days of delivery with your order
        number). Cancellation/change requests should be made as soon as possible—once fulfillment begins, we may not be able to stop the
        shipment and the return process may apply instead. Please see our Returns &amp; Refunds and Shipping policies for details.
      </p>
    ),
  },
];

export default function FaqPage() {
  return (
    <ContentPage
      eyebrow="Help"
      title="Frequently asked questions"
      intro="Clear answers about ordering, shipping, tracking, payments, and returns. If you need more help, our contact page is always open."
    >
      <Accordion items={items} />
    </ContentPage>
  );
}
