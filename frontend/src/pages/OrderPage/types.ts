export type OrderStatus = "PENDING" | "PAID" | "REFUNDED" | "EXPIRED";

export interface OrderSeat {
  rowLabel: string;
  seatNumber: number;
  priceCents: number;
}

export interface Order {
  id: string;
  eventId: string;
  status: OrderStatus;
  section: string;
  seats: OrderSeat[];
  totalCents: number;
}