import React, { createContext, useContext, useMemo, useReducer } from 'react';

const CartContext = createContext(null);

function reducer(state, action) {
  switch (action.type) {
    case 'add': {
      const existing = state[action.product.id];
      const quantity = (existing?.quantity || 0) + 1;
      return { ...state, [action.product.id]: { product: action.product, quantity } };
    }
    case 'set': {
      if (action.quantity <= 0) {
        const { [action.id]: _removed, ...rest } = state;
        return rest;
      }
      return { ...state, [action.id]: { ...state[action.id], quantity: action.quantity } };
    }
    case 'clear':
      return {};
    default:
      return state;
  }
}

export function CartProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, {});
  const value = useMemo(() => {
    const lines = Object.values(state);
    return {
      lines,
      count: lines.reduce((n, l) => n + l.quantity, 0),
      totalCents: lines.reduce((sum, l) => sum + l.quantity * l.product.priceCents, 0),
      add: (product) => dispatch({ type: 'add', product }),
      setQuantity: (id, quantity) => dispatch({ type: 'set', id, quantity }),
      clear: () => dispatch({ type: 'clear' }),
    };
  }, [state]);
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export const useCart = () => useContext(CartContext);
