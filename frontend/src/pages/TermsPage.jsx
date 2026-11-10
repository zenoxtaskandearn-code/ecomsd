import ContentPage, { PolicySection } from '../components/content/ContentPage.jsx';

export default function TermsPage() {
  return (
    <ContentPage
      eyebrow="Policy"
      title="Terms & conditions"
      intro="These terms outline the basic rules for using the Lupine &amp; Leash website and placing orders. This is a working draft and should be reviewed by qualified legal counsel before launch."
      updated="May 10, 2026"
    >
      <PolicySection title="Use of the site">
        <p>By accessing or using this site, you agree to use it lawfully and not to misuse, disrupt, or attempt unauthorized access to the site, accounts, or systems.</p>
      </PolicySection>
      <PolicySection title="Products and pricing">
        <p>We aim to present products, descriptions, pricing, and availability accurately. However, errors may occur. We reserve the right to correct errors, update information, or cancel/refund orders affected by material pricing or availability errors, in accordance with applicable law and our policies.</p>
      </PolicySection>
      <PolicySection title="Orders and payment">
        <p>An order is subject to acceptance, payment verification, and availability. Payments are processed through approved payment providers such as PayPal, and order/payment status is confirmed on our servers. We may cancel or limit orders in cases of suspected fraud, misuse, or operational issues.</p>
      </PolicySection>
      <PolicySection title="Shipping, returns, and delivery">
        <p>Shipping and returns are governed by our Shipping Policy and Returns &amp; Refunds Policy. Delivery estimates are provided for guidance and are not guarantees, as fulfillment and carrier timelines may vary (including for dropshipped items). Customs duties/taxes and risk-of-loss terms apply as described in the applicable policies and law.</p>
      </PolicySection>
      <PolicySection title="Intellectual property">
        <p>Site content, branding, design, text, graphics, and logos are owned by or licensed to Lupine &amp; Leash unless otherwise stated. You may not use them without permission except as allowed by law.</p>
      </PolicySection>
      <PolicySection title="Limitation and governing terms">
        <p>To the extent permitted by law, the site and services are provided on an operational basis and liability limitations/disclaimers should be defined in the final legal terms. Governing law, venue, and dispute terms must be reviewed and set appropriately before production use.</p>
      </PolicySection>
    </ContentPage>
  );
}
