import { Circle, Heart, Search, Square } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { loadOrders } from "./api";
import { statusFilters } from "./data";
import type { OrderRecord, OrderStage, OrderStatus, PaymentState } from "./types";

const stages: OrderStage[] = ["Pending", "Confirmed", "Paid", "Shipping", "Completed"];

export default function App() {
  const [orders, setOrders] = useState<OrderRecord[]>([]);
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
  const [activeFilter, setActiveFilter] = useState<(typeof statusFilters)[number]>("All");

  useEffect(() => {
    void loadOrders().then((loadedOrders) => {
      setOrders(loadedOrders);
      setSelectedOrderId(null);
    });
  }, []);

  const selectedOrder = useMemo(
    () => orders.find((order) => order.id === selectedOrderId) ?? orders[0],
    [orders, selectedOrderId]
  );

  const filteredOrders = useMemo(() => {
    if (activeFilter === "All") {
      return orders;
    }
    return orders.filter((order) => order.status === activeFilter || order.paymentState === activeFilter);
  }, [activeFilter, orders]);

  function handleCancel(orderId: string) {
    setOrders((currentOrders) =>
      currentOrders.map((order) =>
        order.id === orderId
          ? { ...order, status: "Cancelled", paymentState: "Refunded", stage: "Pending" }
          : order
      )
    );
  }

  if (orders.length === 0 || !selectedOrder) {
    return <div className="loading-screen">Loading orders...</div>;
  }

  const isDetailView = selectedOrderId !== null;

  return (
    <div className="app-shell">
      <TopNavigation />
      <main className={isDetailView ? "page page-detail" : "page page-orders"}>
        {isDetailView ? (
          <OrderDetail
            order={selectedOrder}
            onBack={() => setSelectedOrderId(null)}
            onCancel={() => handleCancel(selectedOrder.id)}
          />
        ) : (
          <OrderList
            orders={filteredOrders}
            activeFilter={activeFilter}
            onFilterChange={setActiveFilter}
            onSelectOrder={setSelectedOrderId}
          />
        )}
      </main>
      <Footer />
    </div>
  );
}

function TopNavigation() {
  return (
    <header className="top-nav">
      <a className="brand" href="#" aria-label="StepZone home">
        StepZone
      </a>
      <nav className="nav-links" aria-label="Primary navigation">
        <a href="#">New</a>
        <a href="#">Sneakers</a>
        <a href="#">Men</a>
        <a href="#">Women</a>
        <a href="#">Sale</a>
        <a href="#">Collections</a>
      </nav>
      <div className="nav-actions">
        <label className="search-pill">
          <Search size={17} strokeWidth={2} aria-hidden="true" />
          <input type="search" placeholder="Search..." />
        </label>
        <button className="icon-button" type="button" aria-label="Wishlist">
          <Heart size={18} strokeWidth={2} />
        </button>
        <button className="icon-button" type="button" aria-label="Cart">
          <Square size={16} strokeWidth={2} />
        </button>
        <button className="icon-button" type="button" aria-label="Account">
          <Circle size={17} strokeWidth={2} />
        </button>
      </div>
    </header>
  );
}

interface OrderListProps {
  orders: OrderRecord[];
  activeFilter: (typeof statusFilters)[number];
  onFilterChange: (filter: (typeof statusFilters)[number]) => void;
  onSelectOrder: (orderId: string) => void;
}

