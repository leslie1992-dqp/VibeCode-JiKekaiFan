/** 与后端 DeliveryTaskStatus 一致（用户端展示用） */
export const DELIVERY_TASK_STATUS_WAITING_DISPATCH = 10;
export const DELIVERY_TASK_STATUS_ASSIGNED = 20;

export interface DeliveryTrackPoint {
  latitude: number;
  longitude: number;
  speedKmh?: number | null;
  heading?: number | null;
  clientTime?: string | null;
  createdAt: string;
}

export interface DeliverySummary {
  taskId: number;
  orderId: number;
  status: number;
  statusText: string;
  riderId?: number | null;
  riderName?: string | null;
  expectedArriveAt?: string | null;
  /** 商家相对用户的演示距离 km，与后端 merchant.distance_km 一致 */
  routeDistanceKm?: number | null;
  latestLocation?: DeliveryTrackPoint | null;
  events: string[];
}
