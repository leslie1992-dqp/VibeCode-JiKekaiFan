/** 与后端 OrderStatus 一致 */
export const ORDER_STATUS_PAID = 1;
export const ORDER_STATUS_PENDING = 2;
export const ORDER_STATUS_CANCELLED = 3;
export const ORDER_STATUS_DELIVERY_ASSIGNED = 4;
/** 骑手已到店 */
export const ORDER_STATUS_DELIVERY_AT_MERCHANT = 9;
export const ORDER_STATUS_DELIVERY_PICKING_UP = 5;
export const ORDER_STATUS_DELIVERY_IN_TRANSIT = 6;
export const ORDER_STATUS_DELIVERY_DONE = 7;
export const ORDER_STATUS_DELIVERY_FAILED = 8;

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
