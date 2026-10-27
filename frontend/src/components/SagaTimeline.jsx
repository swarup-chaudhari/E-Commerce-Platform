import React from 'react';

/** Derives the saga's progress from the order's event-sourced history. */
const STEPS = [
  { key: 'OrderCreatedEvent', label: 'Order placed' },
  { key: 'InventoryReservedEvent', label: 'Stock reserved' },
  { key: 'PaymentCompletedEvent', label: 'Payment captured' },
  { key: 'OrderConfirmedEvent', label: 'Order confirmed' },
];

const FAILURES = {
  InventoryReservationFailedEvent: 'Stock unavailable',
  PaymentFailedEvent: 'Payment declined',
  OrderRejectedEvent: 'Order rejected',
  OrderCancelledEvent: 'Order cancelled',
};

export default function SagaTimeline({ events }) {
  const seen = new Set((events || []).map((e) => e.eventType));
  const failure = Object.keys(FAILURES).filter((k) => seen.has(k));
  const firstMissing = STEPS.findIndex((s) => !seen.has(s.key));

  return (
    <ol className="timeline">
      {STEPS.map((step, i) => {
        const done = seen.has(step.key);
        const active = !failure.length && i === firstMissing;
        return (
          <li key={step.key} className={done ? 'done' : active ? 'active' : ''}>
            <span className="dot" />
            {step.label}
          </li>
        );
      })}
      {failure.map((k) => (
        <li key={k} className="failed">
          <span className="dot" />
          {FAILURES[k]}
        </li>
      ))}
    </ol>
  );
}
