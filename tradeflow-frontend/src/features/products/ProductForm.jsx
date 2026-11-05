
import { useState } from "react";
import { X, Plus } from "lucide-react";
import { createProduct } from "./productApi";
import { getApiErrorMessage } from "../../utils/apiError";

const initialForm = {
  name: "",
  price: "",
  stockQuantity: "",
  category: "",
};

function ProductForm({ onClose, onSuccess }) {
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    const price = Number(form.price);
    const stockQuantity = Number(form.stockQuantity);

    if (!form.name.trim() || !form.category.trim()) {
      setError("Product name and category are required.");
      return;
    }

    if (!Number.isFinite(price) || price < 0) {
      setError("Enter a valid non-negative price.");
      return;
    }

    if (
      !Number.isInteger(stockQuantity) ||
      stockQuantity < 0
    ) {
      setError("Stock quantity must be a non-negative whole number.");
      return;
    }

    const productData = {
      name: form.name.trim(),
      price,
      stockQuantity,
      category: form.category.trim(),
    };

    setLoading(true);

    try {
      await createProduct(productData);
      onSuccess();
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const inputClass =
    "mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:bg-slate-100";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-product-title"
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl bg-white shadow-xl"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <h2
              id="add-product-title"
              className="text-lg font-semibold text-slate-900"
            >
              Add Product
            </h2>
            <p className="mt-1 text-xs text-slate-500">
              Enter the product information.
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            aria-label="Close form"
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-50"
          >
            <X size={19} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 p-5">
          <div>
            <label htmlFor="name" className="text-sm font-medium text-slate-700">
              Product Name
            </label>
            <input
              id="name"
              name="name"
              value={form.name}
              onChange={handleChange}
              maxLength={150}
              required
              disabled={loading}
              className={inputClass}
              placeholder="Enter product name"
            />
          </div>

          <div>
            <label
              htmlFor="category"
              className="text-sm font-medium text-slate-700"
            >
              Category
            </label>
            <input
              id="category"
              name="category"
              value={form.category}
              onChange={handleChange}
              maxLength={100}
              required
              disabled={loading}
              className={inputClass}
              placeholder="Enter category"
            />
          </div>

          <div>
            <label htmlFor="price" className="text-sm font-medium text-slate-700">
              Price
            </label>
            <input
              id="price"
              name="price"
              type="number"
              min="0"
              step="0.01"
              value={form.price}
              onChange={handleChange}
              required
              disabled={loading}
              className={inputClass}
              placeholder="0.00"
            />
          </div>

          <div>
            <label
              htmlFor="stockQuantity"
              className="text-sm font-medium text-slate-700"
            >
              Initial Stock Quantity
            </label>
            <input
              id="stockQuantity"
              name="stockQuantity"
              type="number"
              min="0"
              step="1"
              value={form.stockQuantity}
              onChange={handleChange}
              required
              disabled={loading}
              className={inputClass}
              placeholder="0"
            />
          </div>

          {error && (
            <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
              {error}
            </p>
          )}

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Plus size={16} />
              {loading ? "Creating..." : "Create Product"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ProductForm;
