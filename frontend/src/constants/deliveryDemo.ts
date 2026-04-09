/**
 * 须与后端 `app.delivery`（如 application.yml）中演示相关项保持一致，
 * 否则地图与「已到店/已取餐」会与自动化推进不同步。
 */
export const DELIVERY_DEMO = {
  timeScale: 0.1,
  assignedToArriveMinutes: 2,
  arriveToPickupMinutes: 1,
  pickupToShipMinutes: 0.5
} as const;

export function demoScaledMs(minutes: number): number {
  return Math.round(minutes * 60 * 1000 * DELIVERY_DEMO.timeScale);
}

/** 三段时间与后端 DeliveryAutomation 一致，供订单页地图时间线使用 */
export function demoPhaseLegMs(): { leg1: number; leg2: number; leg3: number } {
  return {
    leg1: demoScaledMs(DELIVERY_DEMO.assignedToArriveMinutes),
    leg2: demoScaledMs(DELIVERY_DEMO.arriveToPickupMinutes),
    leg3: demoScaledMs(DELIVERY_DEMO.pickupToShipMinutes)
  };
}