function OrderList({ orders, activeFilter, onFilterChange, onSelectOrder }: OrderListProps) {
  return (
    <div className="orders-layout">
      <aside className="account-panel">
        <h2>My Account</h2>
        <nav aria-label="Account navigation">
          {["Profile", "Orders", "Addresses", "Notifications", "Change Password", "Logout"].map((item) => (
            <button className={item === "Orders" ? "account-link active" : "account-link"} key={item} type="button">
              {item}
            </button>
          ))}
        </nav>
      </aside>

      <section className="orders-content">
        <div className="page-title">
          <h1>My Orders</h1>
          <p>Track order status, payment state, and past purchases.</p>
        </div>

        <div className="filter-bar" role="tablist" aria-label="Order status filters">
          {statusFilters.map((filter) => (
            <button
              className={filter === activeFilter ? "filter-pill active" : "filter-pill"}
              key={filter}
              type="button"
              onClick={() => onFilterChange(filter)}
              role="tab"
              aria-selected={filter === activeFilter}
            >
              {filter}
            </button>
          ))}
        </div>

        <div className="order-list">
          {orders.map((order) => (
            <article className="order-row" key={order.id}>
              <div className="order-row-main">
                <h3>{order.code}</h3>
                <p>Placed on {order.placedOn}</p>
                <strong>{formatMoney(order.total)}</strong>
              </div>
              <div className="order-row-status">
                <StatusPill label={order.status} />
                <PaymentPill label={order.paymentState} />
              </div>
              <div className="order-row-actions">
                <button className="secondary-button" type="button" onClick={() => onSelectOrder(order.id)}>
                  View Details
                </button>
                <button className="primary-button" type="button" onClick={() => onSelectOrder(order.id)}>
                  Track Order
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

interface OrderDetailProps {
  order: OrderRecord;
  onBack: () => void;
  onCancel: () => void;
}

function OrderDetail({ order, onBack, onCancel }: OrderDetailProps) {
  return (
    <section className="detail-layout">
      <div className="breadcrumb">My Account / Orders / {order.code}</div>
      <div className="detail-heading">
        <div>
          <h1>Order Detail</h1>
          <p>
            Order {order.code} <span aria-hidden="true">&middot;</span> Placed on {order.placedOn}
          </p>
        </div>
        <StatusPill label={order.status} />
      </div>

      <ProgressTracker currentStage={order.stage} />

      <div className="info-grid">
        <InfoCard title="Shipping Address">
          {order.shippingAddress.map((line) => (
            <p key={line}>{line}</p>
          ))}
        </InfoCard>
        <InfoCard title="Payment Information">
          <p>Method: {order.paymentInfo.method}</p>
          <p>Status: {order.paymentInfo.status}</p>
          <p>Transaction: {order.paymentInfo.transaction}</p>
        </InfoCard>
        <InfoCard title="Order Summary" compact>
          <SummaryRow label="Subtotal" value={formatMoney(order.summary.subtotal)} />
          <SummaryRow label="Shipping" value={formatMoney(order.summary.shipping)} />
          <SummaryRow label="Discount" value={formatMoney(order.summary.discount)} />
          <SummaryRow label="Total" value={formatMoney(order.summary.total)} />
        </InfoCard>
      </div>

      <div className="lower-grid">
        <section className="items-panel">
          <h2>Items</h2>
          <div className="item-list">
            {order.items.map((item) => (
              <article className="item-row" key={item.id}>
                <img src={item.image} alt={item.name} />
                <div className="item-copy">
                  <h3>{item.name}</h3>
                  <p>{item.variant}</p>
                  <strong>{formatMoney(item.price)}</strong>
                </div>
                <div className="qty-stepper" aria-label={`Quantity for ${item.name}`}>
                  <button type="button" aria-label="Decrease quantity">
                    -
                  </button>
                  <span>{item.quantity}</span>
                  <button type="button" aria-label="Increase quantity">
                    +
                  </button>
                </div>
                <button className="remove-button" type="button">
                  Remove
                </button>
              </article>
            ))}
          </div>
        </section>

        <aside className="help-panel">
          <h2>Need help?</h2>
          <p>Contact support if delivery status is delayed or payment information does not match your receipt.</p>
          <button className="secondary-button wide" type="button" onClick={onBack}>
            Back to Orders
          </button>
          <button className="primary-button wide" type="button" onClick={onCancel}>
            Cancel Order
          </button>
        </aside>
      </div>
    </section>
  );
}

function ProgressTracker({ currentStage }: { currentStage: OrderStage }) {
  const activeIndex = stages.indexOf(currentStage);
  return (
    <section className="progress-card" aria-label="Order progress">
      {stages.map((stage, index) => {
        const isDone = index <= activeIndex;
        return (
          <div className="progress-step" key={stage}>
            <div className={isDone ? "progress-dot done" : "progress-dot"}>{isDone ? "✓" : ""}</div>
            {index < stages.length - 1 && (
              <div className={index < activeIndex ? "progress-line done" : "progress-line"} aria-hidden="true" />
            )}
            <span>{stage}</span>
          </div>
        );
      })}
    </section>
  );
}

function InfoCard({ title, children, compact = false }: { title: string; children: React.ReactNode; compact?: boolean }) {
  return (
    <article className={compact ? "info-card compact" : "info-card"}>
      <h2>{title}</h2>
      <div>{children}</div>
    </article>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <p className="summary-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </p>
  );
}

function StatusPill({ label }: { label: OrderStatus }) {
  return <span className={`status-pill ${label.toLowerCase()}`}>{label}</span>;
}

function PaymentPill({ label }: { label: PaymentState }) {
  return <span className={`payment-pill ${label.toLowerCase()}`}>{label}</span>;
}

function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-grid">
        <div>
          <h2>StepZone</h2>
          <p>Elevating streetwear through curated design and high-performance aesthetics.</p>
        </div>
        <div>
          <h3>Shop</h3>
          <a href="#">New Arrivals</a>
          <a href="#">Best Sellers</a>
        </div>
        <div>
          <h3>Support</h3>
          <a href="#">Shipping</a>
          <a href="#">Returns</a>
        </div>
        <div>
          <h3>Company</h3>
          <a href="#">About StepZone</a>
          <a href="#">Contact</a>
        </div>
      </div>
      <div className="copyright">&copy; 2026 STEPZONE ALL RIGHTS RESERVED.</div>
    </footer>
  );
}

function formatMoney(value: number) {
  const sign = value < 0 ? "-" : "";
  return `${sign}$${Math.abs(value).toFixed(2)}`;
}
