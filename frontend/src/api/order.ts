import request from "./request";
import type { ApiResult } from "../types/auth";
import type { OrderListItem } from "../types/order";

export async function fetchOrders(): Promise<OrderListItem[]> {
  const res = await request.get<ApiResult<OrderListItem[]>>("/orders");
  return res.data.data;
}

export async function payOrder(orderId: number): Promise<void> {
  await request.post<ApiResult<null>>(`/orders/${orderId}/pay`);
}

export async function cancelOrder(orderId: number): Promise<void> {
  await request.post<ApiResult<null>>(`/orders/${orderId}/cancel`);
}
