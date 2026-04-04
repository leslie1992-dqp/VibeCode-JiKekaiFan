import { defineStore } from "pinia";
import { loginApi, meApi } from "../api/auth";
import type { LoginRequest, UserInfo } from "../types/auth";

interface AuthState {
  token: string;
  userInfo: UserInfo | null;
}

function readStoredToken(): string {
  const raw = localStorage.getItem("token");
  if (raw == null) return "";
  const t = raw.trim();
  return t.length > 0 ? t : "";
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: readStoredToken(),
    userInfo: null
  }),
  actions: {
    async login(payload: LoginRequest) {
      const data = await loginApi(payload);
      const t = data.token?.trim() || "";
      this.token = t;
      this.userInfo = data.userInfo;
      if (t) {
        localStorage.setItem("token", t);
      } else {
        localStorage.removeItem("token");
      }
    },
    async loadMe() {
      if (!this.token) return;
      this.userInfo = await meApi();
    },
    setUserInfo(info: UserInfo) {
      this.userInfo = info;
    },
    logout() {
      this.token = "";
      this.userInfo = null;
      localStorage.removeItem("token");
    }
  }
});
