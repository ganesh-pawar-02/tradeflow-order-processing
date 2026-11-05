
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Search,
  RefreshCw,
  Package,
  Plus,
  Pencil,
  PackagePlus,
} from "lucide-react";

import { getAllProducts } from "./productApi";
import { getApiErrorMessage } from "../../utils/apiError";

import ProductForm from "./ProductForm";
import EditProductForm from "./EditProductForm";
import StockAdjustmentForm from "./StockAdjustmentForm";

function Products() {
  const [products, setProducts] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [adjustingProduct, setAdjustingProduct] = useState(null);

  const [stockAdjustmentMessage, setStockAdjustmentMessage] = useState("");

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getAllProducts();

      if (!Array.isArray(data)) {
        throw new Error("Unexpected product response from the server.");
      }

      setProducts(data);
    } catch (err) {
      setError(
        err.message?.startsWith("Unexpected")
          ? err.message
          : getApiErrorMessage(err)
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  const filteredProducts = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();

    if (!query) return products;

    return products.filter((product) => {
      return (
        String(product.name ?? "").toLowerCase().includes(query) ||
        String(product.category ?? "").toLowerCase().includes(query) ||
        String(product.id ?? "").includes(query)
      );
    });
  }, [products, searchTerm]);

  const formatPrice = (price) => {
    if (price === null || price === undefined) return "—";

    const amount = Number(price);

    if (!Number.isFinite(amount)) return "—";

    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
    }).format(amount);
  };

  const handleStockAdjustmentSuccess = async (result) => {
    const action =
      result.type === "INCREASE" ? "increased" : "decreased";

    setStockAdjustmentMessage(
      `Stock ${action} successfully. Stock immediately after adjustment: ${result.resultingStock}`
    );

    setAdjustingProduct(null);

    // Refresh from the backend instead of calculating stock locally.
    await loadProducts();
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">
          Products
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          View and manage your product catalogue.
        </p>
      </div>

      <section className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-md">
            <Search
              size={18}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            />

            <input
              type="search"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              placeholder="Search by name, category, or ID..."
              className="w-full rounded-lg border border-slate-300 py-2.5 pl-10 pr-3 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              aria-label="Search products"
            />
          </div>

          <div className="flex flex-col gap-2 sm:flex-row">
            <button
              type="button"
              onClick={loadProducts}
              disabled={loading}
              className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RefreshCw
                size={16}
                className={loading ? "animate-spin" : ""}
              />
              Refresh
            </button>

            <button
              type="button"
              onClick={() => setIsAddModalOpen(true)}
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700"
            >
              <Plus size={16} />
              Add Product
            </button>
          </div>
        </div>
      </section>

      {stockAdjustmentMessage && (
        <div
          role="status"
          className="flex items-center justify-between rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800"
        >
          <span>{stockAdjustmentMessage}</span>

          <button
            type="button"
            onClick={() => setStockAdjustmentMessage("")}
            className="ml-4 shrink-0 font-medium hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {error && (
        <div
          role="alert"
          className="flex flex-col gap-3 rounded-lg border border-red-200 bg-red-50 p-4 sm:flex-row sm:items-center sm:justify-between"
        >
          <p className="text-sm text-red-700">{error}</p>

          <button
            type="button"
            onClick={loadProducts}
            disabled={loading}
            className="text-sm font-semibold text-red-700 hover:text-red-900 disabled:opacity-50"
          >
            Try again
          </button>
        </div>
      )}

      <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <h2 className="font-semibold text-slate-900">
              Product Catalogue
            </h2>
            <p className="mt-1 text-xs text-slate-500">
              {loading
                ? "Loading products..."
                : `${filteredProducts.length} of ${products.length} products`}
            </p>
          </div>

          <Package size={21} className="text-slate-400" />
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
        ) : !error && filteredProducts.length === 0 ? (
          <div className="px-5 py-14 text-center">
            <Package size={32} className="mx-auto text-slate-300" />

            <p className="mt-3 font-medium text-slate-700">
              {products.length === 0
                ? "No products found"
                : "No matching products"}
            </p>

            <p className="mt-1 text-sm text-slate-500">
              {products.length === 0
                ? "Products will appear here when they are available."
                : "Try a different search term."}
            </p>
          </div>
        ) : !error ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[850px] text-left text-sm">
              <thead className="bg-slate-50">
                <tr className="text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-5 py-3 font-semibold">Product ID</th>
                  <th className="px-5 py-3 font-semibold">Name</th>
                  <th className="px-5 py-3 font-semibold">Category</th>
                  <th className="px-5 py-3 font-semibold">Price</th>
                  <th className="px-5 py-3 font-semibold">Stock</th>
                  <th className="px-5 py-3 font-semibold">Actions</th>
                </tr>
              </thead>

              <tbody>
                {filteredProducts.map((product) => (
                  <tr
                    key={product.id}
                    className="border-t border-slate-100 hover:bg-slate-50"
                  >
                    <td className="px-5 py-4 font-medium text-slate-700">
                      #{product.id}
                    </td>

                    <td className="px-5 py-4 font-medium text-slate-900">
                      {product.name}
                    </td>

                    <td className="px-5 py-4 text-slate-600">
                      {product.category}
                    </td>

                    <td className="px-5 py-4 text-slate-700">
                      {formatPrice(product.price)}
                    </td>

                    <td className="px-5 py-4 font-medium text-slate-700">
                      {product.stockQuantity ?? "—"}
                    </td>

                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => setEditingProduct(product)}
                          aria-label={`Edit ${product.name}`}
                          className="inline-flex items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                        >
                          <Pencil size={14} />
                          Edit
                        </button>

                        <button
                          type="button"
                          onClick={() => setAdjustingProduct(product)}
                          aria-label={`Adjust stock for ${product.name}`}
                          className="inline-flex items-center gap-2 rounded-lg border border-blue-200 px-3 py-2 text-xs font-medium text-blue-700 hover:bg-blue-50"
                        >
                          <PackagePlus size={14} />
                          Adjust Stock
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>

      {isAddModalOpen && (
        <ProductForm
          onClose={() => setIsAddModalOpen(false)}
          onSuccess={async () => {
            setIsAddModalOpen(false);
            await loadProducts();
          }}
        />
      )}

      {editingProduct && (
        <EditProductForm
          product={editingProduct}
          onClose={() => setEditingProduct(null)}
          onSuccess={async () => {
            setEditingProduct(null);
            await loadProducts();
          }}
        />
      )}

      {adjustingProduct && (
        <StockAdjustmentForm
          product={adjustingProduct}
          onClose={() => setAdjustingProduct(null)}
          onSuccess={handleStockAdjustmentSuccess}
        />
      )}
    </div>
  );
}

export default Products;
