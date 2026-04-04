import request from "./request";
import type { ApiResult } from "../types/auth";
import type { CartCheckoutBatchResult, CartCheckoutPreview } from "../types/draft";
import type { CartItem } from "../types/cart";

export async function addCartItem(productId: number, quantity = 1): Promise<void> {
  await request.post<ApiResult<null>>("/cart/items", {
    productId,
    quantity
  });
}

export async function fetchCartItems(): Promise<CartItem[]> {
  const res = await request.get<ApiResult<CartItem[]>>("/cart/items");
  return res.data.data;
}

export async function removeCartItem(productId: number): Promise<void> {
  await request.delete<ApiResult<null>>(`/cart/items/${productId}`);
}

export async function clearCart(): Promise<void> {
  await request.delete<ApiResult<null>>("/cart/clear");
}

export async function postCartCheckoutPreview(productIds: number[]): Promise<CartCheckoutPreview> {
  const res = await request.post<ApiResult<CartCheckoutPreview>>("/cart/checkout-preview", {
    productIds
  });
  return res.data.data;
}

export async function postCartCheckout(
  productIds: number[],
  immediate: boolean
): Promise<CartCheckoutBatchResult> {
  const res = await request.post<ApiResult<CartCheckoutBatchResult>>("/cart/checkout", {
    productIds,
    immediate
  });
  return res.data.data;
}

export async function increaseCartItem(
  productId: number,
  quantity = 1
): Promise<void> {
  await request.post<ApiResult<null>>(`/cart/items/${productId}/increase`, {
    quantity
  });
}

export async function decreaseCartItem(
  productId: number,
  quantity = 1
): Promise<void> {
  await request.post<ApiResult<null>>(`/cart/items/${productId}/decrease`, {
    quantity
  });
}

