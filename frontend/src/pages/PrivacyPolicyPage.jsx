import ContentPage, { PolicySection } from '../components/content/ContentPage.jsx';

export default function PrivacyPolicyPage() {
  return (
    <ContentPage
      eyebrow="Policy"
      title="Privacy policy"
      intro="This page explains what information Lupine &amp; Leash may collect, how it may be used, and the choices available to you. This is a working draft and should be reviewed by qualified legal counsel before launch."
      updated="November 10, 2026"
    >
      <PolicySection title="Information we may collect">
        <p>We may collect information you provide directly, such as name, email, shipping address, phone number (if provided), and order details. We may also collect technical information needed to operate the site, such as device/browser data, approximate usage data, and cookies or similar technologies where implemented.</p>
      </PolicySection>
      <PolicySection title="How information may be used">
        <p>Information may be used to process orders, provide customer support, communicate about purchases and shipping, improve the site, prevent fraud or abuse, and comply with legal obligations. Marketing communications, if used, should be sent only with appropriate consent/preferences.</p>
      </PolicySection>
      <PolicySection title="Payments">
        <p>Payments are processed through PayPal. Lupine &amp; Leash does not store full payment card numbers. Payment data is handled according to PayPal’s systems and policies, and payment/order status is confirmed on our servers.</p>
      </PolicySection>
      <PolicySection title="Sharing">
        <p>We may share necessary information with service providers involved in operating the store, such as payment processors, fulfillment or shipping partners, hosting/providers, and support tools — only as needed for those services. We do not sell personal information as a core business practice.</p>
      </PolicySection>
      <PolicySection title="Retention and security">
        <p>We retain information as needed for orders, support, legal, and operational purposes. Reasonable technical and organizational safeguards should be used, but no online system can be guaranteed completely secure.</p>
      </PolicySection>
      <PolicySection title="Your choices and contact">
        <p>You may contact us to request access, correction, or deletion of certain information, subject to legal and operational requirements. Final rights, notices, and disclosures should be tailored to applicable laws and reviewed before production launch.</p>
      </PolicySection>
    </ContentPage>
  );
}
