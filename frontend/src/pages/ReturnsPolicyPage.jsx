import ContentPage, { PolicySection } from '../components/content/ContentPage.jsx';

export default function ReturnsPolicyPage() {
  return (
    <ContentPage
      eyebrow="Policy"
      title="Returns & refunds"
      intro="We want you to feel confident shopping with Lupine &amp; Leash. This policy explains how returns and refunds are handled. Please review it before requesting a return."
      updated="May 10, 2026"
    >
      <PolicySection title="Eligibility">
        <p>Return eligibility depends on the product, condition, and time since delivery. Items generally must be unused, in original condition, and returned with required packaging where applicable. Some items may be non-returnable for hygiene, safety, or supplier reasons; product-specific restrictions will be communicated where relevant.</p>
      </PolicySection>
      <PolicySection title="How to start a return">
        <p>Contact us with your order reference, the item(s) you want to return, and the reason for the request. Please wait for return instructions before sending anything back, as return addresses and procedures may vary by fulfillment partner.</p>
      </PolicySection>
      <PolicySection title="Refunds">
        <p>Once a return is approved and received/verified as required, eligible refunds are issued to the original payment method where possible. Processing times can vary by payment provider. Shipping costs may be non-refundable unless the return is due to our error or a confirmed issue covered by policy.</p>
      </PolicySection>
      <PolicySection title="Damaged or incorrect items">
        <p>If your order arrives damaged or incorrect, contact us promptly with your order reference and clear photos of the issue. We’ll review the case and offer an appropriate resolution in line with our operations and supplier process.</p>
      </PolicySection>
      <PolicySection title="Cancellations">
        <p>Cancellation requests should be submitted as soon as possible. If an order has already entered processing or fulfillment, cancellation may not be possible and the return policy may apply instead.</p>
      </PolicySection>
    </ContentPage>
  );
}
