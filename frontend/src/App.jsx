import { Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage.jsx';
import ShopPage from './pages/ShopPage.jsx';
import CollectionPage from './pages/CollectionPage.jsx';
import SearchPage from './pages/SearchPage.jsx';
import ProductDetailsPage from './pages/ProductDetailsPage.jsx';
import CartPage from './pages/CartPage.jsx';
import CheckoutPage from './pages/CheckoutPage.jsx';
import OrderConfirmationPage from './pages/OrderConfirmationPage.jsx';
import AboutPage from './pages/AboutPage.jsx';
import ContactPage from './pages/ContactPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import ActivateAccountPage from './pages/ActivateAccountPage.jsx';
import ForgotPasswordPage from './pages/ForgotPasswordPage.jsx';
import ResetPasswordPage from './pages/ResetPasswordPage.jsx';
import ResendActivationPage from './pages/ResendActivationPage.jsx';
import AccountPage from './pages/AccountPage.jsx';
import AccountAddressesPage from './pages/AccountAddressesPage.jsx';
import AccountOrdersPage from './pages/AccountOrdersPage.jsx';
import AccountOrderDetailPage from './pages/AccountOrderDetailPage.jsx';
import FaqPage from './pages/FaqPage.jsx';
import ShippingPolicyPage from './pages/ShippingPolicyPage.jsx';
import ReturnsPolicyPage from './pages/ReturnsPolicyPage.jsx';
import PrivacyPolicyPage from './pages/PrivacyPolicyPage.jsx';
import TermsPage from './pages/TermsPage.jsx';
import BlogPage from './pages/BlogPage.jsx';
import BlogPostPage from './pages/BlogPostPage.jsx';
import ProtectedRoute from './components/auth/ProtectedRoute.jsx';
import AdminProtectedRoute from './components/admin/AdminProtectedRoute.jsx';
import AdminLoginPage from './pages/admin/AdminLoginPage.jsx';
import AdminDashboardPage from './pages/admin/AdminDashboardPage.jsx';
import AdminProductsPage from './pages/admin/AdminProductsPage.jsx';
import AdminCategoriesPage from './pages/admin/AdminCategoriesPage.jsx';
import AdminOrdersPage from './pages/admin/AdminOrdersPage.jsx';
import AdminOrderDetailPage from './pages/admin/AdminOrderDetailPage.jsx';
import AdminCustomersPage from './pages/admin/AdminCustomersPage.jsx';
import AdminSuppliersPage from './pages/admin/AdminSuppliersPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/shop" element={<ShopPage />} />
      <Route path="/collections/:slug" element={<CollectionPage />} />
      <Route path="/search" element={<SearchPage />} />
      <Route path="/product/:slug" element={<ProductDetailsPage />} />
      <Route path="/cart" element={<CartPage />} />
      <Route path="/checkout" element={<CheckoutPage />} />
      <Route path="/order-confirmation" element={<OrderConfirmationPage />} />
      <Route path="/about" element={<AboutPage />} />
      <Route path="/contact" element={<ContactPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/activate-account" element={<ActivateAccountPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/resend-activation" element={<ResendActivationPage />} />
      <Route path="/account" element={<ProtectedRoute><AccountPage /></ProtectedRoute>} />
      <Route path="/account/addresses" element={<ProtectedRoute><AccountAddressesPage /></ProtectedRoute>} />
      <Route path="/account/orders" element={<ProtectedRoute><AccountOrdersPage /></ProtectedRoute>} />
      <Route path="/account/orders/:orderNumber" element={<ProtectedRoute><AccountOrderDetailPage /></ProtectedRoute>} />
      <Route path="/faq" element={<FaqPage />} />
      <Route path="/policies/shipping" element={<ShippingPolicyPage />} />
      <Route path="/policies/returns" element={<ReturnsPolicyPage />} />
      <Route path="/policies/privacy" element={<PrivacyPolicyPage />} />
      <Route path="/policies/terms" element={<TermsPage />} />
      <Route path="/blog" element={<BlogPage />} />
      <Route path="/blog/:slug" element={<BlogPostPage />} />

      <Route path="/admin/login" element={<AdminLoginPage />} />
      <Route path="/admin" element={<AdminProtectedRoute><AdminDashboardPage /></AdminProtectedRoute>} />
      <Route path="/admin/products" element={<AdminProtectedRoute><AdminProductsPage /></AdminProtectedRoute>} />
      <Route path="/admin/categories" element={<AdminProtectedRoute><AdminCategoriesPage /></AdminProtectedRoute>} />
      <Route path="/admin/orders" element={<AdminProtectedRoute><AdminOrdersPage /></AdminProtectedRoute>} />
      <Route path="/admin/orders/:orderNumber" element={<AdminProtectedRoute><AdminOrderDetailPage /></AdminProtectedRoute>} />
      <Route path="/admin/customers" element={<AdminProtectedRoute><AdminCustomersPage /></AdminProtectedRoute>} />
      <Route path="/admin/suppliers" element={<AdminProtectedRoute><AdminSuppliersPage /></AdminProtectedRoute>} />
    </Routes>
  );
}
