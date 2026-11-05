import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import Dashboard from "./features/dashboard/Dashboard";
import Products from "./features/products/Products";
import Orders from "./features/orders/Orders";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route
            path="/"
            element={<Navigate to="/dashboard" replace />}
          />

          <Route path="/dashboard" element={<Dashboard />} />

          <Route path="/products" element={<Products />} />

          <Route path="/orders" element={<Orders />} />

          <Route
            path="/inventory"
            element={<h1 className="text-2xl font-bold">Inventory</h1>}
          />
        </Route>

        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;