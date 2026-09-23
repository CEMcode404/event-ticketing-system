import { useEffect, useState } from "react";
import { Modal } from "../../../components/Modal";
import { Button } from "../../../components/Button";
import { TextField } from "../../../components/fields/TextField";
import { NumberField } from "../../../components/fields/NumberField";
import { useAuthedFetch } from "../../../hooks/useAuthedFetch";
import { ApiError } from "../../../lib/api";

interface AddSectionModalProps {
  open: boolean;
  onClose: () => void;
  eventId: string;
  onGenerated: () => void;
}

export function AddSectionModal({ open, onClose, eventId, onGenerated }: AddSectionModalProps) {
  const authedFetch = useAuthedFetch();

  const [section, setSection] = useState("");
  const [rowCount, setRowCount] = useState("");
  const [seatsPerRow, setSeatsPerRow] = useState("");
  const [price, setPrice] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    if (open) {
      setSection("");
      setRowCount("");
      setSeatsPerRow("");
      setPrice("");
      setError(null);
    }
  }, [open]);

  async function handleGenerate(e: React.SubmitEvent) {
    e.preventDefault();
    setError(null);
    setGenerating(true);

    try {
      await authedFetch<unknown[]>("/api/admin/seats/bulk", {
        method: "POST",
        body: {
          eventId,
          section,
          rowCount: Number(rowCount),
          seatsPerRow: Number(seatsPerRow),
          priceCents: Math.round(Number(price) * 100),
        },
      });
      onGenerated();
      onClose();
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("Check the seat details — this request may exceed the per-call limit.");
      } else {
        setError("Something went wrong. Try again.");
      }
    } finally {
      setGenerating(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Add section">
      <form onSubmit={handleGenerate}>
        <TextField label="Section" value={section} onChange={setSection} required placeholder="e.g. VIP" />

        <div className="mt-4 grid grid-cols-2 gap-4">
          <NumberField label="Rows" value={rowCount} onChange={setRowCount} required min="1" />
          <NumberField label="Seats per row" value={seatsPerRow} onChange={setSeatsPerRow} required min="1" />
        </div>

        <NumberField
          label="Price (USD)"
          value={price}
          onChange={setPrice}
          required
          min="0"
          step="0.01"
          className="mt-4"
        />

        {error && <p className="mt-4 text-sm text-urgent">{error}</p>}

        <div className="mt-6 flex gap-3">
          <Button type="submit" size="lg" disabled={generating}>
            {generating ? "Generating…" : "Generate seats"}
          </Button>
          <Button type="button" variant="secondary" size="lg" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </form>
    </Modal>
  );
}