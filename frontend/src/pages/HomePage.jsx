import AnnouncementBar from '../components/layout/AnnouncementBar.jsx';
import Navbar from '../components/layout/Navbar.jsx';
import Footer from '../components/layout/Footer.jsx';
import HeroSection from '../components/home/HeroSection.jsx';
import FeaturedCollection from '../components/home/FeaturedCollection.jsx';
import BrandStory from '../components/home/BrandStory.jsx';
import BestSellers from '../components/home/BestSellers.jsx';
import LifestyleSection from '../components/home/LifestyleSection.jsx';
import SustainabilitySection from '../components/home/SustainabilitySection.jsx';
import NewsletterSection from '../components/home/NewsletterSection.jsx';

export default function HomePage() {
  return (
    <div className="min-h-screen bg-white">
      <AnnouncementBar />
      <Navbar />
      <main>
        <HeroSection />
        <FeaturedCollection />
        <BrandStory />
        <BestSellers />
        <LifestyleSection />
        <SustainabilitySection />
        <NewsletterSection />
      </main>
      <Footer />
    </div>
  );
}
