import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, apiFetch } from "../../../lib/api";
import { queueSessionKey, saveAdmission } from "../../../lib/queueStorage";
import type { QueueJoinResponse, QueueStatus } from "../types";

export type QueuePhase = "joining" | "waiting" | "dropped" | "error";

const pendingJoins = new Map<string, Promise<string>>();

function getOrJoin(eventId: string): Promise<string> {
  const stored = sessionStorage.getItem(queueSessionKey(eventId));
  if (stored) return Promise.resolve(stored);

  let pending = pendingJoins.get(eventId);
  if (!pending) {
    pending = apiFetch<QueueJoinResponse>(`/api/queue/${eventId}/join`, { method: "POST" })
      .then((res) => {
        sessionStorage.setItem(queueSessionKey(eventId), res.sessionId);
        return res.sessionId;
      })
      .finally(() => pendingJoins.delete(eventId));
    pendingJoins.set(eventId, pending);
  }
  return pending;
}

function pollDelay(position: number): number {
  if (position <= 100) return 3000;
  if (position <= 1000) return 5000;
  return 10000;
}

export function useQueue(eventId: string) {
  const navigate = useNavigate();
  const [phase, setPhase] = useState<QueuePhase>("joining");
  const [position, setPosition] = useState<number | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;

    async function poll(sessionId: string) {
      try {
        const status = await apiFetch<QueueStatus>(`/api/queue/${eventId}/status`, {
          headers: { "X-Queue-Session": sessionId },
        });
        if (cancelled) return;

        if (status.status === "ADMITTED") {
          saveAdmission(eventId, { token: status.admissionToken, expiresAt: status.expiresAt });
          sessionStorage.removeItem(queueSessionKey(eventId));
          navigate(`/events/${eventId}/shop`, { replace: true });
          return;
        }

        setPosition(status.position);
        setPhase("waiting");
        timer = setTimeout(() => poll(sessionId), pollDelay(status.position));
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          sessionStorage.removeItem(queueSessionKey(eventId));
          setPhase("dropped");
        } else {
          timer = setTimeout(() => poll(sessionId), 3000);
        }
      }
    }

    getOrJoin(eventId)
      .then((sessionId) => {
        if (!cancelled) poll(sessionId);
      })
      .catch(() => {
        if (!cancelled) setPhase("error");
      });

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [eventId, attempt, navigate]);

  const rejoin = useCallback(() => {
    sessionStorage.removeItem(queueSessionKey(eventId));
    setPhase("joining");
    setPosition(null);
    setAttempt((a) => a + 1);
  }, [eventId]);

  return { phase, position, rejoin };
}