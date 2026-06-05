import { mockOrders } from "./data";
import type { OrderRecord } from "./types";

const API_BASE = import.meta.env.VITE_ORDER_API_BASE ?? "";

interface BackendApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}

interface BackendOrderItem {
  id: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

interface BackendOrder {
  id: string;
  orderCode: string;
  totalAmount: number;
  status: string;
  shippingAddress: string;
  createdAt?: string;
  items: BackendOrderItem[];
}

export async function loadOrders(): Promise<OrderRecord[]> {
  try {
    const response = await fetch(`${API_BASE}/api/v1/orders`);
    if (!response.ok) {
      return mockOrders;
    }

    const payload = (await response.json()) as BackendApiResponse<BackendOrder[]>;
    if (!payload.success || !Array.isArray(payload.data) || payload.data.length === 0) {
      return mockOrders;
    }

    return payload.data.map(toOrderRecord);
  } catch {
    return mockOrders;
  }
}

function toOrderRecord(order: BackendOrder): OrderRecord {
  const placedDate = order.createdAt ? new Date(order.createdAt) : new Date();
  const subtotal = order.items.reduce((sum, item) => sum + Number(item.unitPrice) * item.quantity, 0);
  const shipping = subtotal > 0 ? 12 : 0;
  const discount = subtotal > 400 ? -20 : 0;
  const total = Number(order.totalAmount || subtotal + shipping + discount);

  return {
    id: order.id,
    code: order.orderCode.startsWith("#") ? order.orderCode : `#${order.orderCode}`,
    placedOn: placedDate.toLocaleDateString("en-GB", {
      day: "2-digit",
      month: "short",
      year: "numeric"
    }),
    status: normalizeOrderStatus(order.status),
    paymentState: order.status === "PENDING_PAYMENT" || order.status === "PENDING" ? "Pending" : "Paid",
    total,
    stage: normalizeStage(order.status),
    customerName: "Phan Hong Phuc",
    shippingAddress: splitAddress(order.shippingAddress),
    paymentInfo: {
      method: "Online payment",
      status: order.status === "PENDING_PAYMENT" || order.status === "PENDING" ? "Pending" : "Paid",
      transaction: "PAY-884521"
    },
    summary: {
      subtotal,
      shipping,
      discount,
      total
    },
    items: order.items.map((item, index) => ({
      id: item.id,
      name: item.productName,
      variant: index === 0 ? "Size 10.5 US · Crimson / White · Qty: 1" : "Size 9 US · Core Black · Qty: 1",
      quantity: item.quantity,
      price: Number(item.unitPrice),
      image: index === 0 ? "/assets/aero-runner-v2.png" : "/assets/canvas-minimalist.png"
    }))
  };
}

function normalizeOrderStatus(status: string): OrderRecord["status"] {
  switch (status) {
    case "COMPLETED":
      return "Completed";
    case "CANCELLED":
    case "FAILED":
      return "Cancelled";
    case "PAID":
      return "Paid";
    case "SHIPPING":
      return "Shipping";
    default:
      return "Pending";
  }
}

function normalizeStage(status: string): OrderRecord["stage"] {
  switch (status) {
    case "CONFIRMED":
      return "Confirmed";
    case "PAID":
      return "Paid";
    case "SHIPPING":
      return "Shipping";
    case "COMPLETED":
      return "Completed";
    default:
      return "Pending";
  }
}

function splitAddress(address: string): string[] {
  if (!address) {
    return ["Phan Hong Phuc", "Hoa Lac, Thach That, Ha Noi", "Phone: +84 912 345 678"];
  }
  return address.split(",").map((part) => part.trim()).filter(Boolean);
}
