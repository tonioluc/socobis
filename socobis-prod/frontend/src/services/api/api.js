import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8010/socobis/api";

const apiClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  console.log(
    "API",
    config.method.toUpperCase(),
    "→",
    config.url,
    config.params || config.data || {}
  );
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error("API ERROR →", error.response?.status, error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export default apiClient;
