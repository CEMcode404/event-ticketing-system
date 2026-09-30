import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Navbar } from "../../components/Navbar";
import { Footer } from "../../components/Footer";
import { Button } from "../../components/Button";
import { apiFetch } from "../../lib/api";
import { formatCountdown, formatDateTime } from "../../lib/util";
import { useNow } from "../../hooks/useNow";
import { useQueue } from "./hooks/useQueue"; 
import type { PublicEvent } from "./types";

export function QueuePage() {
  const { id = "" } = useParams<{ id: string }>();
  const [event, setEvent] = useState<PublicEvent | null>(null);
  const [eventError, setEventError] = useState(false);
  const { phase, position, rejoin } = useQueue(id);
  const now = useNow(1000);

  useEffect(() => {
    apiFetch<PublicEvent>(`/api/events/${id}`)
      .then(setEvent)
      .catch(() => setEventError(true));
  }, [id]);

  const saleOpensAtMs = event ? new Date(event.saleOpensAt).getTime() : null;
  const beforeSale = saleOpensAtMs !== null && now < saleOpensAtMs;

  return (
    <div className="flex min-h-svh flex-col">
      <Navbar />

      <main className="mx-auto flex w-full max-w-xl flex-1 flex-col px-6 py-16">
        {eventError ? (
          <p className="text-urgent">This event isn't available.</p>
        ) : (
          <>
            {event && (
              <header className="mb-10 text-center">
                <h1 className="font-display text-4xl font-bold">{event.name}</h1>
                <p className="mt-2 text-muted">{event.venue}</p>
              </header>
            )}

            <section className="rounded-lg border border-card-border bg-card px-8 py-10 text-center shadow-lg shadow-black/40">
              {phase === "joining" && <p className="text-muted">Joining the waiting room…</p>}

              {phase === "waiting" && event && beforeSale && saleOpensAtMs !== null && (
                <>
                  <p className="text-sm font-semibold uppercase tracking-wider text-accent">Waiting room</p>
                  <p className="mt-4 font-display text-5xl font-bold tabular-nums">
                    {formatCountdown(saleOpensAtMs - now)}
                  </p>
                  <p className="mt-2 text-muted">until the sale opens at {formatDateTime(event.saleOpensAt)}</p>
                  <p className="mx-auto mt-6 max-w-sm text-sm text-muted">
                    Your place in line is assigned randomly when the sale opens, so there's no advantage to
                    joining early. Keep this tab open; leaving gives up your spot.
                  </p>
                </>
              )}

              {phase === "waiting" && !beforeSale && position !== null && (
                <>
                  <p className="text-sm font-semibold uppercase tracking-wider text-accent">You're in line</p>
                  <p className="mt-4 font-display text-6xl font-bold tabular-nums">{position.toLocaleString()}</p>
                  <p className="mt-2 text-muted">
                    {position === 1 ? "You're next" : `${(position - 1).toLocaleString()} ahead of you`}
                  </p>
                  <p className="mx-auto mt-6 max-w-sm text-sm text-muted">
                    Keep this tab open. We'll take you to the tickets automatically when it's your turn.
                  </p>
                </>
              )}

              {phase === "dropped" && (
                <>
                  <p className="font-semibold">You left the line</p>
                  <p className="mt-2 text-sm text-muted">
                    Your spot was released because this page stopped checking in. Rejoining puts you at the back.
                  </p>
                  <div className="mt-6">
                    <Button onClick={rejoin}>Rejoin the queue</Button>
                  </div>
                </>
              )}

              {phase === "error" && (
                <>
                  <p className="font-semibold text-urgent">Couldn't join the queue</p>
                  <p className="mt-2 text-sm text-muted">The event may not be on sale yet, or something went wrong.</p>
                  <div className="mt-6">
                    <Button onClick={rejoin}>Try again</Button>
                  </div>
                </>
              )}
            </section>

            <Link to="/" className="mt-8 text-center text-sm text-muted">
              ← Back to events
            </Link>
          </>
        )}
      </main>

      <Footer />
    </div>
  );
}