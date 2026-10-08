export interface Admission {
  token: string;
  expiresAt: string;
}

export interface HeldSeat {
  id: string;
  section: string;
  rowLabel: string;
  seatNumber: number;
  priceCents: number;
}

export interface Hold {
  holdToken: string;
  section: string;
  seats: HeldSeat[];
  totalCents: number;
  expiresAt: string;
}

export const queueSessionKey = (eventId: string) => `queue-session:${eventId}`;
const admissionKey = (eventId: string) => `admission:${eventId}`;
const holdKey = (eventId: string) => `hold:${eventId}`;

export function saveAdmission(eventId: string, admission: Admission) {
  sessionStorage.setItem(admissionKey(eventId), JSON.stringify(admission));
}

export function loadAdmission(eventId: string): Admission | null {
  const raw = sessionStorage.getItem(admissionKey(eventId));
  return raw ? (JSON.parse(raw) as Admission) : null;
}

export function saveHold(eventId: string, hold: Hold) {
  sessionStorage.setItem(holdKey(eventId), JSON.stringify(hold));
}

export function loadHold(eventId: string): Hold | null {
  const raw = sessionStorage.getItem(holdKey(eventId));
  return raw ? (JSON.parse(raw) as Hold) : null;
}