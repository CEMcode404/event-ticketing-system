import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Navbar } from "../components/Navbar";
import { Footer } from "../components/Footer";
import { formatCountdown } from "../lib/util";
import { loadAdmission } from "../lib/queueStorage";
import { useNow } from "../hooks/useNow";

export function ShopPage() {
  const { id = "" } = useParams<{ id: string }>();
  const [admission] = useState(() => loadAdmission(id));
  const now = useNow(1000);

  const remainingMs = admission ? new Date(admission.expiresAt).getTime() - now : 0;
  const expired = remainingMs <= 0;

  return (
    <div className="flex min-h-svh flex-col">
      <Navbar />

      <main className="mx-auto flex w-full max-w-xl flex-1 flex-col px-6 py-16">
        <section className="rounded-lg border border-card-border bg-card px-8 py-10 text-center shadow-lg shadow-black/40">
          {expired ? (
            <>
              <p className="font-semibold">Your shopping window has ended</p>
              <p className="mt-2 text-sm text-muted">Rejoin the queue to get another turn.</p>
              <Link
                to={`/events/${id}/queue`}
                className="mt-6 inline-block rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink"
              >
                Rejoin the queue
              </Link>
            </>
          ) : (
            <>
              <p className="text-sm font-semibold uppercase tracking-wider text-accent">You're in</p>
              <p className="mt-4 font-display text-5xl font-bold tabular-nums">{formatCountdown(remainingMs)}</p>
              <p className="mt-2 text-muted">left to choose your tickets</p>
              <p className="mx-auto mt-6 max-w-sm text-sm text-muted">Section selection is coming next.</p>
            </>
          )}
        </section>
      </main>

      <Footer />
    </div>
  );
}