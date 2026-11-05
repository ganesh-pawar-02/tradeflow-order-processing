
import { useEffect, useState } from "react";
import {
  Package,
  Boxes,
  ShoppingCart,
  RefreshCw,
} from "lucide-react";

import { getAllProducts } from "../products/productApi";
import { getOrders } from "../orders/orderApi";
import { getApiErrorMessage } from "../../utils/apiError";

const numberFormatter = new Intl.NumberFormat("en-IN");

const formatNumber = (value) =>
  numberFormatter.format(Number(value ?? 0));

const formatAmount = (value) =>
  numberFormatter.format(Number(value ?? 0));

const formatDate = (value) => {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return "—";

  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
};

const statusStyles = {
  PENDING: "bg-amber-50 text-amber-700",
  COMMITTING: "bg-blue-50 text-blue-700",
  CONFIRMED: "bg-green-50 text-green-700",
  SHIPPED: "bg-indigo-50 text-indigo-700",
  DELIVERED: "bg-emerald-50 text-emerald-700",
  CANCELLED: "bg-slate-100 text-slate-700",
  COMPENSATION_REQUIRED: "bg-red-50 text-red-700",
};

function MetricCard({ title, value, description, icon: Icon, loading }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-slate-500">{title}</p>

          {loading ? (
            <div className="mt-3 h-8 w-24 animate-pulse rounded bg-slate-100" />
          ) : (
            <p className="mt-2 text-2xl font-bold text-slate-900">
              {value}
            </p>
          )}

          <p className="mt-2 text-xs text-slate-500">{description}</p>
        </div>

        <div className="rounded-lg bg-blue-50 p-3 text-blue-700">
          <Icon size={21} />
        </div>
      </div>
    </section>
  );
}

function SectionMessage({ message, onRetry }) {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4">
      <p className="text-sm text-red-700">{message}</p>

      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 inline-flex items-center gap-2 text-sm font-medium text-red-700 hover:text-red-900"
        >
          <RefreshCw size={14} />
          Try again
        </button>
      )}
    </div>
  );
}

function Dashboard() {
  const [products, setProducts] = useState([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [productsError, setProductsError] = useState("");

  const [orders, setOrders] = useState([]);
  const [totalOrders, setTotalOrders] = useState(0);
  const [ordersLoading, setOrdersLoading] = useState(true);
  const [ordersError, setOrdersError] = useState("");

  const loadProducts = async () => {
    setProductsLoading(true);
    setProductsError("");

    try {
      const data = await getAllProducts();

      if (!Array.isArray(data)) {
        throw new Error("Unexpected product response from the server.");
      }

      setProducts(data);
    } catch (error) {
      setProductsError(
        error.message?.startsWith("Unexpected")
          ? error.message
          : getApiErrorMessage(error)
      );
    } finally {
      setProductsLoading(false);
    }
  };

  const loadOrders = async () => {
    setOrdersLoading(true);
    setOrdersError("");

    try {
      const data = await getOrders({ page: 0, size: 5 });

      if (!Array.isArray(data.content)) {
        throw new Error("Unexpected order response from the server.");
      }

      setOrders(data.content);
      setTotalOrders(Number(data.totalElements ?? 0));
    } catch (error) {
      setOrdersError(
        error.message?.startsWith("Unexpected")
          ? error.message
          : getApiErrorMessage(error)
      );
    } finally {
      setOrdersLoading(false);
    }
  };

  useEffect(() => {
    let active = true;

    const loadDashboard = async () => {
      await Promise.allSettled([
        loadProductsIfActive(),
        loadOrdersIfActive(),
      ]);
    };

    const loadProductsIfActive = async () => {
      if (!active) return;
      await loadProducts();
    };

    const loadOrdersIfActive = async () => {
      if (!active) return;
      await loadOrders();
    };

    loadDashboard();

    return () => {
      active = false;
    };
  }, []);

  const totalStock = products.reduce(
    (total, product) => total + Number(product.stockQuantity ?? 0),
    0
  );

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">
          Dashboard Overview
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          Overview of your products, inventory, and orders.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <MetricCard
          title="Total Products"
          value={formatNumber(products.length)}
          description="Products returned by Product Service"
          icon={Package}
          loading={productsLoading}
        />

        <MetricCard
          title="Total Stock"
          value={formatNumber(totalStock)}
          description="Units across the returned product list"
          icon={Boxes}
          loading={productsLoading}
        />

        <MetricCard
          title="Total Orders"
          value={formatNumber(totalOrders)}
          description="Total orders reported by Order Service"
          icon={ShoppingCart}
          loading={ordersLoading}
        />
      </div>

      <section className="rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-200 px-5 py-4">
          <h2 className="font-semibold text-slate-900">Recent Orders</h2>
          <p className="mt-1 text-xs text-slate-500">
            Latest five orders, sorted by creation time.
          </p>
        </div>

        <div className="p-5">
          {ordersLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((item) => (
                <div
                  key={item}
                  className="h-12 animate-pulse rounded-lg bg-slate-100"
                />
              ))}
            </div>
          ) : ordersError ? (
            <SectionMessage message={ordersError} onRetry={loadOrders} />
          ) : orders.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-500">
              No orders found.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[650px] text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs uppercase text-slate-500">
                    <th className="px-3 py-3 font-medium">Order ID</th>
                    <th className="px-3 py-3 font-medium">Customer</th>
                    <th className="px-3 py-3 font-medium">Amount</th>
                    <th className="px-3 py-3 font-medium">Status</th>
                    <th className="px-3 py-3 font-medium">Created</th>
                  </tr>
                </thead>

                <tbody>
                  {orders.map((order) => (
                    <tr
                      key={order.orderId}
                      className="border-b border-slate-100 last:border-0"
                    >
                      <td className="px-3 py-4 font-medium text-slate-900">
                        #{order.orderId}
                      </td>

                      <td className="px-3 py-4 text-slate-600">
                        {order.customerId}
                      </td>

                      <td className="px-3 py-4 text-slate-900">
                        {formatAmount(order.totalAmount)}
                      </td>

                      <td className="px-3 py-4">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
                            statusStyles[order.status] ??
                            "bg-slate-100 text-slate-700"
                          }`}
                        >
                          {order.status}
                        </span>
                      </td>

                      <td className="px-3 py-4 text-slate-500">
                        {formatDate(order.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>

      {productsError && (
        <SectionMessage message={productsError} onRetry={loadProducts} />
      )}
    </div>
  );
}

export default Dashboard;
