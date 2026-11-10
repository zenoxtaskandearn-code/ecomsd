import ContentPage, { PolicySection } from '../components/content/ContentPage.jsx';

export default function ShippingPolicyPage() {
  return (
    <ContentPage
      eyebrow="Policy"
      title="Shipping policy"
      intro="Lupine &amp; Leash ships pet accessories with order and tracking updates provided in your account. Many items are fulfilled through trusted fulfillment partners (including CJdropshipping), so processing and delivery times can vary by product and destination."
      updated="November 10, 2026"
    >
      <PolicySection title="Order processing">
        <p>Orders are processed after payment is confirmed on our servers. Processing typically begins within 1–3 business days, but may vary based on product availability, fulfillment partner capacity, and destination. You can view your order status and timeline in your account.</p>
      </PolicySection>
      <PolicySection title="Delivery estimates">
        <p>Estimated delivery depends on the product, fulfillment location, carrier, and destination. For many dropshipped items, delivery is commonly <strong>7–15 business days</strong> after payment, but some orders may arrive sooner or take longer. Any available product-specific estimate is shown on the product page and/or in your order details. Delivery estimates are not guarantees.</p>
      </PolicySection>
      <PolicySection title="Tracking and updates">
        <p>Once your order ships, tracking details (carrier, tracking number/link, shipment status, and available timestamps) will appear on your order detail page in your account when provided by the fulfillment partner/carrier. Tracking may take 24–72 hours to update after a label is created, and some routes may have limited or delayed scans.</p>
      </PolicySection>
      <PolicySection title="Shipping address">
        <p>Please verify your shipping address at checkout. If an order has already been submitted for fulfillment, we may be unable to change the address. Orders returned or delayed due to an incorrect/incomplete address may require additional time and may be subject to reshipping costs or adjustments where applicable.</p>
      </PolicySection>
      <PolicySection title="Customs, duties, and taxes">
        <p>Depending on the destination and fulfillment route, international shipments may be subject to customs processing, import duties, taxes, or fees charged by the destination country. Unless explicitly stated at checkout, these charges (if any) are the customer’s responsibility. Customs processing can also affect delivery times.</p>
      </PolicySection>
      <PolicySection title="Delays and support">
        <p>Delays can occur due to carrier issues, customs, weather, peak seasons, or supplier constraints. If you need help with a shipment, contact us with your order number and we’ll assist you using the available fulfillment and tracking information.</p>
      </PolicySection>
    </ContentPage>
  );
}
