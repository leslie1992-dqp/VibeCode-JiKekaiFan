export interface LoginRequest {
  mobile: string;
  password: string;
}

export interface RegisterRequest {
  mobile: string;
  password: string;
  nickname: string;
}

export interface ResetPasswordRequest {
  mobile: string;
  newPassword: string;
}

export interface UserInfo {
  id: number;
  mobile: string;
  nickname: string;
  /** 头像完整 URL，未设置时为空 */
  avatarUrl?: string | null;
}

export interface LoginData {
  token: string;
  expireAt: number;
  userInfo: UserInfo;
}

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}
