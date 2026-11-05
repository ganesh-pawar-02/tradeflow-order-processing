
import { useCallback, useEffect, useState } from "react";
import {
  RefreshCw,
  ShoppingCart,
  Search,
  ChevronLeft,
  ChevronRight,
  Eye,
  Plus,
} from "lucide-react";

import { getOrders } from "./orderApi";
import { getApiErrorMessage } from "../../utils/apiError";
import OrderDetails from "./OrderDetails";
import CreateOrderForm from "./CreateOrderForm";
import OrderStatusBadge from "./OrderStatusBadge";

function Orders() {
  const [orders, setOrders] = useState([]);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);

  const [customerIdInput, setCustomerIdInput] = useState("");
  const [customerId, setCustomerId] = useState("");

  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [selectedOrderId, setSelectedOrderId] = useState(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");

  const loadOrders = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await getOrders({
        page,
        size: pageSize,
        customerId,
      });

      if (!response || !Array.isArray(response.content)) {
        throw new Error("Unexpected order response from the server.");
      }

      setOrders(response.content);
      setTotalElements(response.totalElements ?? 0);
      setTotalPages(response.totalPages ?? 0);
    } catch (err) {
      setOrders([]);
      setTotalElements(0);
      setTotalPages(0);

      setError(
        err.message?.startsWith("Unexpected")
          ? err.message
          : getApiErrorMessage(err)
      );
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, customerId]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const handleSearch = (event) => {
    event.preventDefault();
    setPage(0);
    setCustomerId(customerIdInput.trim());
  };

  const handleClearFilter = () => {
    setCustomerIdInput("");
    setCustomerId("");
    setPage(0);
  };

  const handleCreateOrderSuccess = async (response) => {
    setIsCreateModalOpen(false);

    const orderId = response.orderId;
    const status = response.status ?? "UNKNOWN";

    setSuccessMessage(
      `Order #${orderId} created. Current status: ${status.replaceAll("_", " ")}.`
    );

    if (customerId) {
      setCustomerId("");
      setCustomerIdInput("");
      setPage(0);
    } else if (page !== 0) {
      setPage(0);
    } else {
      await loadOrders();
    }
  };

  const formatDate = (dateValue) => {
    if (!dateValue) return "—";

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) return "—";

    return new Intl.DateTimeFormat("en-IN", {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(date);
  };

  const formatPrice = (price) => {
    if (price === null || price === undefined) return "—";

    const amount = Number(price);

    if (!Number.isFinite(amount)) return "—";

    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
    }).format(amount);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Orders
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            View and manage customer orders.
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setSuccessMessage("");
            setIsCreateModalOpen(true);
          }}
          className="inline-flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Plus size={17} />
          Create Order
        </button>
      </div>

      {successMessage && (
        <div
          role="status"
          className="flex items-center justify-between gap-3 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800"
        >
          <span>{successMessage}</span>

          <button
            type="button"
            onClick={() => setSuccessMessage("")}
            className="shrink-0 font-medium hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      <section className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <form
            onSubmit={handleSearch}
            className="flex w-full flex-col gap-2 sm:max-w-md sm:flex-row"
          >
            <div className="relative flex-1">
              <Search
                size={18}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
              />

              <input
                type="number"
                min="1"
                value={customerIdInput}
                onChange={(event) =>
                  setCustomerIdInput(event.target.value)
                }
                placeholder="Filter by customer ID..."
                className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                aria-label="Filter by customer ID"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              Search
            </button>

            {customerId && (
              <button
                type="button"
                onClick={handleClearFilter}
                className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Clear
              </button>
            )}
          </form>

          <button
            type="button"
            onClick={loadOrders}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            <RefreshCw
              size={16}
              className={loading ? "animate-spin" : ""}
            />
            Refresh
          </button>
        </div>
      </section>

      {error && (
        <div
          role="alert"
          className="flex flex-col gap-3 rounded-lg border border-red-200 bg-red-50 p-4 sm:flex-row sm:items-center sm:justify-between"
        >
          <p className="text-sm text-red-700">{error}</p>

          <button
            type="button"
            onClick={loadOrders}
            disabled={loading}
            className="text-sm font-semibold text-red-700 hover:text-red-900"
          >
            Try again
          </button>
        </div>
      )}

      <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <h2 className="font-semibold text-slate-900">
              Order History
            </h2>

            <p className="mt-1 text-xs text-slate-500">
              {loading
                ? "Loading orders..."
                : `${totalElements} total orders`}
            </p>
          </div>

          <ShoppingCart size={21} className="text-slate-400" />
        </div>

        {loading ? (
          <div className="space-y-3 p-5">
            {[1, 2, 3, 4].map((item) => (
              <div
                key={item}
                className="h-12 animate-pulse rounded-lg bg-slate-100"
              />
            ))}
          </div>
        ) : !error && orders.length === 0 ? (
          <div className="px-5 py-14 text-center">
            <ShoppingCart
              size={32}
              className="mx-auto text-slate-300"
            />

            <p className="mt-3 font-medium text-slate-700">
              No orders found
            </p>

            <p className="mt-1 text-sm text-slate-500">
              Orders will appear here when they are available.
            </p>
          </div>
        ) : !error ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[850px] text-left text-sm">
              <thead className="bg-slate-50">
                <tr className="text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-5 py-3 font-semibold">Order ID</th>
                  <th className="px-5 py-3 font-semibold">Customer ID</th>
                  <th className="px-5 py-3 font-semibold">Total Amount</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold">Created At</th>
                  <th className="px-5 py-3 font-semibold">Items</th>
                  <th className="px-5 py-3 font-semibold">Actions</th>
                </tr>
              </thead>

              <tbody>
                {orders.map((order) => (
                  <tr
                    key={order.orderId}
                    className="border-t border-slate-100 hover:bg-slate-50"
                  >
                    <td className="px-5 py-4 font-medium text-slate-900">
                      #{order.orderId}
                    </td>

                    <td className="px-5 py-4 text-slate-600">
                      {order.customerId}
                    </td>

                    <td className="px-5 py-4 font-medium text-slate-700">
                      {formatPrice(order.totalAmount)}
                    </td>

                    <td className="px-5 py-4">
                      <OrderStatusBadge status={order.status} />
                    </td>

                    <td className="px-5 py-4 text-slate-600">
                      {formatDate(order.createdAt)}
                    </td>

                    <td className="px-5 py-4 text-slate-600">
                      {order.items?.length ?? 0}
                    </td>

                    <td className="px-5 py-4">
                      <button
                        type="button"
                        onClick={() =>
                          setSelectedOrderId(order.orderId)
                        }
                        className="inline-flex items-center gap-2 rounded-lg border border-blue-200 px-3 py-2 text-xs font-medium text-blue-700 hover:bg-blue-50"
                      >
                        <Eye size={14} />
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

        {!loading && !error && totalPages > 1 && (
          <div className="flex flex-col gap-3 border-t border-slate-200 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-slate-500">
              Page {page + 1} of {totalPages}
            </p>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setPage((current) => current - 1)}
                disabled={page === 0}
                className="inline-flex items-center gap-1 rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                <ChevronLeft size={16} />
                Previous
              </button>

              <button
                type="button"
                onClick={() => setPage((current) => current + 1)}
                disabled={page >= totalPages - 1}
                className="inline-flex items-center gap-1 rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                Next
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </section>

      {isCreateModalOpen && (
        <CreateOrderForm
          onClose={() => setIsCreateModalOpen(false)}
          onSuccess={handleCreateOrderSuccess}
        />
      )}

      {selectedOrderId !== null && (
        <OrderDetails
          orderId={selectedOrderId}
          onClose={() => setSelectedOrderId(null)}
        />
      )}
    </div>
  );
}

export default Orders;
