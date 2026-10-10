export type QueueStatus =
  | { status: "WAITING"; position: number; admissionToken: null; expiresAt: null }
  | { status: "ADMITTED"; position: null; admissionToken: string; expiresAt: string };

export interface QueueJoinResponse {
  sessionId: string;
  status: QueueStatus;
}

export interface PublicEvent {
  id: string;
  name: string;
  venue: string;
  description: string | null;
  saleOpensAt: string;
  startsAt: string;
}