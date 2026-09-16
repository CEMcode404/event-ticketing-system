import { Navbar } from "../components/Navbar";
import { Footer } from "../components/Footer";
import { EventCard, type EventSummary } from "../components/EventCard";

// Placeholder data — GET /api/events (public, unguarded) doesn't exist yet.
// Shape matches the real Event entity (id, name, saleOpensAt) so swapping
// this for a real fetch later is a drop-in change, not a rewrite.
const MOCK_EVENTS: EventSummary[] = [
  { id: "1", name: "Northbound — Fall Tour", saleOpensAt: "2026-10-14T18:00:00Z" },
  { id: "2", name: "The Salt Line", saleOpensAt: "2026-10-21T18:00:00Z" },
  { id: "3", name: "Hollow Coast", saleOpensAt: "2026-11-02T18:00:00Z" },
  { id: "4", name: "Radio Static Live", saleOpensAt: "2026-11-09T18:00:00Z" },
];

export function HomePage() {
  return (
    <div className="flex min-h-svh flex-col">
      <Navbar />

      <section className="mx-auto grid w-full max-w-6xl grid-cols-1 items-center gap-12 px-6 py-[72px] md:grid-cols-[1.2fr_0.8fr]">
        <div>
          <h1 className="font-display text-5xl font-bold leading-[1.05] tracking-tight md:text-7xl">
            Find your
            <br />
            next show.
          </h1>
          <p className="mt-6 max-w-[46ch] text-lg text-muted">
            Fair queues, real seats, no bots. A waiting room that actually holds the line.
          </p>
          <a
            href="#events"
            className="mt-8 inline-block rounded bg-accent px-7 py-3.5 font-semibold text-accent-ink"
          >
            Browse events
          </a>
        </div>
        <div className="relative hidden h-[260px] rotate-[-4deg] rounded border border-border bg-bg-panel md:block">
          <div className="hero-stub-perforation" />
        </div>
      </section>

      <section id="events" className="mx-auto w-full max-w-6xl flex-1 px-6 pb-24">
        <h2 className="font-display text-4xl font-bold">Upcoming</h2>
        <p className="mt-2 mb-8 text-muted">Sorted by when the on-sale opens.</p>
        <div className="grid grid-cols-[repeat(auto-fill,minmax(240px,1fr))] gap-5">
          {MOCK_EVENTS.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      </section>

      <Footer />
    </div>
  );
}