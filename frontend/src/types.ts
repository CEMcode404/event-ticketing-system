export type EventStatus = "DRAFT" | "PUBLISHED" | "SUSPENDED" | "CANCELLED";

export interface EventResponse {
  id: string;
  name: string;
  venue: string;
  description: string | null;
  saleOpensAt: string;
  status: EventStatus;
}

export const STATUS_COLOR: Record<EventStatus, string> = {
  DRAFT: "text-muted",
  PUBLISHED: "text-accent",
  SUSPENDED: "text-urgent",
  CANCELLED: "text-urgent",
};