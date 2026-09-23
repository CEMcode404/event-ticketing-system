import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuthedFetch } from "../hooks/useAuthedFetch";
import { Navbar } from "../components/Navbar";
import { Modal } from "../components/Modal";
import { ApiError } from "../lib/api";

type EventStatus = "DRAFT" | "PUBLISHED" | "SUSPENDED" | "CANCELLED";

interface EventResponse {
  id: string;
  name: string;
  venue: string;
  description: string | null;
  saleOpensAt: string;
  status: EventStatus;
}

interface SeatSummary {
  section: string;
  count: number;
  priceCents: number;
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

function toDatetimeLocal(iso: string): string {
  const date = new Date(iso);
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60000);
  return local.toISOString().slice(0, 16);
}

function formatPrice(cents: number): string {
  return `$${(cents / 100).toFixed(2)}`;
}

const STATUS_COLOR: Record<EventStatus, string> = {
  DRAFT: "text-muted",
  PUBLISHED: "text-accent",
  SUSPENDED: "text-urgent",
  CANCELLED: "text-urgent",
};

export function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const authedFetch = useAuthedFetch();
  const navigate = useNavigate();

  const [event, setEvent] = useState<EventResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [seatSummary, setSeatSummary] = useState<SeatSummary[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [name, setName] = useState("");
  const [venue, setVenue] = useState("");
  const [description, setDescription] = useState("");
  const [saleOpensAt, setSaleOpensAt] = useState("");
  const [editError, setEditError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [statusError, setStatusError] = useState<string | null>(null);
  const [updatingStatus, setUpdatingStatus] = useState(false);

  const [addSectionOpen, setAddSectionOpen] = useState(false);
  const [section, setSection] = useState("");
  const [rowCount, setRowCount] = useState("");
  const [seatsPerRow, setSeatsPerRow] = useState("");
  const [price, setPrice] = useState("");
  const [seatError, setSeatError] = useState<string | null>(null);
  const [generatingSeats, setGeneratingSeats] = useState(false);

  function loadSummary() {
    authedFetch<SeatSummary[]>(`/api/admin/seats/summary?eventId=${id}`)
      .then(setSeatSummary)
      .catch(() => {});
  }

  useEffect(() => {
    authedFetch<EventResponse>(`/api/admin/events/${id}`)
      .then((data) => {
        setEvent(data);
        setName(data.name);
        setVenue(data.venue);
        setDescription(data.description ?? "");
        setSaleOpensAt(toDatetimeLocal(data.saleOpensAt));
      })
      .catch(() => setLoadError("Couldn't load this event."))
      .finally(() => setLoading(false));

    loadSummary();
  }, [id]);

  function openEdit() {
    if (event) {
      setName(event.name);
      setVenue(event.venue);
      setDescription(event.description ?? "");
      setSaleOpensAt(toDatetimeLocal(event.saleOpensAt));
    }
    setEditError(null);
    setEditOpen(true);
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setEditError(null);
    setSaving(true);

    try {
      const updated = await authedFetch<EventResponse>(`/api/admin/events/${id}`, {
        method: "PUT",
        body: {
          name,
          venue,
          description: description.trim() === "" ? null : description,
          saleOpensAt: new Date(saleOpensAt).toISOString(),
        },
      });
      setEvent(updated);
      setEditOpen(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setEditError("Check the event details — sale date must be in the future.");
      } else {
        setEditError("Something went wrong. Try again.");
      }
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(newStatus: EventStatus) {
    setStatusError(null);
    setUpdatingStatus(true);
    try {
      const updated = await authedFetch<EventResponse>(`/api/admin/events/${id}/status`, {
        method: "PATCH",
        body: { status: newStatus },
      });
      setEvent(updated);
    } catch {
      setStatusError("Couldn't update status.");
    } finally {
      setUpdatingStatus(false);
    }
  }

  function openAddSection() {
    setSection("");
    setRowCount("");
    setSeatsPerRow("");
    setPrice("");
    setSeatError(null);
    setAddSectionOpen(true);
  }

  async function handleGenerateSeats(e: React.FormEvent) {
    e.preventDefault();
    setSeatError(null);
    setGeneratingSeats(true);

    try {
      await authedFetch<unknown[]>("/api/admin/seats/bulk", {
        method: "POST",
        body: {
          eventId: id,
          section,
          rowCount: Number(rowCount),
          seatsPerRow: Number(seatsPerRow),
          priceCents: Math.round(Number(price) * 100),
        },
      });
      setAddSectionOpen(false);
      loadSummary();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setSeatError("Check the seat details — this request may exceed the per-call limit.");
      } else {
        setSeatError("Something went wrong. Try again.");
      }
    } finally {
      setGeneratingSeats(false);
    }
  }

  if (loading) {
    return (
      <div className="min-h-svh bg-bg">
        <Navbar />
        <p className="mx-auto max-w-6xl px-6 py-12 text-muted">Loading…</p>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="min-h-svh bg-bg">
        <Navbar />
        <p className="mx-auto max-w-6xl px-6 py-12 text-urgent">{loadError ?? "Event not found."}</p>
      </div>
    );
  }

  const totalSeats = seatSummary.reduce((sum, s) => sum + s.count, 0);

  return (
    <div className="min-h-svh bg-bg">
      <Navbar />

      <div className="mx-auto max-w-6xl px-6 py-12">
        <button onClick={() => navigate("/admin")} className="cursor-pointer text-sm text-muted">
          ← Back to events
        </button>

        <div className="mt-4 flex items-start justify-between">
          <div>
            <h1 className="font-display text-4xl font-bold">{event.name}</h1>
            <p className="mt-2 text-muted">{event.venue}</p>
            {event.description && <p className="mt-2 max-w-lg text-sm text-muted">{event.description}</p>}
            <p className="mt-2 text-sm text-muted">On sale {formatDateTime(event.saleOpensAt)}</p>
          </div>
          <span className={`rounded border border-border px-3 py-1 text-sm font-semibold ${STATUS_COLOR[event.status]}`}>
            {event.status}
          </span>
        </div>

        <div className="mt-6 flex gap-3">
          {event.status !== "PUBLISHED" && event.status !== "CANCELLED" && (
            <button
              onClick={() => changeStatus("PUBLISHED")}
              disabled={updatingStatus}
              className="cursor-pointer rounded bg-accent px-4 py-2 font-semibold text-accent-ink disabled:opacity-60"
            >
              Publish
            </button>
          )}
          {event.status === "PUBLISHED" && (
            <button
              onClick={() => changeStatus("SUSPENDED")}
              disabled={updatingStatus}
              className="cursor-pointer rounded border border-border px-4 py-2 text-muted disabled:opacity-60"
            >
              Suspend
            </button>
          )}
          {event.status !== "CANCELLED" && (
            <button
              onClick={() => changeStatus("CANCELLED")}
              disabled={updatingStatus}
              className="cursor-pointer rounded border border-urgent px-4 py-2 text-urgent disabled:opacity-60"
            >
              Cancel event
            </button>
          )}
          <button onClick={openEdit} className="cursor-pointer rounded border border-border px-4 py-2 text-muted">
            Edit details
          </button>
        </div>
        {statusError && <p className="mt-3 text-sm text-urgent">{statusError}</p>}

        <div className="mt-10 max-w-2xl">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-2xl font-bold">
              Seats {totalSeats > 0 && <span className="text-muted">({totalSeats} total)</span>}
            </h2>
            <button
              onClick={openAddSection}
              className="cursor-pointer rounded bg-accent px-4 py-2 text-sm font-semibold text-accent-ink"
            >
              Add section
            </button>
          </div>

          {seatSummary.length === 0 && <p className="mt-4 text-sm text-muted">No seats generated yet.</p>}

          {seatSummary.length > 0 && (
            <ul className="mt-4 divide-y divide-border rounded border border-border">
              {seatSummary.map((s) => (
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
      </div>

      <Modal open={editOpen} onClose={() => setEditOpen(false)} title="Edit event">
        <form onSubmit={handleSave}>
          <label className="block text-sm text-muted">
            Event name
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
            />
          </label>

          <label className="mt-4 block text-sm text-muted">
            Venue
            <input
              type="text"
              value={venue}
              onChange={(e) => setVenue(e.target.value)}
              required
              className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
            />
          </label>

          <label className="mt-4 block text-sm text-muted">
            Description
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="mt-1.5 w-full resize-none rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
            />
          </label>

          <label className="mt-4 block text-sm text-muted">
            Sale opens at
            <input
              type="datetime-local"
              value={saleOpensAt}
              onChange={(e) => setSaleOpensAt(e.target.value)}
              required
              className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent [color-scheme:dark]"
            />
          </label>

          {editError && <p className="mt-4 text-sm text-urgent">{editError}</p>}

          <div className="mt-6 flex gap-3">
            <button
              type="submit"
              disabled={saving}
              className="cursor-pointer rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink disabled:opacity-60"
            >
              {saving ? "Saving…" : "Save changes"}
            </button>
            <button
              type="button"
              onClick={() => setEditOpen(false)}
              className="cursor-pointer rounded border border-border px-5 py-2.5 text-muted"
            >
              Cancel
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={addSectionOpen} onClose={() => setAddSectionOpen(false)} title="Add section">
        <form onSubmit={handleGenerateSeats}>
          <label className="block text-sm text-muted">
            Section
            <input
              type="text"
              value={section}
              onChange={(e) => setSection(e.target.value)}
              required
              placeholder="e.g. VIP"
              className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
            />
          </label>

          <div className="mt-4 grid grid-cols-2 gap-4">
            <label className="block text-sm text-muted">
              Rows
              <input
                type="number"
                min="1"
                value={rowCount}
                onChange={(e) => setRowCount(e.target.value)}
                required
                className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
              />
            </label>
            <label className="block text-sm text-muted">
              Seats per row
              <input
                type="number"
                min="1"
                value={seatsPerRow}
                onChange={(e) => setSeatsPerRow(e.target.value)}
                required
                className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
              />
            </label>
          </div>

          <label className="mt-4 block text-sm text-muted">
            Price (USD)
            <input
              type="number"
              min="0"
              step="0.01"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              required
              className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
            />
          </label>

          {seatError && <p className="mt-4 text-sm text-urgent">{seatError}</p>}

          <div className="mt-6 flex gap-3">
            <button
              type="submit"
              disabled={generatingSeats}
              className="cursor-pointer rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink disabled:opacity-60"
            >
              {generatingSeats ? "Generating…" : "Generate seats"}
            </button>
            <button
              type="button"
              onClick={() => setAddSectionOpen(false)}
              className="cursor-pointer rounded border border-border px-5 py-2.5 text-muted"
            >
              Cancel
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}