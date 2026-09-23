import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuthedFetch } from "../../hooks/useAuthedFetch";
import { Navbar } from "../../components/Navbar";
import { EditEventModal } from "./components/EditEventModal";
import { AddSectionModal } from "./components/AddSectionModal";
import { StatusActions } from "./components/StatusActions";
import { SeatsPanel } from "./components/SeatsPanel";
import type { EventResponse, EventStatus } from "../../types";
import { STATUS_COLOR } from "../../types";
import { formatDateTime } from "../../lib/util";
import type { SeatSummary } from "./types";

export function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const authedFetch = useAuthedFetch();
  const navigate = useNavigate();

  const [event, setEvent] = useState<EventResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [seatSummary, setSeatSummary] = useState<SeatSummary[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [addSectionOpen, setAddSectionOpen] = useState(false);

  const [statusError, setStatusError] = useState<string | null>(null);
  const [updatingStatus, setUpdatingStatus] = useState(false);

  function loadSummary() {
    authedFetch<SeatSummary[]>(`/api/admin/seats/summary?eventId=${id}`)
      .then(setSeatSummary)
      .catch(() => {});
  }

  useEffect(() => {
    authedFetch<EventResponse>(`/api/admin/events/${id}`)
      .then(setEvent)
      .catch(() => setLoadError("Couldn't load this event."))
      .finally(() => setLoading(false));

    loadSummary();
  }, [id]);

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

        <div className="mt-6">
          <StatusActions
            status={event.status}
            updating={updatingStatus}
            onChangeStatus={changeStatus}
            onEdit={() => setEditOpen(true)}
          />
        </div>
        {statusError && <p className="mt-3 text-sm text-urgent">{statusError}</p>}

        <SeatsPanel summary={seatSummary} onAddSection={() => setAddSectionOpen(true)} />
      </div>

      <EditEventModal open={editOpen} onClose={() => setEditOpen(false)} event={event} onSaved={setEvent} />

      <AddSectionModal
        open={addSectionOpen}
        onClose={() => setAddSectionOpen(false)}
        eventId={event.id}
        onGenerated={loadSummary}
      />
    </div>
  );
}