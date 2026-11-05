import { Menu, X } from "lucide-react";
import { useLocation } from "react-router-dom";

const pageTitles = {
  "/dashboard": "Dashboard",
  "/products": "Products",
  "/orders": "Orders",
  "/inventory": "Inventory",
};

function Header({ onMenuToggle, isMobileMenuOpen }) {
  const location = useLocation();

  const pageTitle = pageTitles[location.pathname] || "Dashboard";

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 sm:px-6">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onMenuToggle}
          className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
          aria-label={isMobileMenuOpen ? "Close navigation" : "Open navigation"}
          aria-expanded={isMobileMenuOpen}
        >
          {isMobileMenuOpen ? <X size={21} /> : <Menu size={21} />}
        </button>

        <div>
          <h2 className="text-lg font-semibold text-slate-900">
            {pageTitle}
          </h2>
          <p className="hidden text-xs text-slate-500 sm:block">
            TradeFlow Order Processing
          </p>
        </div>
      </div>

      <div className="text-sm text-slate-500">
        Admin Workspace
      </div>
    </header>
  );
}

export default Header;