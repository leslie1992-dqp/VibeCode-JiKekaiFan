import request from "./request";
import type { ApiResult } from "../types/auth";
import type { CheckoutResult, DraftMerchantPayload } from "../types/draft";

export async function addDraftItem(productId: number, quantity = 1): Promise<void> {
  await request.post<ApiResult<null>>("/merchant-drafts/items", {
    productId,
    quantity
  });
}

export async function fetchMerchantDraft(merchantId: number): Promise<DraftMerchantPayload> {
  const res = await request.get<ApiResult<DraftMerchantPayload>>(
    `/merchant-drafts/merchants/${merchantId}`
  );
  return res.data.data;
}

export async function increaseDraftItem(
  merchantId: number,
  productId: number,
  quantity = 1
): Promise<void> {
  await request.post<ApiResult<null>>(
    `/merchant-drafts/merchants/${merchantId}/items/${productId}/increase`,
    { quantity }
  );
}

export async function decreaseDraftItem(
  merchantId: number,
  productId: number,
  quantity = 1
): Promise<void> {
  await request.post<ApiResult<null>>(
    `/merchant-drafts/merchants/${merchantId}/items/${productId}/decrease`,
    { quantity }
  );
}

export async function removeDraftItem(merchantId: number, productId: number): Promise<void> {
  await request.delete<ApiResult<null>>(
    `/merchant-drafts/merchants/${merchantId}/items/${productId}`
  );
}

export async function checkoutDraft(merchantId: number): Promise<CheckoutResult> {
  const res = await request.post<ApiResult<CheckoutResult>>(
    `/merchant-drafts/merchants/${merchantId}/checkout`
  );
  return res.data.data;
}

/** 取消立即支付：生成待支付订单，30 分钟内有效 */
export async function checkoutPendingDraft(merchantId: number): Promise<CheckoutResult> {
  const res = await request.post<ApiResult<CheckoutResult>>(
    `/merchant-drafts/merchants/${merchantId}/checkout-pending`
  );
  return res.data.data;
}
