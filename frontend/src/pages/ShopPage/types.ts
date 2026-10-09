export interface SectionAvailability {
  section: string;
  available: number;
  priceCents: number;
}

export interface CheckoutResponse {
  orderId: string;
  checkoutUrl: string;
}