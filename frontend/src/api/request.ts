import axios from "axios";
import { useAuthStore } from "../store/auth";

const request = axios.create({
  baseURL: "http://localhost:8081/api/v1",
  timeout: 10000
});

request.interceptors.request.use((config) => {
  const store = useAuthStore();
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`;
  }
  return config;
});

/** 后端常以 HTTP 200 + body.code≠0 表示业务失败，需转为 reject 才能被 catch */
request.interceptors.response.use(
  (res) => {
    const d = res.data as { code?: number; message?: string } | undefined;
    if (d != null && typeof d.code === "number" && d.code !== 0) {
      return Promise.reject({
        response: { data: d },
        message: d.message || "请求失败"
      });
    }
    return res;
  },
  (err) => Promise.reject(err)
);

export default request;
