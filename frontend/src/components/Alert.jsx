import React from 'react';

export default function Alert({ error, kind = 'error', children }) {
  const text = children || error?.message;
  if (!text) return null;
  return <div className={`alert alert-${kind}`}>{text}</div>;
}
