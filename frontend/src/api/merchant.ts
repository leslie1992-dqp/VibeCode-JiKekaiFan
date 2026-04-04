import request from "./request";
import type { ApiResult } from "../types/auth";
import type {
  MerchantListItem,
  MerchantPage,
  ProductCategory,
  ProductListItem,
  SeckillCoupon
} from "../types/merchant";

export type MerchantSortMode = "recommend" | "distance" | "rating" | "sales";

export async function fetchMerchantPage(params: {
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
  /** 推荐 recommend | 距离 distance | 评分 rating | 月售 sales */
  sort?: MerchantSortMode;
}): Promise<MerchantPage> {
  const res = await request.get<ApiResult<MerchantPage>>("/merchants", { params });
  return res.data.data;
}

export async function fetchMerchant(merchantId: number): Promise<MerchantListItem> {
  const res = await request.get<ApiResult<MerchantListItem>>(`/merchants/${merchantId}`);
  return res.data.data;
}

export async function fetchMerchantCategories(merchantId: number): Promise<ProductCategory[]> {
  const res = await request.get<ApiResult<ProductCategory[]>>(
    `/merchants/${merchantId}/categories`
  );
  return res.data.data;
}

export async function fetchMerchantProducts(
  merchantId: number,
  categoryId?: number
): Promise<ProductListItem[]> {
  const res = await request.get<ApiResult<ProductListItem[]>>(
    `/merchants/${merchantId}/products`,
    { params: categoryId != null ? { categoryId } : undefined }
  );
  return res.data.data;
}

export async function fetchMerchantSeckillCoupons(merchantId: number): Promise<SeckillCoupon[]> {
  const res = await request.get<ApiResult<SeckillCoupon[]>>(
    `/merchants/${merchantId}/seckill-coupons`
  );
  return res.data.data;
}

export async function claimSeckillCoupon(couponId: number): Promise<void> {
  await request.post<ApiResult<null>>(`/merchant-seckill-coupons/${couponId}/claim`);
}
