<template>
  <section class="order-page" aria-labelledby="order-page-heading">
    <div
      v-if="toast.visible"
      class="order-toast"
      :class="toast.type"
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      {{ toast.message }}
    </div>
    <h2 id="order-page-heading" class="order-page-title">我的订单</h2>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <p v-else-if="loading" class="muted">加载中…</p>
    <template v-else>
      <p v-if="!orders.length" class="muted">暂无订单，在商家页「模拟支付」或「取消」后会出现在这里。</p>
      <ul v-else class="order-list" aria-label="订单列表">
        <li
          v-for="o in orders"
          :key="o.id"
          class="card order-row"
          :aria-label="`订单 ${o.orderNo || o.id}`"
        >
          <div class="order-row-top">
            <span class="order-id">订单号 {{ o.orderNo || "—" }}</span>
            <span
              class="order-status-tag"
              :class="
                orderStatus(o) === ORDER_STATUS_PENDING
                  ? 'pending'
                  : orderStatus(o) === ORDER_STATUS_CANCELLED
                    ? 'cancelled'
                    : 'paid'
              "
            >
              {{ o.statusText }}
            </span>
          </div>
          <p class="order-merchant">
            {{ o.merchantName || `商家 #${o.merchantId}` }}
          </p>
          <div class="order-row-time">
            <span class="muted">下单时间</span>
            <span>{{ formatTime(o.createdAt) }}</span>
          </div>
          <div
            v-if="orderStatus(o) === ORDER_STATUS_PENDING && o.expireAt"
            class="order-countdown"
            :class="{ 'order-countdown--expired': isPendingExpired(o) }"
            role="status"
            aria-live="polite"
            aria-atomic="true"
          >
            <span class="muted">剩余支付时间</span>
            <span class="order-countdown-value">{{ countdownText(o.expireAt) }}</span>
          </div>
          <ul v-if="o.items && o.items.length" class="order-goods">
            <li v-for="line in o.items" :key="`${o.id}-${line.productId}`" class="order-goods-line">
              <span class="order-goods-name">{{ line.productName }}</span>
              <span class="muted order-goods-qty">×{{ line.quantity }}</span>
              <span class="order-goods-price">¥{{ Number(line.subtotal).toFixed(2) }}</span>
            </li>
          </ul>
          <p v-else class="muted order-goods-empty">暂无商品明细</p>
          <div class="order-fee-rows">
            <div class="order-fee-line">
              <span class="muted">商品合计</span>
              <span>¥{{ feeNum(o.goodsAmount).toFixed(2) }}</span>
            </div>
            <div class="order-fee-line">
              <span class="muted">配送费</span>
              <span>¥{{ feeNum(o.deliveryFee).toFixed(2) }}</span>
            </div>
            <div class="order-fee-line order-fee-line--coupon">
              <span class="muted">优惠券</span>
              <span v-if="feeNum(o.couponAmount) > 0" class="order-coupon-wrap">
                <span class="order-coupon-minus">-¥{{ feeNum(o.couponAmount).toFixed(2) }}</span>
                <span v-if="o.couponCode" class="muted order-coupon-tag">{{ o.couponCode }}</span>
              </span>
              <span v-else class="muted">未使用</span>
            </div>
          </div>
          <div class="order-row-body">
            <span class="muted order-sum">应付</span>
            <span class="order-pay">¥{{ Number(o.payAmount).toFixed(2) }}</span>
          </div>
          <div v-if="orderStatus(o) === ORDER_STATUS_PENDING" class="order-pending-actions">
            <button
              type="button"
              class="order-pay-now-btn"
              :disabled="!!paying[o.id] || !!cancelling[o.id] || isPendingExpired(o)"
              :aria-busy="!!paying[o.id]"
              :title="isPendingExpired(o) ? '支付时间已过' : undefined"
              @click="payPending(o.id)"
            >
              {{
                paying[o.id]
                  ? "支付中…"
                  : isPendingExpired(o)
                    ? "已超时"
                    : "立即支付"
              }}
            </button>
            <button
              type="button"
              class="order-cancel-btn"
              :disabled="!!paying[o.id] || !!cancelling[o.id]"
              :aria-busy="!!cancelling[o.id]"
              @click="openCancelConfirm(o.id)"
            >
              {{ cancelling[o.id] ? "处理中…" : "取消订单" }}
            </button>
          </div>
        </li>
      </ul>
    </template>

    <div
      v-if="cancelConfirmOpen"
      class="order-cancel-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="order-cancel-dialog-title"
      @click.self="closeCancelConfirm"
    >
      <div class="order-cancel-dialog card" @click.stop>
        <p id="order-cancel-dialog-title" class="order-cancel-dialog-title">确认取消该订单？</p>
        <p class="muted order-cancel-dialog-hint">取消后需重新选购下单。</p>
        <div class="order-cancel-dialog-actions">
          <button type="button" class="order-cancel-dialog-no" @click="closeCancelConfirm">
            返回
          </button>
          <button
            type="button"
            class="order-cancel-dialog-yes"
            :disabled="cancelConfirmSubmitting"
            @click="confirmCancelOrder"
          >
            {{ cancelConfirmSubmitting ? "处理中…" : "确认取消" }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { cancelOrder, fetchOrders, payOrder } from "../../api/order";
import { getApiErrorMessage } from "../../utils/apiError";
import {
  ORDER_STATUS_CANCELLED,
  ORDER_STATUS_PENDING,
  type OrderListItem
} from "../../types/order";

const loading = ref(true);
const error = ref("");
const orders = ref<OrderListItem[]>([]);
const paying = ref<Record<number, boolean>>({});
const cancelling = ref<Record<number, boolean>>({});
const tick = ref(0);

const cancelConfirmOpen = ref(false);
const cancelConfirmOrderId = ref<number | null>(null);
const cancelConfirmSubmitting = ref(false);

const toast = ref<{
  visible: boolean;
  type: "success" | "error";
  message: string;
}>({
  visible: false,
  type: "success",
  message: ""
});

let toastTimer: number | undefined;
function showToast(type: "success" | "error", message: string) {
  toast.value.type = type;
  toast.value.message = message;
  toast.value.visible = true;
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => {
    toast.value.visible = false;
  }, 2000);
}

