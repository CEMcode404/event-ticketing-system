import { useState, type SubmitEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ApiError } from "../lib/api";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: SubmitEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      await login(username, password);
      navigate("/admin");
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError("Incorrect username or password.");
      } else {
        setError("Something went wrong. Try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-svh flex-col items-center justify-center bg-bg px-6">
      <a href="/" className="mb-8 font-display text-2xl font-bold">
        Lineup
      </a>

      <form onSubmit={handleSubmit} className="w-full max-w-sm rounded border border-border bg-bg-panel p-8">
        <h1 className="font-display text-2xl font-bold">Admin sign in</h1>

        <label className="mt-6 block text-sm text-muted">
          Username
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoFocus
            className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
          />
        </label>

        <label className="mt-4 block text-sm text-muted">
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="mt-1.5 w-full rounded border border-border bg-bg px-3 py-2 text-ink outline-none focus:border-accent"
          />
        </label>

        {error && <p className="mt-4 text-sm text-urgent">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="mt-6 w-full rounded bg-accent py-2.5 font-semibold text-accent-ink disabled:opacity-60"
        >
          {submitting ? "Signing in…" : "Sign in"}
        </button>
      </form>
    </div>
  );
}