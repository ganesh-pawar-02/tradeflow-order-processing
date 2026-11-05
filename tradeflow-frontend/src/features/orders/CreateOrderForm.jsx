
import { useEffect, useState } from "react";
import {
  X,
  Plus,
  Trash2,
  Loader2,
  ShoppingCart,
} from "lucide-react";

import { getAllProducts } from "../products/productApi";
import { createOrder } from "./orderApi";
import { getApiErrorMessage } from "../../utils/apiError";

function CreateOrderForm({ onClose, onSuccess }) {
  const [customerId, setCustomerId] = useState("");
  const [products, setProducts] = useState([]);
  const [items, setItems] = useState([
    { productId: "", quantity: "1" },
  ]);

  const [loadingProducts, setLoadingProducts] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    const loadProducts = async () => {
      try {
        const data = await getAllProducts();

        if (active) {
          setProducts(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (active) {
          setError(getApiErrorMessage(err));
        }
      } finally {
        if (active) {
          setLoadingProducts(false);
        }
      }
    };

    loadProducts();

    return () => {
      active = false;
    };
  }, []);

  const handleItemChange = (index, field, value) => {
    setItems((current) =>
      current.map((item, itemIndex) =>
        itemIndex === index
          ? { ...item, [field]: value }
          : item
      )
    );
  };

  const addItem = () => {
    setItems((current) => [
      ...current,
      { productId: "", quantity: "1" },
    ]);
  };

  const removeItem = (index) => {
    setItems((current) =>
      current.filter((_, itemIndex) => itemIndex !== index)
    );
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    const parsedCustomerId = Number(customerId);

    if (
      !Number.isSafeInteger(parsedCustomerId) ||
      parsedCustomerId <= 0
    ) {
      setError("Enter a valid positive customer ID.");
      return;
    }

    if (items.length === 0) {
      setError("Add at least one product.");
      return;
    }

    const validatedItems = [];

    for (const item of items) {
      const productId = Number(item.productId);
      const quantity = Number(item.quantity);

      if (!Number.isSafeInteger(productId) || productId <= 0) {
        setError("Select a valid product for every order item.");
        return;
      }

      if (
        !Number.isSafeInteger(quantity) ||
        quantity < 1 ||
        quantity > 2147483647
      ) {
        setError("Each quantity must be a positive whole number.");
        return;
      }

      validatedItems.push({ productId, quantity });
    }

    const productIds = validatedItems.map((item) => item.productId);

    if (new Set(productIds).size !== productIds.length) {
      setError("A product can only appear once. Update its quantity instead.");
      return;
    }

    setSubmitting(true);

    try {
      const response = await createOrder({
        customerId: parsedCustomerId,
        items: validatedItems,
      });

      onSuccess(response);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl bg-white shadow-xl">
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 bg-white px-6 py-4">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">
              Create Order
            </h2>
            <p className="text-sm text-slate-500">
              Add customer and product information.
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-50"
            aria-label="Close create order form"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6 p-6">
          <div>
            <label
              htmlFor="customer-id"
              className="mb-2 block text-sm font-medium text-slate-700"
            >
              Customer ID
            </label>

            <input
              id="customer-id"
              type="number"
              min="1"
              step="1"
              required
              value={customerId}
              onChange={(event) => setCustomerId(event.target.value)}
              disabled={submitting}
              placeholder="Enter customer ID"
              className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
            />
          </div>

          <div>
            <div className="mb-3 flex items-center justify-between">
              <h3 className="font-semibold text-slate-900">
                Order Items
              </h3>

              <button
                type="button"
                onClick={addItem}
                disabled={submitting || loadingProducts}
                className="inline-flex items-center gap-1 rounded-lg border border-blue-200 px-3 py-2 text-sm font-medium text-blue-700 hover:bg-blue-50 disabled:opacity-50"
              >
                <Plus size={16} />
                Add Product
              </button>
            </div>

            {loadingProducts ? (
              <div className="flex items-center justify-center gap-2 py-8 text-sm text-slate-500">
                <Loader2 size={18} className="animate-spin" />
                Loading products...
              </div>
            ) : products.length === 0 ? (
              <p className="rounded-lg bg-slate-50 p-4 text-sm text-slate-500">
                No products are available.
              </p>
            ) : (
              <div className="space-y-3">
                {items.map((item, index) => (
                  <div
                    key={index}
                    className="grid grid-cols-1 gap-3 rounded-lg border border-slate-200 p-3 sm:grid-cols-[1fr_120px_auto] sm:items-end"
                  >
                    <div>
                      <label
                        htmlFor={`product-${index}`}
                        className="mb-1 block text-xs font-medium text-slate-600"
                      >
                        Product
                      </label>

                      <select
                        id={`product-${index}`}
                        required
                        value={item.productId}
                        onChange={(event) =>
                          handleItemChange(
                            index,
                            "productId",
                            event.target.value
                          )
                        }
                        disabled={submitting}
                        className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-blue-500"
                      >
                        <option value="">Select product</option>

                        {products.map((product) => (
                          <option
                            key={product.id}
                            value={product.id}
                          >
                            {product.name} — Stock: {product.stockQuantity}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label
                        htmlFor={`quantity-${index}`}
                        className="mb-1 block text-xs font-medium text-slate-600"
                      >
                        Quantity
                      </label>

                      <input
                        id={`quantity-${index}`}
                        type="number"
                        min="1"
                        step="1"
                        required
                        value={item.quantity}
                        onChange={(event) =>
                          handleItemChange(
                            index,
                            "quantity",
                            event.target.value
                          )
                        }
                        disabled={submitting}
                        className="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-blue-500"
                      />
                    </div>

                    <button
                      type="button"
                      onClick={() => removeItem(index)}
                      disabled={submitting || items.length === 1}
                      className="flex items-center justify-center rounded-lg p-2.5 text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-30"
                      aria-label={`Remove item ${index + 1}`}
                    >
                      <Trash2 size={18} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {error && (
            <div
              role="alert"
              className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={submitting || loadingProducts || products.length === 0}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {submitting ? (
                <Loader2 size={16} className="animate-spin" />
              ) : (
                <ShoppingCart size={16} />
              )}

              {submitting ? "Creating Order..." : "Create Order"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreateOrderForm;
