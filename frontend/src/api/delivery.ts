import request from "./request";
import type { ApiResult } from "../types/auth";
import type { DeliverySummary, DeliveryTrackPoint } from "../types/delivery";
import { useAuthStore } from "../store/auth";

export async function fetchDeliverySummary(orderId: number): Promise<DeliverySummary> {
  const res = await request.get<ApiResult<DeliverySummary>>(`/orders/${orderId}/delivery`);
  return res.data.data;
}

export async function fetchDeliveryTrack(orderId: number, limit = 30): Promise<DeliveryTrackPoint[]> {
  const res = await request.get<ApiResult<DeliveryTrackPoint[]>>(`/orders/${orderId}/delivery/track`, {
    params: { limit }
  });
  return res.data.data;
}

export function subscribeDelivery(orderId: number, onData: (raw: MessageEvent) => void): EventSource | null {
  const token = useAuthStore().token?.trim();
  if (!token) return null;
  const src = new EventSource(
    `http://localhost:8081/api/v1/orders/${orderId}/delivery/subscribe?token=${encodeURIComponent(token)}`
  );
  src.addEventListener("delivery", onData);
  return src;
}
