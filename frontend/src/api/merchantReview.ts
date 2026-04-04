import request from "./request";
import type { ApiResult } from "../types/auth";
import type { MerchantReviewCreatePayload, MerchantReviewPage } from "../types/merchantReview";

export async function fetchMerchantReviews(
  merchantId: number,
  pageNo = 1,
  pageSize = 20
): Promise<MerchantReviewPage> {
  const res = await request.get<ApiResult<MerchantReviewPage>>(
    `/merchants/${merchantId}/reviews`,
    { params: { pageNo, pageSize } }
  );
  return res.data.data;
}

export async function postMerchantReview(payload: MerchantReviewCreatePayload): Promise<number> {
  const res = await request.post<ApiResult<number>>("/users/me/merchant-reviews", payload);
  return res.data.data;
}

/** 本地上传评价配图，返回可访问的完整 URL 列表（单次最多 6 张，由后端校验） */
export async function uploadMerchantReviewImages(files: File[]): Promise<string[]> {
  const form = new FormData();
  for (const f of files) {
    form.append("files", f);
  }
  const res = await request.post<ApiResult<string[]>>("/users/me/merchant-review-images", form, {
    timeout: 60000
  });
  return res.data.data;
}
