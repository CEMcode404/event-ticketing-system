import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthedFetch } from "../hooks/useAuthedFetch";
import { Navbar } from "../components/Navbar";

type EventStatus = "DRAFT" | "PUBLISHED" | "SUSPENDED" | "CANCELLED";

interface EventResponse {
  id: string;
  name: string;
  venue: string;
  description: string | null;
  saleOpensAt: string;
  status: EventStatus;
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

// Status text color only, for now — actions (publish/suspend/cancel
// buttons) are a separate follow-up, not built yet.
const STATUS_COLOR: Record<EventStatus, string> = {
  DRAFT: "text-muted",
  PUBLISHED: "text-accent",
  SUSPENDED: "text-urgent",
  CANCELLED: "text-urgent",
};

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
  }, []);

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
          <ul className="mt-8 max-w-2xl divide-y divide-border rounded border border-border">
            {events.map((event) => (
              <li
                key={event.id}
                onClick={() => navigate(`/admin/events/${event.id}`)}
                className="flex cursor-pointer items-center justify-between px-5 py-4 hover:bg-bg-panel"
              >
                <div>
                  <span className="font-medium">{event.name}</span>
                  <p className="mt-0.5 text-sm text-muted">{event.venue}</p>
                </div>
                <div className="text-right">
                  <span className="text-sm text-muted">On sale {formatDateTime(event.saleOpensAt)}</span>
                  <p className={`mt-0.5 text-xs font-semibold ${STATUS_COLOR[event.status]}`}>{event.status}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}