export type OrderStage = "Pending" | "Confirmed" | "Paid" | "Shipping" | "Completed";

export type OrderStatus = "Pending" | "Paid" | "Shipping" | "Completed" | "Cancelled";

export type PaymentState = "Pending" | "Paid" | "Refunded";

export interface OrderItem {
  id: string;
  name: string;
  variant: string;
  quantity: number;
  price: number;
  image: string;
}

export interface OrderSummary {
  subtotal: number;
  shipping: number;
  discount: number;
  total: number;
}

export interface OrderRecord {
  id: string;
  code: string;
  placedOn: string;
  status: OrderStatus;
  paymentState: PaymentState;
  total: number;
  stage: OrderStage;
  customerName: string;
  shippingAddress: string[];
  paymentInfo: {
    method: string;
    status: PaymentState;
    transaction: string;
  };
  summary: OrderSummary;
  items: OrderItem[];
}
