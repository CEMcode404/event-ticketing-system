import { Button } from "../../../components/Button";
import { formatPrice } from "../../../lib/util";
import type { SeatSummary } from "../types";

interface SeatsPanelProps {
  summary: SeatSummary[];
  onAddSection: () => void;
}

export function SeatsPanel({ summary, onAddSection }: SeatsPanelProps) {
  const totalSeats = summary.reduce((sum, s) => sum + s.count, 0);

  return (
    <div className="mt-10 max-w-2xl">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-2xl font-bold">
          Seats {totalSeats > 0 && <span className="text-muted">({totalSeats} total)</span>}
        </h2>
        <Button onClick={onAddSection} className="text-sm">
          Add section
        </Button>
      </div>

      {summary.length === 0 && <p className="mt-4 text-sm text-muted">No seats generated yet.</p>}

      {summary.length > 0 && (
        <ul className="mt-4 divide-y divide-border rounded border border-border">
          {summary.map((s) => (
            <li key={s.section} className="flex items-center justify-between px-5 py-4">
              <span className="font-medium">{s.section}</span>
              <span className="text-sm text-muted">
                {s.count} seats · {formatPrice(s.priceCents)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}