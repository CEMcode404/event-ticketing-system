import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Navbar } from "../../components/Navbar";
import { Footer } from "../../components/Footer";
import { apiFetch } from "../../lib/api";
import { formatPrice } from "../../lib/util";
import { clearShopping } from "../../lib/queueStorage";
import type { Order } from "./types";

const POLL_INTERVAL_MS = 2000;
const MAX_PENDING_POLLS = 15;

export function OrderPage() {
  const { id = "", orderId = "" } = useParams<{ id: string; orderId: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [stillPendingAfterWaiting, setStillPendingAfterWaiting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let pendingPolls = 0;

    async function poll() {
      try {
        const latest = await apiFetch<Order>(`/api/orders/${orderId}`);
        if (cancelled) return;
        setOrder(latest);

        if (latest.status === "PAID") {
          clearShopping(id);
          return;
        }
        if (latest.status !== "PENDING") return;

        pendingPolls += 1;
        if (pendingPolls >= MAX_PENDING_POLLS) {
          setStillPendingAfterWaiting(true);
          return;
        }
        timer = setTimeout(poll, POLL_INTERVAL_MS);
      } catch {
        if (!cancelled) setLoadFailed(true);
      }
    }

    poll();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [id, orderId]);

  return (
    <div className="flex min-h-svh flex-col">
      <Navbar />

      <main className="mx-auto flex w-full max-w-xl flex-1 flex-col px-6 py-16">
        <section className="rounded-lg border border-card-border bg-card px-8 py-10 shadow-lg shadow-black/40">
          {loadFailed && <p className="text-center text-urgent">We couldn't find this order.</p>}

          {!loadFailed && (!order || order.status === "PENDING") && (
            <div className="text-center">
              <p className="font-semibold">Confirming your payment…</p>
              <p className="mt-2 text-sm text-muted">
                {stillPendingAfterWaiting
                  ? "This is taking longer than usual. Your payment is safe; refresh this page in a minute."
                  : "This usually takes just a few seconds."}
              </p>
            </div>
          )}

          {order?.status === "PAID" && <PaidOrder order={order} />}

          {order?.status === "REFUNDED" && (
            <div className="text-center">
              <p className="font-semibold">Your payment was refunded</p>
              <p className="mt-2 text-sm text-muted">
                Your hold ended before payment finished and the seats were taken, so you were refunded in full.
              </p>
            </div>
          )}

          {order?.status === "EXPIRED" && (
            <div className="text-center">
              <p className="font-semibold">Checkout expired</p>
              <p className="mt-2 text-sm text-muted">This checkout wasn't completed, and you weren't charged.</p>
            </div>
          )}
        </section>

        <Link to="/" className="mt-8 text-center text-sm text-muted">
          ← Back to events
        </Link>
      </main>

      <Footer />
    </div>
  );
}

function PaidOrder({ order }: { order: Order }) {
  return (
    <div>
      <p className="text-sm font-semibold uppercase tracking-wider text-accent">You're going!</p>
      <p className="mt-3 font-display text-2xl font-bold">{order.section}</p>

      <ul className="mt-4 space-y-1 text-muted">
        {order.seats.map((seat) => (
          <li key={`${seat.rowLabel}-${seat.seatNumber}`}>
            Row {seat.rowLabel} · Seat {seat.seatNumber}
          </li>
        ))}
      </ul>

      <div className="mt-6 flex items-center justify-between border-t border-card-border pt-4">
        <span className="text-muted">Total paid</span>
        <span className="text-lg font-semibold">{formatPrice(order.totalCents)}</span>
      </div>

      <p className="mt-4 text-xs text-muted">Order {order.id}</p>
    </div>
  );
}