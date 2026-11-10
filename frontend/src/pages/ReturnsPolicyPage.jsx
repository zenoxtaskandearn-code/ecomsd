import ContentPage, { PolicySection } from '../components/content/ContentPage.jsx';

export default function ReturnsPolicyPage() {
  return (
    <ContentPage
      eyebrow="Policy"
      title="Returns &amp; refunds"
      intro="We want you to feel confident shopping with Lupine &amp; Leash. Because many items are fulfilled through fulfillment partners, return handling may vary by product and supplier. Please review this policy and contact us before sending anything back."
      updated="November 10, 2026"
    >
      <PolicySection title="Return requests and eligibility">
        <p>Please contact us within <strong>30 days of delivery</strong> (or as soon as an issue is discovered) with your order number, the item(s), and the reason for the request. Eligibility depends on the product, condition, and timing. Items generally must be unused and in original condition/packaging where applicable. Some items may not be eligible for return due to hygiene, safety, or supplier restrictions.</p>
      </PolicySection>
      <PolicySection title="How to start a return">
        <p>Email/contact support with your order reference, photos (if applicable), and details. <strong>Do not return items without instructions</strong> — return addresses and procedures can vary by fulfillment partner and product. We’ll confirm whether a return, replacement, or other resolution is appropriate and provide next steps.</p>
      </PolicySection>
      <PolicySection title="Damaged, defective, or incorrect items">
        <p>If your order arrives damaged, defective, or incorrect, contact us promptly with your order number and clear photos/videos of the item, packaging, and shipping label if available. We’ll review the issue with fulfillment/shipping records and offer an appropriate resolution (such as a replacement or refund) in accordance with the circumstances and partner process.</p>
      </PolicySection>
      <PolicySection title="Refunds">
        <p>Approved refunds are issued to the original payment method (e.g. PayPal) when possible. Refund timing depends on the payment provider and may take several business days to appear after processing. Original shipping costs are generally non-refundable unless the issue is due to our error or a confirmed defect/damage/incorrect item.</p>
      </PolicySection>
      <PolicySection title="Cancellations and changes">
        <p>If you need to cancel or change an order, contact us as soon as possible. Once payment is confirmed and fulfillment/processing has begun, we may be unable to cancel or modify the order; in that case, the return process (if eligible) may apply after delivery.</p>
      </PolicySection>
      <PolicySection title="Non-delivery and tracking issues">
        <p>If tracking shows delays or delivery issues, contact us with your order number. We’ll review carrier/fulfillment tracking information and work with the fulfillment partner to investigate and resolve the issue where possible.</p>
      </PolicySection>
    </ContentPage>
  );
}
