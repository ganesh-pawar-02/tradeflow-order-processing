import axiosInstance from "../../api/axiosInstance";

// Fetch all products
export const getAllProducts = async () => {
  const response = await axiosInstance.get("/products");
  return response.data;
};

// Fetch a single product
export const getProductById = async (productId) => {
  const response = await axiosInstance.get(`/products/${productId}`);
  return response.data;
};

// Create a product
export const createProduct = async (productData) => {
  const response = await axiosInstance.post("/products", productData);
  return response.data;
};

// Update product details (does not update stock)
export const updateProduct = async (productId, productData) => {
  const response = await axiosInstance.put(
    `/products/${productId}`,
    productData
  );
  return response.data;
};

// Adjust product stock
export const adjustProductStock = async (productId, adjustmentData) => {
  const response = await axiosInstance.post(
    `/products/${productId}/stock-adjustments`,
    adjustmentData
  );
  return response.data;
};