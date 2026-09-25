import { useEffect, useState } from "react";
import { Navbar } from "../components/Navbar";
import { Footer } from "../components/Footer";
import { EventCard, type EventSummary } from "../components/EventCard";
import { apiFetch } from "../lib/api";
import type { Page } from "../types";

export function HomePage() {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Page<EventSummary>>("/api/events")
      .then((page) => setEvents(page.content))
      .catch(() => setError("Couldn't load events."))
      .finally(() => setLoading(false));
  }, []);

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
        <p className="mt-2 mb-8 text-muted">Catch these before the queue opens.</p>

        {loading && <p className="text-muted">Loading…</p>}
        {error && <p className="text-urgent">{error}</p>}
        {!loading && !error && events.length === 0 && (
          <p className="text-muted">No upcoming events yet.</p>
        )}

        {!loading && !error && events.length > 0 && (
          <div className="grid grid-cols-[repeat(auto-fill,minmax(240px,1fr))] gap-5">
            {events.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>
        )}
      </section>

      <Footer />
    </div>
  );
}