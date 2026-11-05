
import { useCallback, useEffect, useState } from "react";
import { X, Loader2, AlertCircle, Package, RefreshCw } from "lucide-react";

import { getOrderById } from "./orderApi";
import { getApiErrorMessage } from "../../utils/apiError";
import OrderStatusBadge from "./OrderStatusBadge";

const POLLING_INTERVAL = 5000;

const PROCESSING_STATUSES = new Set([
  "PENDING",
  "COMMITTING",
]);

function OrderDetails({ orderId, onClose }) {
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [lastUpdated, setLastUpdated] = useState(null);

  const loadOrder = useCallback(async (isBackground = false) => {
    if (isBackground) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    try {
      const response = await getOrderById(orderId);

      setOrder(response);
      setError("");
      setLastUpdated(new Date());
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [orderId]);

  useEffect(() => {
    let active = true;
    let requestInProgress = false;
    let timeoutId;

    const fetchOrder = async () => {
      if (!active || requestInProgress) return;

      requestInProgress = true;

      try {
        const response = await getOrderById(orderId);

        if (!active) return;

        setOrder(response);
        setError("");
        setLastUpdated(new Date());

        const isProcessing = PROCESSING_STATUSES.has(
          response.status
        );

        if (isProcessing) {
          timeoutId = setTimeout(fetchOrder, POLLING_INTERVAL);
        }
      } catch (err) {
        if (!active) return;

        setError(getApiErrorMessage(err));

        // Keep polling after a temporary failure while the last
        // known order status is still processing.
        timeoutId = setTimeout(fetchOrder, POLLING_INTERVAL);
      } finally {
        requestInProgress = false;
        if (active) {
          setLoading(false);
          setRefreshing(false);
        }
      }
    };

    setLoading(true);
    setError("");
    setOrder(null);

    fetchOrder();

    return () => {
      active = false;
      clearTimeout(timeoutId);
    };
  }, [orderId]);

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

  const isProcessing = PROCESSING_STATUSES.has(order?.status);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="order-details-title"
        className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl bg-white shadow-xl"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <h2
              id="order-details-title"
              className="text-lg font-bold text-slate-900"
            >
              Order Details
            </h2>

            <p className="mt-1 text-sm text-slate-500">
              Order #{orderId}
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close order details"
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
          >
            <X size={20} />
          </button>
        </div>

        <div className="p-5">
          {loading && !order ? (
            <div className="flex flex-col items-center justify-center py-16">
              <Loader2
                size={30}
                className="animate-spin text-blue-600"
              />
              <p className="mt-3 text-sm text-slate-500">
                Loading order details...
              </p>
            </div>
          ) : error && !order ? (
            <div
              role="alert"
              className="flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-4"
            >
              <AlertCircle
                size={20}
                className="mt-0.5 shrink-0 text-red-600"
              />

              <div className="flex-1">
                <p className="font-medium text-red-800">
                  Unable to load order
                </p>
                <p className="mt-1 text-sm text-red-700">
                  {error}
                </p>

                <button
                  type="button"
                  onClick={() => loadOrder(false)}
                  className="mt-3 text-sm font-semibold text-red-700 hover:underline"
                >
                  Try again
                </button>
              </div>
            </div>
          ) : order ? (
            <div className="space-y-6">
              {isProcessing && (
                <div className="flex items-center gap-2 rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800">
                  <RefreshCw
                    size={16}
                    className={refreshing ? "animate-spin" : ""}
                  />
                  Order processing. Status refreshes automatically.
                </div>
              )}

              {error && (
                <div
                  role="alert"
                  className="rounded-lg border border-yellow-200 bg-yellow-50 p-3 text-sm text-yellow-800"
                >
                  Could not refresh the latest status: {error}
                </div>
              )}

              <section className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs font-medium uppercase text-slate-500">
                    Customer ID
                  </p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">
                    {order.customerId}
                  </p>
                </div>

                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs font-medium uppercase text-slate-500">
                    Order Status
                  </p>
                  <div className="mt-2">
                    <OrderStatusBadge status={order.status} />
                  </div>
                </div>

                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs font-medium uppercase text-slate-500">
                    Total Amount
                  </p>
                  <p className="mt-2 text-lg font-semibold text-slate-900">
                    {formatPrice(order.totalAmount)}
                  </p>
                </div>

                <div className="rounded-lg bg-slate-50 p-4">
                  <p className="text-xs font-medium uppercase text-slate-500">
                    Created At
                  </p>
                  <p className="mt-2 text-sm font-semibold text-slate-900">
                    {formatDate(order.createdAt)}
                  </p>
                </div>
              </section>

              <section>
                <div className="mb-3 flex items-center gap-2">
                  <Package size={19} className="text-slate-500" />
                  <h3 className="font-semibold text-slate-900">
                    Order Items
                  </h3>
                </div>

                <div className="overflow-x-auto rounded-lg border border-slate-200">
                  <table className="w-full min-w-[550px] text-left text-sm">
                    <thead className="bg-slate-50">
                      <tr className="text-xs uppercase text-slate-500">
                        <th className="px-4 py-3">Product</th>
                        <th className="px-4 py-3">Unit Price</th>
                        <th className="px-4 py-3">Quantity</th>
                        <th className="px-4 py-3">Subtotal</th>
                      </tr>
                    </thead>

                    <tbody>
                      {order.items?.map((item, index) => (
                        <tr
                          key={`${item.productId}-${index}`}
                          className="border-t border-slate-100"
                        >
                          <td className="px-4 py-3">
                            <p className="font-medium text-slate-800">
                              {item.productName}
                            </p>
                            <p className="mt-1 text-xs text-slate-500">
                              Product #{item.productId}
                            </p>
                          </td>

                          <td className="px-4 py-3 text-slate-600">
                            {formatPrice(item.unitPrice)}
                          </td>

                          <td className="px-4 py-3 text-slate-600">
                            {item.quantity}
                          </td>

                          <td className="px-4 py-3 font-medium text-slate-800">
                            {formatPrice(item.subtotal)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>

              <section className="grid grid-cols-1 gap-4 border-t border-slate-200 pt-4 sm:grid-cols-2">
                <div>
                  <p className="text-xs font-medium uppercase text-slate-500">
                    Last Updated
                  </p>
                  <p className="mt-1 text-sm text-slate-700">
                    {formatDate(order.updatedAt)}
                  </p>

                  {lastUpdated && (
                    <p className="mt-1 text-xs text-slate-400">
                      Checked at {formatDate(lastUpdated)}
                    </p>
                  )}
                </div>

                <div className="text-left sm:text-right">
                  <p className="text-xs font-medium uppercase text-slate-500">
                    Order Total
                  </p>
                  <p className="mt-1 text-xl font-bold text-slate-900">
                    {formatPrice(order.totalAmount)}
                  </p>
                </div>
              </section>
            </div>
          ) : null}
        </div>

        <div className="flex justify-end border-t border-slate-200 px-5 py-4">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg bg-slate-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-slate-800"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

export default OrderDetails;
