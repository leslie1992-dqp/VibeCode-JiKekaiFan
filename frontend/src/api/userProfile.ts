import request from "./request";
import type { ApiResult, UserInfo } from "../types/auth";
import type { MerchantReviewPage } from "../types/merchantReview";

export async function uploadUserAvatar(file: File): Promise<UserInfo> {
  const form = new FormData();
  form.append("file", file);
  const res = await request.post<ApiResult<UserInfo>>("/users/me/avatar", form, {
    timeout: 60000
  });
  return res.data.data;
}

export async function fetchMyReviews(
  pageNo = 1,
  pageSize = 10
): Promise<MerchantReviewPage> {
  const res = await request.get<ApiResult<MerchantReviewPage>>("/users/me/reviews", {
    params: { pageNo, pageSize }
  });
  return res.data.data;
}
