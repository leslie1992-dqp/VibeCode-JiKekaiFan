export interface DraftItem {
  merchantId: number;
  productId: number;
  productName: string;
  price: number;
  quantity: number;
  subtotal: number;
}

export interface DraftMerchantPayload {
  merchantId: number;
  merchantName?: string | null;
  items: DraftItem[];
  totalQuantity: number;
  couponCode: string | null;
  couponAmount: number;
  /** none | no_coupon | ineligible | applied */
  couponLineStatus?: string;
  couponRuleText?: string | null;
  couponHint?: string | null;
  /** 商品小计（不含配送） */
  goodsAmount: number;
  /** 配送费 */
  deliveryFee: number;
  /** 商品 + 配送（扣券前） */
  originalAmount: number;
  payableAmount: number;
}

export interface CheckoutResult {
  orderId: number;
  orderNo: string;
  merchantId: number;
  payAmount: number;
}

export interface CartCheckoutPreview {
  merchants: DraftMerchantPayload[];
  grandPayable: number;
}

export interface CartCheckoutBatchResult {
  orders: CheckoutResult[];
}
