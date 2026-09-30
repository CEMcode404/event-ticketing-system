export interface Admission {
  token: string;
  expiresAt: string;
}

export const queueSessionKey = (eventId: string) => `queue-session:${eventId}`;
const admissionKey = (eventId: string) => `admission:${eventId}`;

export function saveAdmission(eventId: string, admission: Admission) {
  sessionStorage.setItem(admissionKey(eventId), JSON.stringify(admission));
}

export function loadAdmission(eventId: string): Admission | null {
  const raw = sessionStorage.getItem(admissionKey(eventId));
  return raw ? (JSON.parse(raw) as Admission) : null;
}