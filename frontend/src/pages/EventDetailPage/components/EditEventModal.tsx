import { useState } from "react";
import { Modal } from "../../../components/Modal";
import { Button } from "../../../components/Button";
import { TextField } from "../../../components/fields/TextField";
import { TextAreaField } from "../../../components/fields/TextAreaField";
import { DateTimeField } from "../../../components/fields/DateTimeField";
import { useAuthedFetch } from "../../../hooks/useAuthedFetch";
import { ApiError } from "../../../lib/api";
import type { EventResponse } from "../../../types";
import { toDatetimeLocal } from "../../../lib/util";

interface EditEventModalProps {
  open: boolean;
  onClose: () => void;
  event: EventResponse;
  onSaved: (updated: EventResponse) => void;
}

export function EditEventModal({ open, onClose, event, onSaved }: EditEventModalProps) {
  const authedFetch = useAuthedFetch();

  const [name, setName] = useState(event.name);
  const [venue, setVenue] = useState(event.venue);
  const [description, setDescription] = useState(event.description ?? "");
  const [saleOpensAt, setSaleOpensAt] = useState(toDatetimeLocal(event.saleOpensAt));
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);


  async function handleSave(e: React.SubmitEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);

    try {
      const updated = await authedFetch<EventResponse>(`/api/admin/events/${event.id}`, {
        method: "PUT",
        body: {
          name,
          venue,
          description: description.trim() === "" ? null : description,
          saleOpensAt: new Date(saleOpensAt).toISOString(),
        },
      });
      onSaved(updated);
      onClose();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("Check the event details — sale date must be in the future.");
      } else {
        setError("Something went wrong. Try again.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Edit event">
      <form onSubmit={handleSave}>
        <TextField label="Event name" value={name} onChange={setName} required />
        <TextField label="Venue" value={venue} onChange={setVenue} required className="mt-4" />
        <TextAreaField label="Description" value={description} onChange={setDescription} className="mt-4" />
        <DateTimeField label="Sale opens at" value={saleOpensAt} onChange={setSaleOpensAt} required className="mt-4" />

        {error && <p className="mt-4 text-sm text-urgent">{error}</p>}

        <div className="mt-6 flex gap-3">
          <Button type="submit" size="lg" disabled={saving}>
            {saving ? "Saving…" : "Save changes"}
          </Button>
          <Button type="button" variant="secondary" size="lg" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </form>
    </Modal>
  );
}