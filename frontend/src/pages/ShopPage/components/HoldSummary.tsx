import { Button } from "../../../components/Button";
import { formatPrice } from "../../../lib/util";
import type { HeldSeat, Hold } from "../../../lib/queueStorage";

function describeSeats(seats: HeldSeat[]): string {
  const sorted = [...seats].sort((a, b) => a.seatNumber - b.seatNumber);
  const first = sorted[0];
  const last = sorted[sorted.length - 1];
  const seatText = sorted.length === 1 ? `Seat ${first.seatNumber}` : `Seats ${first.seatNumber}–${last.seatNumber}`;
  return `Row ${first.rowLabel} · ${seatText}`;
}

export function HoldSummary({ hold }: { hold: Hold }) {
  const pricePerSeat = hold.seats[0].priceCents;

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
        <Button size="lg" disabled>
          Checkout (coming next)
        </Button>
      </div>
      <p className="mt-3 text-sm text-muted">These seats are held for you until your shopping window ends.</p>
    </div>
  );
}