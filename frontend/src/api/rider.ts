import request from "./request";
import type { ApiResult } from "../types/auth";

export async function riderAcceptTask(taskId: number): Promise<void> {
  await request.post<ApiResult<null>>(`/rider/tasks/${taskId}/accept`);
}

export async function riderPickUpTask(taskId: number): Promise<void> {
  await request.post<ApiResult<null>>(`/rider/tasks/${taskId}/pickup`);
}

export async function riderDeliverTask(taskId: number): Promise<void> {
  await request.post<ApiResult<null>>(`/rider/tasks/${taskId}/deliver`);
}

export async function riderReportLocation(payload: {
  taskId: number;
  latitude: number;
  longitude: number;
  speedKmh?: number;
  heading?: number;
  clientTime?: string;
}): Promise<void> {
  await request.post<ApiResult<null>>("/rider/location", payload);
}