let tickTimer: number | undefined;

const hasPendingOrders = computed(() =>
  orders.value.some((o) => orderStatus(o) === ORDER_STATUS_PENDING)
);

function stopTickTimer() {
  if (tickTimer !== undefined) {
    window.clearInterval(tickTimer);
    tickTimer = undefined;
  }
}

function startTickTimer() {
  if (tickTimer !== undefined) return;
  tickTimer = window.setInterval(() => {
    tick.value++;
    if (tick.value % 15 === 0) {
      void load({ silent: true });
    }
  }, 1000);
}

watch(
  hasPendingOrders,
  (has) => {
    if (has) startTickTimer();
    else stopTickTimer();
  },
  { immediate: true }
);

/** 后端可能返回 number 或字符串，统一成数字再比较状态 */
function orderStatus(o: OrderListItem) {
  return Number(o.status);
}

function parseTime(raw: string) {
  if (!raw) return NaN;
  const normalized = raw.includes("T") ? raw : raw.replace(" ", "T");
  return new Date(normalized).getTime();
}

function feeNum(v: number | undefined | null) {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

function formatTime(raw: string) {
  if (!raw) return "—";
  const t = parseTime(raw);
  if (Number.isNaN(t)) return raw;
  return new Date(t).toLocaleString();
}

function isPendingExpired(o: OrderListItem): boolean {
  if (orderStatus(o) !== ORDER_STATUS_PENDING || !o.expireAt) return false;
  const end = parseTime(o.expireAt);
  return Number.isFinite(end) && end <= Date.now();
}

function countdownText(expireAt: string) {
  tick.value;
  const end = parseTime(expireAt);
  if (Number.isNaN(end)) return "—";
  const sec = Math.max(0, Math.floor((end - Date.now()) / 1000));
  if (sec <= 0) return "已过期";
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

async function load(opts?: { silent?: boolean }) {
  const silent = !!opts?.silent;
  if (!silent) {
    loading.value = true;
    error.value = "";
  }
  try {
    orders.value = await fetchOrders();
  } catch (e: unknown) {
    if (silent) {
      showToast("error", getApiErrorMessage(e, "订单列表更新失败"));
    } else {
      error.value = getApiErrorMessage(e, "加载失败");
    }
  } finally {
    if (!silent) loading.value = false;
  }
}

async function payPending(orderId: number) {
  if (paying.value[orderId]) return;
  const row = orders.value.find((o) => o.id === orderId);
  if (row && isPendingExpired(row)) {
    showToast("error", "支付时间已过，正在刷新订单");
    await load({ silent: true });
    return;
  }
  paying.value[orderId] = true;
  try {
    await payOrder(orderId);
    showToast("success", "支付成功");
    await load({ silent: true });
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "支付失败"));
    await load({ silent: true });
  } finally {
    paying.value[orderId] = false;
  }
}

function openCancelConfirm(orderId: number) {
  if (paying.value[orderId] || cancelling.value[orderId]) return;
  cancelConfirmOrderId.value = orderId;
  cancelConfirmOpen.value = true;
}

function closeCancelConfirm() {
  if (cancelConfirmSubmitting.value) return;
  cancelConfirmOpen.value = false;
  cancelConfirmOrderId.value = null;
}

async function confirmCancelOrder() {
  const id = cancelConfirmOrderId.value;
  if (id == null || cancelConfirmSubmitting.value) return;
  cancelConfirmSubmitting.value = true;
  cancelling.value[id] = true;
  try {
    await cancelOrder(id);
    closeCancelConfirm();
    showToast("success", "订单已取消");
    await load({ silent: true });
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "取消失败"));
    await load({ silent: true });
  } finally {
    cancelConfirmSubmitting.value = false;
    cancelling.value[id] = false;
  }
}

