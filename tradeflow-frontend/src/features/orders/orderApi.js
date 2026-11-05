import axiosInstance from "../../api/axiosInstance";

// Create an order
export const createOrder = async (orderData) => {
  const response = await axiosInstance.post("/orders", orderData);
  return response.data;
};

// Fetch an order by ID
export const getOrderById = async (orderId) => {
  const response = await axiosInstance.get(`/orders/${orderId}`);
  return response.data;
};

// Fetch paginated orders, optionally filtered by customer ID
export const getOrders = async ({
  page = 0,
  size = 20,
  customerId,
} = {}) => {
  const params = {
    page,
    size,
    sort: "createdAt,desc",
  };

  if (customerId !== undefined && customerId !== null && customerId !== "") {
    params.customerId = customerId;
  }

  const response = await axiosInstance.get("/orders", { params });
  return response.data;
};