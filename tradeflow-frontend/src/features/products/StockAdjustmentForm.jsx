
import { useState } from "react";
import { adjustProductStock } from "./productApi";
import { getApiErrorMessage } from "../../utils/apiError";
import { X, Plus, Minus, Loader2 } from "lucide-react";

function StockAdjustmentForm({ product, onClose, onSuccess }) {
  const [type, setType] = useState("INCREASE");
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // One request ID belongs to this adjustment operation.
  // Keep it unchanged if the request needs to be retried.
  const [requestId] = useState(() => crypto.randomUUID());

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    const parsedQuantity = Number(quantity);

    if (
      !Number.isSafeInteger(parsedQuantity) ||
      parsedQuantity <= 0 ||
      parsedQuantity > 2147483647
    ) {
      setError("Enter a valid positive whole-number quantity.");
      return;
    }

    if (!reason.trim()) {
      setError("Please provide a reason for this adjustment.");
      return;
    }

    if (reason.trim().length > 500) {
      setError("Reason cannot exceed 500 characters.");
      return;
    }

    if (
      type === "DECREASE" &&
      parsedQuantity > product.stockQuantity
    ) {
      setError("Adjustment quantity exceeds the available stock.");
      return;
    }

    setLoading(true);

    try {
      const response = await adjustProductStock(product.id, {
        requestId,
        type,
        quantity: parsedQuantity,
        reason: reason.trim(),
      });

      onSuccess(response.data);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-xl bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">
              Adjust Stock
            </h2>
            <p className="text-sm text-gray-500">
              {product.name} · Current stock: {product.stockQuantity}
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 disabled:opacity-50"
            aria-label="Close modal"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 p-6">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700">
              Adjustment Type
            </label>

            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setType("INCREASE")}
                disabled={loading}
                className={`flex items-center justify-center gap-2 rounded-lg border px-4 py-3 text-sm font-medium ${
                  type === "INCREASE"
                    ? "border-green-600 bg-green-50 text-green-700"
                    : "border-gray-300 text-gray-600 hover:bg-gray-50"
                }`}
              >
                <Plus size={18} />
                Increase
              </button>

              <button
                type="button"
                onClick={() => setType("DECREASE")}
                disabled={loading}
                className={`flex items-center justify-center gap-2 rounded-lg border px-4 py-3 text-sm font-medium ${
                  type === "DECREASE"
                    ? "border-red-600 bg-red-50 text-red-700"
                    : "border-gray-300 text-gray-600 hover:bg-gray-50"
                }`}
              >
                <Minus size={18} />
                Decrease
              </button>
            </div>
          </div>

          <div>
            <label
              htmlFor="adjustment-quantity"
              className="mb-2 block text-sm font-medium text-gray-700"
            >
              Quantity
            </label>

            <input
              id="adjustment-quantity"
              type="number"
              min="1"
              max="2147483647"
              step="1"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              disabled={loading}
              required
              className="w-full rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              placeholder="Enter quantity"
            />
          </div>

          <div>
            <label
              htmlFor="adjustment-reason"
              className="mb-2 block text-sm font-medium text-gray-700"
            >
              Reason
            </label>

            <textarea
              id="adjustment-reason"
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              disabled={loading}
              required
              maxLength={500}
              rows={3}
              className="w-full resize-none rounded-lg border border-gray-300 px-3 py-2.5 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              placeholder="Explain why stock is being adjusted..."
            />

            <p className="mt-1 text-right text-xs text-gray-400">
              {reason.length}/500
            </p>
          </div>

          {error && (
            <div
              role="alert"
              className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3 border-t border-gray-100 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={loading}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading && <Loader2 size={16} className="animate-spin" />}
              {loading ? "Processing..." : "Confirm Adjustment"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default StockAdjustmentForm;
