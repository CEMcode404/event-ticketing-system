import { useEffect, useState } from "react";
import { useAuthedFetch } from "../hooks/useAuthedFetch";
import { Navbar } from "../components/Navbar";

interface EventResponse {
  id: string;
  name: string;
  saleOpensAt: string;
  createdAt: string;
}

interface Page<T> {
  content: T[];
  totalElements: number;
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export function DashboardPage() {
  const authedFetch = useAuthedFetch();

  const [events, setEvents] = useState<EventResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    authedFetch<Page<EventResponse>>("/api/admin/events")
      .then((page) => setEvents(page.content))
      .catch(() => setError("Couldn't load events."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-svh bg-bg">
      <Navbar />

      <div className="mx-auto max-w-3xl px-6 py-12">
        <div className="flex items-center justify-between">
          <h1 className="font-display text-4xl font-bold">Events</h1>
          {/* TODO: hook up once the create-event form exists */}
          <button className="rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink">
            New event
          </button>
        </div>

        {loading && <p className="mt-8 text-muted">Loading…</p>}
        {error && <p className="mt-8 text-urgent">{error}</p>}

        {!loading && !error && events.length === 0 && (
          <p className="mt-8 text-muted">No events yet.</p>
        )}

        {!loading && !error && events.length > 0 && (
          <ul className="mt-8 divide-y divide-border rounded border border-border">
            {events.map((event) => (
              <li key={event.id} className="flex items-center justify-between px-5 py-4">
                <span className="font-medium">{event.name}</span>
                <span className="text-sm text-muted">On sale {formatDateTime(event.saleOpensAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}