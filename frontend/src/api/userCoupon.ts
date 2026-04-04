import request from "./request";
import type { ApiResult } from "../types/auth";
import type { UserCouponItem } from "../types/coupon";

export async function fetchMyCoupons(): Promise<UserCouponItem[]> {
  const res = await request.get<ApiResult<UserCouponItem[]>>("/users/me/coupons");
  return res.data.data;
}

export async function fetchClaimedSeckillIds(merchantId: number): Promise<number[]> {
  const res = await request.get<ApiResult<number[]>>("/users/me/claimed-seckill-ids", {
    params: { merchantId }
  });
  return res.data.data;
}
