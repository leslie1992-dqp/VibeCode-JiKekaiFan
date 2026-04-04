/** 与后端 OrderStatus 一致 */
export const ORDER_STATUS_PAID = 1;
export const ORDER_STATUS_PENDING = 2;
export const ORDER_STATUS_CANCELLED = 3;

export interface OrderLineItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface OrderListItem {
  id: number;
  /** 12 位数字订单号 */
  orderNo: string;
  merchantId: number;
  merchantName?: string | null;
  totalAmount: number;
  goodsAmount: number;
  deliveryFee: number;
  /** 下单时券标识，无券可为 null */
  couponCode?: string | null;
  couponAmount: number;
  payAmount: number;
  status: number;
  statusText: string;
  /** 待支付订单截止时间 */
  expireAt?: string | null;
  createdAt: string;
  items: OrderLineItem[];
}
