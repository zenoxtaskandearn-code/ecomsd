import { createContext, useContext, useEffect, useMemo, useReducer, useCallback } from 'react';
import { apiEnabled } from '../services/api.js';
import * as cartApi from '../services/cartApi.js';
import { useAuth } from './AuthContext.jsx';

const CartContext = createContext(null);

function reducer(state, action) {
  switch (action.type) {
    case 'SET': return { ...state, items: action.items || [], server: !!action.server };
    case 'ADD': {
      const { product, quantity = 1 } = action;
      const key = product.slug || product.id;
      const existing = state.items.find((i) => (i.slug || i.id) === key);
      if (existing) {
        return { ...state, items: state.items.map((i) => ((i.slug || i.id) === key ? { ...i, quantity: i.quantity + quantity } : i)) };
      }
      return { ...state, items: [...state.items, { slug: product.slug, variantId: product.variants?.[0]?.id || product.variantId, name: product.name, price: Number(product.price), image: product.image, variant: product.variant || product.variants?.[0]?.title, quantity }] };
    }
    case 'UPDATE':
      return { ...state, items: state.items.map((i) => ((action.id && i.id === action.id) || (!action.id && i.slug === action.slug)) ? { ...i, quantity: Math.max(1, action.quantity) } : i) };
    case 'REMOVE':
      return { ...state, items: state.items.filter((i) => action.id ? i.id !== action.id : i.slug !== action.slug) };
    case 'CLEAR':
      return { ...state, items: [] };
    default: return state;
  }
}

function normalizeServerCart(data) {
  return (data.items || []).map((i) => ({
    id: i.id, // cart item id
    slug: i.slug,
    variantId: i.variantId,
    name: i.name,
    price: Number(i.unitPrice ?? i.price ?? 0),
    image: i.image,
    variant: i.variant,
    quantity: Number(i.quantity || 1),
    lineTotal: Number(i.lineTotal ?? 0),
  }));
}

export function CartProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [state, dispatch] = useReducer(reducer, { items: [], server: false });
  const useServer = apiEnabled && isAuthenticated;

  const refresh = useCallback(async () => {
    if (!useServer) return;
    try {
      const data = await cartApi.getCart();
      dispatch({ type: 'SET', items: normalizeServerCart(data), server: true });
    } catch {
      // if unauthenticated or backend unavailable, keep local
    }
  }, [useServer]);

  useEffect(() => {
    if (useServer) refresh();
    else if (apiEnabled && !isAuthenticated) dispatch({ type: 'SET', items: [], server: false });
  }, [useServer, refresh, isAuthenticated]);

  const value = useMemo(() => {
    const count = state.items.reduce((n, i) => n + Number(i.quantity || 0), 0);
    const subtotal = state.items.reduce((n, i) => n + Number(i.price || 0) * Number(i.quantity || 0), 0);
    return {
      items: state.items,
      count,
      subtotal,
      serverMode: !!state.server && useServer,
      refresh,
      addItem: async (product, quantity = 1) => {
        const variantId = product?._variantId || product?.variants?.[0]?.id || product?.variantId;
        if (useServer && variantId) {
          const data = await cartApi.addItem(variantId, quantity);
          dispatch({ type: 'SET', items: normalizeServerCart(data), server: true });
          return;
        }
        dispatch({ type: 'ADD', product, quantity });
      },
      updateQuantity: async (keyOrId, quantity) => {
        if (useServer && typeof keyOrId === 'number') {
          const data = await cartApi.updateItem(keyOrId, quantity);
          dispatch({ type: 'SET', items: normalizeServerCart(data), server: true });
          return;
        }
        if (useServer && typeof keyOrId === 'string') {
          // fallback: find by slug not supported directly; refresh
          await refresh(); return;
        }
        dispatch({ type: 'UPDATE', slug: keyOrId, quantity });
      },
      removeItem: async (keyOrId) => {
        if (useServer && typeof keyOrId === 'number') {
          const data = await cartApi.removeItem(keyOrId);
          dispatch({ type: 'SET', items: normalizeServerCart(data), server: true });
          return;
        }
        dispatch({ type: 'REMOVE', slug: keyOrId });
      },
      clearCart: async () => {
        if (useServer) { try { await cartApi.clearCart(); } catch {} }
        dispatch({ type: 'CLEAR' });
      },
    };
  }, [state.items, state.server, useServer, refresh]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
