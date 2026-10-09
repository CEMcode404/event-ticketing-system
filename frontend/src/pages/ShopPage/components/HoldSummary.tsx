import { useState } from "react";
import { Button } from "../../../components/Button";
import { ApiError, apiFetch } from "../../../lib/api";
import { formatPrice } from "../../../lib/util";
import type { HeldSeat, Hold } from "../../../lib/queueStorage";
import type { CheckoutResponse } from "../types";

function describeSeats(seats: HeldSeat[]): string {
  const sorted = [...seats].sort((a, b) => a.seatNumber - b.seatNumber);
  const first = sorted[0];
  const last = sorted[sorted.length - 1];
  const seatText = sorted.length === 1 ? `Seat ${first.seatNumber}` : `Seats ${first.seatNumber}–${last.seatNumber}`;
  return `Row ${first.rowLabel} · ${seatText}`;
}

function checkoutErrorMessage(err: unknown): string {
  if (err instanceof ApiError && err.status === 403) {
    return "Your shopping window has ended. Rejoin the queue to try again.";
  }
  if (err instanceof ApiError && err.status === 409) {
    return "Your hold has expired. Please choose seats again.";
  }
  return "Couldn't start checkout. Please try again.";
}

interface HoldSummaryProps {
  eventId: string;
  admissionToken: string;
  hold: Hold;
}

export function HoldSummary({ eventId, admissionToken, hold }: HoldSummaryProps) {
  const [redirecting, setRedirecting] = useState(false);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);
  const pricePerSeat = hold.seats[0].priceCents;

  async function startCheckout() {
    setRedirecting(true);
    setCheckoutError(null);
    try {
      const { checkoutUrl } = await apiFetch<CheckoutResponse>(`/api/events/${eventId}/checkout`, {
        method: "POST",
        headers: { "X-Admission-Token": admissionToken },
        body: { holdToken: hold.holdToken, seatIds: hold.seats.map((s) => s.id) },
      });
      window.location.assign(checkoutUrl);
    } catch (err) {
      setCheckoutError(checkoutErrorMessage(err));
      setRedirecting(false);
    }
  }

  return (
    <div>
      <p className="text-sm font-semibold uppercase tracking-wider text-muted">Your seats</p>
      <p className="mt-3 font-display text-2xl font-bold">{hold.section}</p>
      <p className="mt-1 text-muted">{describeSeats(hold.seats)}</p>

      <div className="mt-6 flex items-center justify-between border-t border-card-border pt-4">
        <span className="text-muted">
          {hold.seats.length} × {formatPrice(pricePerSeat)}
        </span>
        <span className="text-lg font-semibold">{formatPrice(hold.totalCents)}</span>
      </div>

      <div className="mt-6">
        <Button size="lg" onClick={startCheckout} disabled={redirecting}>
          {redirecting ? "Redirecting to payment…" : `Checkout · ${formatPrice(hold.totalCents)}`}
        </Button>
      </div>

      {checkoutError && <p className="mt-4 text-sm text-urgent">{checkoutError}</p>}
      <p className="mt-3 text-sm text-muted">These seats are held for you until your shopping window ends.</p>
    </div>
  );
}