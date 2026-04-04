import request from "./request";
import type {
  ApiResult,
  LoginData,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UserInfo
} from "../types/auth";

export async function registerApi(payload: RegisterRequest): Promise<number> {
  const res = await request.post<ApiResult<{ userId: number }>>("/auth/register", payload);
  return res.data.data.userId;
}

export async function loginApi(payload: LoginRequest): Promise<LoginData> {
  const res = await request.post<ApiResult<LoginData>>("/auth/login", payload);
  return res.data.data;
}

export async function meApi(): Promise<UserInfo> {
  const res = await request.get<ApiResult<UserInfo>>("/users/me");
  return res.data.data;
}

export async function resetPasswordApi(payload: ResetPasswordRequest): Promise<void> {
  await request.post<ApiResult<null>>("/auth/reset-password", payload);
}
