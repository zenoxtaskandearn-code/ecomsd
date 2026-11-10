import { products, categories } from './catalog.js';
import { demoOrders } from './accountDemo.js';

export const adminStats = [
  { label: 'Total sales', value: '$4,280.00', change: '+12.4%', note: 'Demo period' },
  { label: 'Orders', value: '38', change: '+8.1%', note: 'Last 30 days' },
  { label: 'Fulfillment queue', value: '6', change: '2 urgent', note: 'Needs review' },
  { label: 'Customers', value: '124', change: '+5.2%', note: 'Active accounts' },
];

export const salesChart = [
  { name: 'Mon', sales: 320, orders: 4 },
  { name: 'Tue', sales: 480, orders: 6 },
  { name: 'Wed', sales: 290, orders: 3 },
  { name: 'Thu', sales: 610, orders: 7 },
  { name: 'Fri', sales: 540, orders: 6 },
  { name: 'Sat', sales: 760, orders: 8 },
  { name: 'Sun', sales: 690, orders: 7 },
];

export const statusChart = [
  { name: 'Paid', value: 14 },
  { name: 'Processing', value: 6 },
  { name: 'Shipped', value: 11 },
  { name: 'Delivered', value: 7 },
];

export const adminOrders = demoOrders.map((o, i) => ({
  ...o,
  customer: ['Alex Morgan', 'Jordan Lee'][i % 2],
  payment: 'PayPal (demo)',
  fulfillment: i === 0 ? 'Needs tracking update' : 'Completed',
}));

export const adminCustomers = [
  { id: 'c1', name: 'Alex Morgan', email: 'alex@example.com', orders: 2, spent: '$134.00', status: 'Active' },
  { id: 'c2', name: 'Jordan Lee', email: 'jordan@example.com', orders: 1, spent: '$76.00', status: 'Active' },
  { id: 'c3', name: 'Sam Patel', email: 'sam@example.com', orders: 3, spent: '$188.00', status: 'Active' },
];

export const adminSuppliers = [
  { id: 's1', name: 'Primary Dropship Partner', code: 'DSP-01', mode: 'Manual', status: 'Active', mappings: 8, lastSync: 'Not connected' },
  { id: 's2', name: 'Travel Accessories Supplier', code: 'DSP-02', mode: 'Manual', status: 'Pending setup', mappings: 2, lastSync: 'Not connected' },
];

export { products as adminProducts, categories as adminCategories };
