import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Navbar } from "../../components/Navbar";
import { Footer } from "../../components/Footer";
import { formatCountdown } from "../../lib/util";
import { loadAdmission, loadHold, saveHold, type Hold } from "../../lib/queueStorage";
import { useNow } from "../../hooks/useNow";
import { SectionPicker } from "./components/SectionPicker";
import { HoldSummary } from "./components/HoldSummary";

export function ShopPage() {
  const { id = "" } = useParams<{ id: string }>();
  const [admission] = useState(() => loadAdmission(id));
  const [hold, setHold] = useState<Hold | null>(() => loadHold(id));
  const now = useNow(1000);

  const remainingMs = admission ? new Date(admission.expiresAt).getTime() - now : 0;
  const windowEnded = remainingMs <= 0;

  function handleHeld(newHold: Hold) {
    saveHold(id, newHold);
    setHold(newHold);
  }

  return (
    <div className="flex min-h-svh flex-col">
      <Navbar />

      <main className="mx-auto flex w-full max-w-xl flex-1 flex-col px-6 py-16">
        <section className="rounded-lg border border-card-border bg-card px-8 py-10 shadow-lg shadow-black/40">
          {!admission ? (
            <div className="text-center">
              <p className="font-semibold">Join the queue first</p>
              <p className="mt-2 text-sm text-muted">Tickets are available to people admitted from the waiting room.</p>
              <Link
                to={`/events/${id}/queue`}
                className="mt-6 inline-block rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink"
              >
                Go to the queue
              </Link>
            </div>
          ) : windowEnded ? (
            <div className="text-center">
              <p className="font-semibold">Your shopping window has ended</p>
              <p className="mt-2 text-sm text-muted">Rejoin the queue to get another turn.</p>
              <Link
                to={`/events/${id}/queue`}
                className="mt-6 inline-block rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink"
              >
                Rejoin the queue
              </Link>
            </div>
          ) : (
            <>
              <div className="mb-8 flex items-baseline justify-between border-b border-card-border pb-6">
                <p className="text-sm font-semibold uppercase tracking-wider text-accent">You're in</p>
                <p className="font-display text-2xl font-bold tabular-nums">
                  {formatCountdown(remainingMs)} <span className="text-sm font-normal text-muted">left</span>
                </p>
              </div>

              {hold ? (
                <HoldSummary hold={hold} />
              ) : (
                <SectionPicker eventId={id} admissionToken={admission.token} onHeld={handleHeld} />
              )}
            </>
          )}
        </section>
      </main>

      <Footer />
    </div>
  );
}