function onGlobalKeydown(ev: KeyboardEvent) {
  if (ev.key !== "Escape" || !cancelConfirmOpen.value) return;
  closeCancelConfirm();
  ev.preventDefault();
}

onMounted(() => {
  window.addEventListener("keydown", onGlobalKeydown);
  void load();
});

onUnmounted(() => {
  window.removeEventListener("keydown", onGlobalKeydown);
  stopTickTimer();
  if (toastTimer) window.clearTimeout(toastTimer);
});
</script>

<style scoped>
.order-page {
  position: relative;
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
  box-sizing: border-box;
}
.order-toast {
  position: fixed;
  top: calc(var(--header-height) + 12px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 12000;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  max-width: min(90vw, 360px);
  text-align: center;
}
.order-toast.success {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}
.order-toast.error {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}
.order-page-title {
  margin: 0 0 12px;
  font-size: 18px;
}
.order-list {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 100%;
}
/* 每张订单独立卡片：沿用全局 .card 样式，并占满居中栏宽度 */
.order-page li.order-row.card {
  max-width: none;
  width: 100%;
  box-sizing: border-box;
  margin-bottom: 16px;
}
.order-page li.order-row.card:last-child {
  margin-bottom: 0;
}
.order-row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
}
.order-id {
  font-weight: 600;
}
.order-status-tag {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
}
.order-status-tag.paid {
  background: #dcfce7;
  color: #166534;
}
.order-status-tag.pending {
  background: #fef3c7;
  color: #92400e;
}
.order-status-tag.cancelled {
  background: #f3f4f6;
  color: #6b7280;
}
.order-merchant {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}
.order-row-time {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 6px;
}
.order-countdown {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin-bottom: 8px;
  padding: 6px 8px;
  background: #fffbeb;
  border-radius: 6px;
}
.order-countdown--expired {
  background: #fef2f2;
}
.order-countdown--expired .order-countdown-value {
  color: #b91c1c;
}
.order-countdown-value {
  font-weight: 700;
  color: #b45309;
  font-variant-numeric: tabular-nums;
}
.order-goods {
  list-style: none;
  padding: 0;
  margin: 0 0 10px;
  border-top: 1px solid #f3f4f6;
  padding-top: 8px;
}
.order-goods-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 14px;
  padding: 4px 0;
  border-bottom: 1px solid #f9fafb;
}
.order-goods-line:last-child {
  border-bottom: none;
}
.order-goods-name {
  flex: 1;
  min-width: 0;
}
.order-goods-qty {
  flex-shrink: 0;
  font-size: 13px;
}
.order-goods-price {
  flex-shrink: 0;
  font-weight: 600;
  color: #374151;
}
.order-goods-empty {
  margin: 0 0 8px;
  font-size: 13px;
}
.order-fee-rows {
  margin: 8px 0 0;
  padding: 8px 0 0;
  border-top: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
}
.order-fee-line {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}
.order-coupon-wrap {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: flex-end;
  gap: 6px;
  text-align: right;
}
.order-coupon-minus {
  font-weight: 700;
  color: #059669;
  font-variant-numeric: tabular-nums;
}
.order-coupon-tag {
  font-size: 12px;
}
.order-row-body {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  gap: 8px;
  font-size: 14px;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
}
.order-sum {
  font-size: 13px;
}
.order-pay {
  font-weight: 800;
  font-size: 16px;
  color: #dc2626;
}
.order-pending-actions {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.order-pay-now-btn {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
button.order-pay-now-btn:hover:not(:disabled) {
  background: #1d4ed8;
  color: #fff;
  box-shadow: none;
  transform: none;
}
.order-pay-now-btn:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}
.order-cancel-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
button.order-cancel-btn:hover:not(:disabled) {
  background: #f9fafb;
  color: #374151;
  box-shadow: none;
  transform: none;
}
.order-cancel-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.order-cancel-overlay {
  position: fixed;
  inset: 0;
  z-index: 11000;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.order-cancel-dialog {
  max-width: 360px;
  width: 100%;
  padding: 20px 22px;
  margin: 0;
}
.order-cancel-dialog-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  text-align: center;
}
.order-cancel-dialog-hint {
  margin: 0 0 18px;
  font-size: 13px;
  text-align: center;
  line-height: 1.4;
}
.order-cancel-dialog-actions {
  display: flex;
  gap: 10px;
}
.order-cancel-dialog-actions button {
  flex: 1;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.order-cancel-dialog-no {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}
.order-cancel-dialog-no:hover {
  background: #f9fafb;
}
.order-cancel-dialog-yes {
  border: none;
  background: #dc2626;
  color: #fff;
}
.order-cancel-dialog-yes:hover:not(:disabled) {
  background: #b91c1c;
}
.order-cancel-dialog-yes:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
