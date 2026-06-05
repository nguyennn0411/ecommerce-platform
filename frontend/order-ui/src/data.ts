import type { OrderRecord } from "./types";

export const mockOrders: OrderRecord[] = [
  {
    id: "order-sz-10245",
    code: "#SZ-10245",
    placedOn: "28 May 2026",
    status: "Shipping",
    paymentState: "Paid",
    total: 622,
    stage: "Shipping",
    customerName: "Phan Hong Phuc",
    shippingAddress: ["Phan Hong Phuc", "Hoa Lac, Thach That, Ha Noi", "Phone: +84 912 345 678"],
    paymentInfo: {
      method: "Online payment",
      status: "Paid",
      transaction: "PAY-884521"
    },
    summary: {
      subtotal: 630,
      shipping: 12,
      discount: -20,
      total: 622
    },
    items: [
      {
        id: "item-aero-runner-v2",
        name: "Aero Runner V2",
        variant: "Size 10.5 US · Crimson / White · Qty: 1",
        quantity: 1,
        price: 240,
        image: "/assets/aero-runner-v2.png"
      },
      {
        id: "item-canvas-minimalist",
        name: "Canvas Minimalist",
        variant: "Size 9 US · Core Black · Qty: 1",
        quantity: 1,
        price: 180,
        image: "/assets/canvas-minimalist.png"
      }
    ]
  },
  {
    id: "order-sz-10231",
    code: "#SZ-10231",
    placedOn: "28 May 2026",
    status: "Completed",
    paymentState: "Paid",
    total: 240,
    stage: "Completed",
    customerName: "Phan Hong Phuc",
    shippingAddress: ["Phan Hong Phuc", "Hoa Lac, Thach That, Ha Noi", "Phone: +84 912 345 678"],
    paymentInfo: {
      method: "Online payment",
      status: "Paid",
      transaction: "PAY-882104"
    },
    summary: {
      subtotal: 240,
      shipping: 0,
      discount: 0,
      total: 240
    },
    items: [
      {
        id: "item-aero-10231",
        name: "Aero Runner V2",
        variant: "Size 10.5 US · White · Qty: 1",
        quantity: 1,
        price: 240,
        image: "/assets/aero-runner-v2.png"
      }
    ]
  },
  {
    id: "order-sz-10218",
    code: "#SZ-10218",
    placedOn: "28 May 2026",
    status: "Pending",
    paymentState: "Pending",
    total: 180,
    stage: "Pending",
    customerName: "Phan Hong Phuc",
    shippingAddress: ["Phan Hong Phuc", "Hoa Lac, Thach That, Ha Noi", "Phone: +84 912 345 678"],
    paymentInfo: {
      method: "Online payment",
      status: "Pending",
      transaction: "WAITING"
    },
    summary: {
      subtotal: 180,
      shipping: 0,
      discount: 0,
      total: 180
    },
    items: [
      {
        id: "item-canvas-10218",
        name: "Canvas Minimalist",
        variant: "Size 9 US · Core Black · Qty: 1",
        quantity: 1,
        price: 180,
        image: "/assets/canvas-minimalist.png"
      }
    ]
  },
  {
    id: "order-sz-10194",
    code: "#SZ-10194",
    placedOn: "28 May 2026",
    status: "Cancelled",
    paymentState: "Refunded",
    total: 210,
    stage: "Pending",
    customerName: "Phan Hong Phuc",
    shippingAddress: ["Phan Hong Phuc", "Hoa Lac, Thach That, Ha Noi", "Phone: +84 912 345 678"],
    paymentInfo: {
      method: "Online payment",
      status: "Refunded",
      transaction: "RF-10194"
    },
    summary: {
      subtotal: 210,
      shipping: 0,
      discount: 0,
      total: 210
    },
    items: [
      {
        id: "item-cancelled-10194",
        name: "Canvas Minimalist",
        variant: "Size 8 US · Black · Qty: 1",
        quantity: 1,
        price: 210,
        image: "/assets/canvas-minimalist.png"
      }
    ]
  }
];

export const statusFilters = ["All", "Pending", "Paid", "Shipping", "Completed", "Cancelled"] as const;
