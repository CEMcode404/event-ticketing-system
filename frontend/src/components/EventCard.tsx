export interface EventSummary {
  id: string;
  name: string;
  venue: string;
  saleOpensAt: string;
}

function formatSaleDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function EventCard({ event }: { event: EventSummary }) {
  return (
    <article className="relative overflow-hidden rounded border border-border bg-bg-panel px-5 pb-5 pt-7">
      <div className="ticket-perforation top-0 left-0 right-0" aria-hidden="true" />
      <h3 className="mt-2 text-xl font-display font-bold">{event.name}</h3>
      <p className="mt-1 text-sm text-muted">{event.venue}</p>
      <p className="mt-2.5 text-sm text-muted">On sale {formatSaleDate(event.saleOpensAt)}</p>
    </article>
  );
}