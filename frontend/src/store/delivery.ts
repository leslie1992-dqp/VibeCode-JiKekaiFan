import { defineStore } from "pinia";
import { fetchDeliverySummary, subscribeDelivery } from "../api/delivery";
import type { DeliverySummary } from "../types/delivery";

type MapState = Record<number, DeliverySummary | null>;

export const useDeliveryStore = defineStore("delivery", {
  state: () => ({
    summaryByOrderId: {} as MapState
  }),
  actions: {
    async loadSummary(orderId: number) {
      this.summaryByOrderId[orderId] = await fetchDeliverySummary(orderId);
    },
    watchOrder(orderId: number, onRefresh: () => Promise<void> | void) {
      const src = subscribeDelivery(orderId, () => {
        void onRefresh();
      });
      return () => src?.close();
    }
  }
});
