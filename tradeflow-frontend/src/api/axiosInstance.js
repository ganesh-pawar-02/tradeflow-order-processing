import axios from "axios";

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Backend returned an HTTP error response.
      console.error("API Error:", error.response.data);
    } else if (error.request) {
      // Request was sent but no response was received.
      console.error("Network Error:", error.message);
    } else {
      console.error("Request Setup Error:", error.message);
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;