import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthedFetch } from "../hooks/useAuthedFetch";
import { Navbar } from "../components/Navbar";
import { ApiError } from "../lib/api";

export function CreateEventPage() {
  const authedFetch = useAuthedFetch();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [venue, setVenue] = useState("");
  const [description, setDescription] = useState("");
  const [saleOpensAt, setSaleOpensAt] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await authedFetch("/api/admin/events", {
        method: "POST",
        body: {
          name,
          venue,
          description: description.trim() === "" ? null : description,
          saleOpensAt: new Date(saleOpensAt).toISOString(),
        },
      });
      navigate("/admin");
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("Check the event details — name, venue, and sale date (must be in the future) are required.");
      } else {
        setError("Something went wrong. Try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-svh bg-bg">
      <Navbar />

      <div className="mx-auto max-w-6xl px-6 py-12">
        <h1 className="font-display text-4xl font-bold">New event</h1>

        <form onSubmit={handleSubmit} className="mt-8 max-w-lg rounded border border-border bg-bg-panel p-8">
          <label className="block text-sm text-muted">
            Event name
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoFocus
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
              placeholder="e.g. Madison Square Garden"
              className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
            />
          </label>

          <label className="mt-4 block text-sm text-muted">
            Description <span className="text-muted/60">(optional)</span>
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

          {error && <p className="mt-4 text-sm text-urgent">{error}</p>}

          <div className="mt-6 flex gap-3">
            <button
              type="submit"
              disabled={submitting}
              className="cursor-pointer rounded bg-accent px-5 py-2.5 font-semibold text-accent-ink disabled:opacity-60"
            >
              {submitting ? "Creating…" : "Create event"}
            </button>
            <button
              type="button"
              onClick={() => navigate("/admin")}
              className="cursor-pointer rounded border border-border px-5 py-2.5 text-muted"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}