import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthedFetch } from "../hooks/useAuthedFetch";
import { Navbar } from "../components/Navbar";
import type { EventResponse, Page } from "../types";
import { STATUS_COLOR } from "../types";
import { formatDateTime } from "../lib/util";

export function DashboardPage() {
  const authedFetch = useAuthedFetch();
  const navigate = useNavigate();

  const [events, setEvents] = useState<EventResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    authedFetch<Page<EventResponse>>("/api/admin/events")
      .then((page) => setEvents(page.content))
      .catch(() => setError("Couldn't load events."))
      .finally(() => setLoading(false));
  }, [authedFetch]);

  return (
    <div className="min-h-svh bg-bg">
      <Navbar />

      <div className="mx-auto max-w-6xl px-6 py-12">
        <div className="flex items-center justify-between">
          <h1 className="font-display text-4xl font-bold">Events</h1>
          <button
            onClick={() => navigate("/admin/events/new")}
            className="rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink"
          >
            New event
          </button>
        </div>

        {loading && <p className="mt-8 text-muted">Loading…</p>}
        {error && <p className="mt-8 text-urgent">{error}</p>}

        {!loading && !error && events.length === 0 && (
          <p className="mt-8 text-muted">No events yet.</p>
        )}

        {!loading && !error && events.length > 0 && (
          <ul className="mt-8 max-w-2xl space-y-3">
            {events.map((event) => (
              <li key={event.id}>
                <Link
                  to={`/admin/events/${event.id}`}
                  className="group flex items-center justify-between gap-6 rounded-lg border border-card-border bg-card px-5 py-4 shadow-lg shadow-black/40 transition-all hover:-translate-y-0.5 hover:border-accent/70 hover:bg-card-hover focus-visible:border-accent focus-visible:outline-none"
                >
                  <div>
                    <span className="font-medium">{event.name}</span>
                    <p className="mt-0.5 text-sm text-muted">{event.venue}</p>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <span className="text-sm text-muted">On sale {formatDateTime(event.saleOpensAt)}</span>
                      <p className={`mt-0.5 text-xs font-semibold ${STATUS_COLOR[event.status]}`}>{event.status}</p>
                    </div>
                    <svg
                      aria-hidden="true"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                      className="size-5 text-muted transition-transform group-hover:translate-x-1 group-hover:text-accent"
                    >
                      <path
                        fillRule="evenodd"
                        d="M7.21 14.77a.75.75 0 0 1 .02-1.06L11.17 10 7.23 6.29a.75.75 0 1 1 1.04-1.08l4.5 4.25a.75.75 0 0 1 0 1.08l-4.5 4.25a.75.75 0 0 1-1.06-.02Z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}