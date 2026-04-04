export interface UserCouponItem {
  id: number;
  merchantId: number;
  merchantName: string;
  title: string;
  thresholdAmount: number;
  discountAmount: number;
  expireAt: string;
  /** 1 未使用 2 已使用 3 待支付占用 */
  status: number;
}
