
const STATUS_STYLES = {
  PENDING: "bg-yellow-100 text-yellow-800 ring-yellow-200",
  COMMITTING: "bg-blue-100 text-blue-800 ring-blue-200",
  CONFIRMED: "bg-green-100 text-green-800 ring-green-200",
  SHIPPED: "bg-indigo-100 text-indigo-800 ring-indigo-200",
  DELIVERED: "bg-emerald-100 text-emerald-800 ring-emerald-200",
  CANCELLED: "bg-red-100 text-red-800 ring-red-200",
  COMPENSATION_REQUIRED:
    "bg-orange-100 text-orange-800 ring-orange-200",
};

function OrderStatusBadge({ status }) {
  const normalizedStatus = status?.toUpperCase() ?? "UNKNOWN";

  const styles =
    STATUS_STYLES[normalizedStatus] ??
    "bg-slate-100 text-slate-700 ring-slate-200";

  const label = normalizedStatus.replaceAll("_", " ");

  return (
    <span
      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ring-1 ring-inset ${styles}`}
      role="status"
    >
      {label}
    </span>
  );
}

export default OrderStatusBadge;
