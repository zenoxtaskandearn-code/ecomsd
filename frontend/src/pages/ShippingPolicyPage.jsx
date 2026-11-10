import ContentPage, { PolicySection } from '../components/content/ContentPage.jsx';

export default function ShippingPolicyPage() {
  return (
    <ContentPage
      eyebrow="Policy"
      title="Shipping policy"
      intro="Lupine &amp; Leash ships premium pet accessories to customers primarily in the United States. Because some items may be fulfilled through international dropshipping partners, delivery times can vary."
      updated="May 10, 2026"
    >
      <PolicySection title="Processing times">
        <p>Orders are typically prepared for fulfillment after payment is confirmed. Processing time may vary by product, supplier availability, and destination. Any available processing estimate will be shown at checkout or in your order updates.</p>
      </PolicySection>
      <PolicySection title="Delivery estimates">
        <p>Delivery estimates include processing and transit time where available. International fulfillment may take longer than domestic warehouse shipping. Estimates are not guarantees and can be affected by carrier delays, customs processing, weather, peak seasons, or supplier constraints.</p>
      </PolicySection>
      <PolicySection title="Tracking">
        <p>When tracking is available, we’ll share tracking details in your account or by order update. Some shipments may have partial or delayed tracking depending on the carrier and fulfillment route.</p>
      </PolicySection>
      <PolicySection title="Address accuracy">
        <p>Please review your shipping address carefully before placing an order. We may not be able to change an address after fulfillment begins. Orders returned due to incorrect or incomplete addresses may be subject to reshipping costs or refund adjustments where applicable.</p>
      </PolicySection>
      <PolicySection title="Customs, duties, and taxes">
        <p>Any applicable taxes, duties, or import fees depend on destination and order details. Where required, charges will be communicated according to the checkout and applicable regulations. Final tax handling should be confirmed before launch.</p>
      </PolicySection>
      <PolicySection title="Questions">
        <p>If you need help with a shipment, contact us with your order reference and we’ll assist you.</p>
      </PolicySection>
    </ContentPage>
  );
}
