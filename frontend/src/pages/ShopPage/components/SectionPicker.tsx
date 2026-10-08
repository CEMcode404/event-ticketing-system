import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "../../../components/Button";
import { ApiError, apiFetch } from "../../../lib/api";
import { formatPrice } from "../../../lib/util";
import type { Hold } from "../../../lib/queueStorage";
import type { SectionAvailability } from "../types";

const MAX_TICKETS = 8;

interface SectionPickerProps {
  eventId: string;
  admissionToken: string;
  onHeld: (hold: Hold) => void;
}

export function SectionPicker({ eventId, admissionToken, onHeld }: SectionPickerProps) {
  const [sections, setSections] = useState<SectionAvailability[] | null>(null);
  const [admissionEnded, setAdmissionEnded] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [reloadCount, setReloadCount] = useState(0);

  const [selectedSection, setSelectedSection] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(2);
  const [holding, setHolding] = useState(false);
  const [holdError, setHoldError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<SectionAvailability[]>(`/api/events/${eventId}/sections`, {
      headers: { "X-Admission-Token": admissionToken },
    })
      .then(setSections)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 403) {
          setAdmissionEnded(true);
        } else {
          setLoadFailed(true);
        }
      });
  }, [eventId, admissionToken, reloadCount]);

  const selected = sections?.find((s) => s.section === selectedSection) ?? null;
  const maxQuantity = selected ? Math.min(MAX_TICKETS, selected.available) : MAX_TICKETS;
  const ticketCount = Math.max(1, Math.min(quantity, maxQuantity));

  async function holdSeats() {
    if (!selected) return;
    setHolding(true);
    setHoldError(null);
    try {
      const hold = await apiFetch<Hold>(`/api/events/${eventId}/holds`, {
        method: "POST",
        headers: { "X-Admission-Token": admissionToken },
        body: { section: selected.section, quantity: ticketCount },
      });
      onHeld(hold);
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setAdmissionEnded(true);
      } else if (err instanceof ApiError && err.status === 409) {
        setHoldError(
          `Couldn't find ${ticketCount} seats together in ${selected.section}. Try fewer tickets or another section.`,
        );
        setReloadCount((n) => n + 1);
      } else {
        setHoldError("Something went wrong. Try again.");
      }
    } finally {
      setHolding(false);
    }
  }

  if (admissionEnded) {
    return (
      <div className="text-center">
        <p className="font-semibold">Your shopping window has ended</p>
        <Link
          to={`/events/${eventId}/queue`}
          className="mt-6 inline-block rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink"
        >
          Rejoin the queue
        </Link>
      </div>
    );
  }

  if (loadFailed) return <p className="text-center text-urgent">Couldn't load sections.</p>;
  if (!sections) return <p className="text-center text-muted">Loading sections…</p>;
  if (sections.length === 0) return <p className="text-center text-muted">No tickets have been released yet.</p>;

  return (
    <div>
      <p className="text-sm font-semibold uppercase tracking-wider text-muted">Choose a section</p>
      <ul className="mt-3 space-y-2">
        {sections.map((s) => {
          const soldOut = s.available === 0;
          const isSelected = s.section === selectedSection;
          return (
            <li key={s.section}>
              <button
                type="button"
                disabled={soldOut}
                onClick={() => setSelectedSection(s.section)}
                className={`flex w-full items-center justify-between rounded-lg border px-4 py-3 text-left transition-colors ${
                  isSelected ? "border-accent bg-white/5" : "border-card-border hover:border-accent/50"
                } disabled:cursor-not-allowed disabled:opacity-40`}
              >
                <span className="font-medium">{s.section}</span>
                <span className="text-sm text-muted">
                  {formatPrice(s.priceCents)} · {soldOut ? "Sold out" : `${s.available} left`}
                </span>
              </button>
            </li>
          );
        })}
      </ul>

      {selected && (
        <div className="mt-6">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold uppercase tracking-wider text-muted">Tickets</span>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setQuantity(ticketCount - 1)}
                disabled={ticketCount <= 1}
                className="size-9 rounded border border-card-border text-lg disabled:opacity-40"
              >
                −
              </button>
              <span className="w-6 text-center text-lg font-semibold tabular-nums">{ticketCount}</span>
              <button
                type="button"
                onClick={() => setQuantity(ticketCount + 1)}
                disabled={ticketCount >= maxQuantity}
                className="size-9 rounded border border-card-border text-lg disabled:opacity-40"
              >
                +
              </button>
            </div>
          </div>

          <div className="mt-6">
            <Button size="lg" onClick={holdSeats} disabled={holding}>
              {holding
                ? "Finding seats…"
                : `Hold ${ticketCount} seat${ticketCount > 1 ? "s" : ""} · ${formatPrice(selected.priceCents * ticketCount)}`}
            </Button>
          </div>
        </div>
      )}

      {holdError && <p className="mt-4 text-sm text-urgent">{holdError}</p>}
    </div>
  );
}