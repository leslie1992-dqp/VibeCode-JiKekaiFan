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
          <div v-if="deliveryByOrderId[o.id]" class="order-delivery-box">
            <div class="order-delivery-line">
              <span class="muted">配送状态</span>
              <span>{{ deliveryUiStatusText(o.id) }}</span>
            </div>
            <div class="order-delivery-line">
              <span class="muted">骑手</span>
              <span>{{ deliveryByOrderId[o.id]?.riderName || "系统分配中" }}</span>
            </div>
            <div class="order-delivery-line">
              <span class="muted">预计送达</span>
              <span>{{ etaRangeText(deliveryByOrderId[o.id]) }}</span>
            </div>
            <div
              class="order-delivery-line"
              :class="{ 'order-delivery-line--danger': isDeliveryTimeout(deliveryByOrderId[o.id]) }"
            >
              <span class="muted">到达倒计时</span>
              <span>{{ etaCountdownText(deliveryByOrderId[o.id]) }}</span>
            </div>
            <div
              v-if="deliveryByOrderId[o.id]?.latestLocation && isDeliveryAssigned(o.id)"
              class="order-delivery-line order-delivery-line--mono"
            >
              <span class="muted">最新坐标</span>
              <span>
                {{ Number(deliveryByOrderId[o.id]?.latestLocation?.latitude).toFixed(6) }},
                {{ Number(deliveryByOrderId[o.id]?.latestLocation?.longitude).toFixed(6) }}
              </span>
            </div>
            <button
              type="button"
              class="order-delivery-toggle"
              @click="toggleDeliveryDetail(o.id)"
            >
              {{ expandedDelivery[o.id] ? "收起配送详情" : "展开配送详情" }}
            </button>
            <div v-if="expandedDelivery[o.id]" class="order-delivery-detail">
              <div class="order-map">
                <div class="order-map-grid"></div>
                <div class="order-route-line"></div>
                <div class="order-map-label order-map-label--start" :style="mapPinStyle(o.id, 'merchant')">
                  商家
                </div>
                <div class="order-map-distance">{{ distanceLabel(o.id) }} {{ distanceText(o.id) }}</div>
                <div class="order-user-pin" :style="mapPinStyle(o.id, 'user')" title="你的位置">你</div>
                <div
                  v-if="isDeliveryAssigned(o.id)"
                  class="order-rider-pin"
                  :style="riderPinStyle(o.id)"
                  title="骑手位置"
                >
                  <span class="order-rider-avatar">{{ riderAvatarText(o.id) }}</span>
                  <span class="order-rider-arrow" :style="riderArrowStyle(o.id)">▲</span>
                </div>
                <div v-else class="order-map-waiting" role="status">派单完成后显示骑手位置</div>
              </div>
              <div class="order-progress">
                <div class="order-progress-track">
                  <div
                    class="order-progress-fill"
                    :style="{ width: `${deliveryProgressPercent(o.id)}%` }"
                  ></div>
                </div>
                <div class="order-progress-meta">
                  <span>骑手路程进度</span>
                  <span>{{ deliveryProgressPercent(o.id).toFixed(0) }}%</span>
                </div>
              </div>
              <div class="order-delivery-line">
                <span class="muted">{{ distanceLabel(o.id) }}</span>
                <span>{{ distanceText(o.id) }}</span>
              </div>
              <div class="order-delivery-line">
                <span class="muted">轨迹点数</span>
                <span>{{ (trackByOrderId[o.id] || []).length }} 个</span>
              </div>
              <div class="order-delivery-timeline">
                <div
                  v-for="step in deliveryTimeline(o.id)"
                  :key="`${o.id}-${step.key}`"
                  class="order-delivery-step"
                  :class="{
                    'is-done': step.done,
                    'is-current': step.current
                  }"
                >
                  <span class="order-delivery-step-dot"></span>
                  <span class="order-delivery-step-label">{{ step.label }}</span>
                  <span class="order-delivery-step-time">{{ step.timeText }}</span>
                </div>
              </div>
            </div>
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
import { fetchDeliverySummary, fetchDeliveryTrack, subscribeDelivery } from "../../api/delivery";
import { getApiErrorMessage } from "../../utils/apiError";
import {
  ORDER_STATUS_CANCELLED,
  ORDER_STATUS_DELIVERY_ASSIGNED,
  ORDER_STATUS_DELIVERY_AT_MERCHANT,
  ORDER_STATUS_DELIVERY_IN_TRANSIT,
  ORDER_STATUS_DELIVERY_PICKING_UP,
  ORDER_STATUS_PAID,
  ORDER_STATUS_PENDING,
  type OrderListItem
} from "../../types/order";
import {
  DELIVERY_TASK_STATUS_ASSIGNED,
  type DeliverySummary,
  type DeliveryTrackPoint
} from "../../types/delivery";
import { demoPhaseLegMs } from "../../constants/deliveryDemo";

const loading = ref(true);
const error = ref("");
const orders = ref<OrderListItem[]>([]);
const paying = ref<Record<number, boolean>>({});
const cancelling = ref<Record<number, boolean>>({});
const tick = ref(0);
const deliveryByOrderId = ref<Record<number, DeliverySummary>>({});
const trackByOrderId = ref<Record<number, DeliveryTrackPoint[]>>({});
const unsubscribeByOrderId = ref<Record<number, () => void>>({});
const expandedDelivery = ref<Record<number, boolean>>({});

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

/** 订单列表里已是配送中，但 delivery 摘要可能尚未写入 → 不能只看 deliveryByOrderId，否则轮询会停、状态不刷新 */
function orderHasInFlightDelivery(o: OrderListItem): boolean {
  const s = orderStatus(o);
  return (
    s === ORDER_STATUS_DELIVERY_ASSIGNED ||
    s === ORDER_STATUS_DELIVERY_AT_MERCHANT ||
    s === ORDER_STATUS_DELIVERY_PICKING_UP ||
    s === ORDER_STATUS_DELIVERY_IN_TRANSIT
  );
}

const shouldPollOrderList = computed(
  () =>
    hasPendingOrders.value ||
    orders.value.some((o) => orderHasInFlightDelivery(o)) ||
    /** 已支付、尚未写入配送态时订单仍为 1，若不轮询且 SSE 不可用，派单成功后界面不会刷新 */
    orders.value.some((o) => orderStatus(o) === ORDER_STATUS_PAID)
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
    if (tick.value % 10 === 0) {
      void load({ silent: true });
    }
  }, 1000);
}

watch(
  shouldPollOrderList,
  (need) => {
    if (need) startTickTimer();
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

function etaRangeText(summary?: DeliverySummary) {
  if (!summary?.expectedArriveAt) return "待计算";
  const center = parseTime(summary.expectedArriveAt);
  if (Number.isNaN(center)) return "待计算";
  const from = new Date(center - 5 * 60 * 1000);
  const to = new Date(center + 5 * 60 * 1000);
  const hhmm = (d: Date) =>
    `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  return `${hhmm(from)}-${hhmm(to)}`;
}

function isDeliveryTimeout(summary?: DeliverySummary) {
  if (!summary || summary.status !== 70) return false;
  const end = expectedRangeEndMs(summary);
  if (Number.isNaN(end)) return true;
  return Date.now() > end;
}

function expectedRangeEndMs(summary?: DeliverySummary) {
  if (!summary?.expectedArriveAt) return NaN;
  const center = parseTime(summary.expectedArriveAt);
  if (Number.isNaN(center)) return NaN;
  return center + 5 * 60 * 1000;
}

function etaCountdownText(summary?: DeliverySummary) {
  tick.value;
  if (!summary?.expectedArriveAt) return "待计算";
  if (summary.status === 50) return "已送达";
  const end = expectedRangeEndMs(summary);
  if (summary.status === 70 && Date.now() > end) return "已超时";
  if (Number.isNaN(end)) return "待计算";
  const sec = Math.floor((end - Date.now()) / 1000);
  if (sec <= 0) return "即将超时";
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}分${s.toString().padStart(2, "0")}秒`;
}

async function pushRefreshDeliveryOrder(orderId: number) {
  try {
    deliveryByOrderId.value[orderId] = await fetchDeliverySummary(orderId);
    trackByOrderId.value[orderId] = await fetchDeliveryTrack(orderId, 60);
  } catch {
    // 与轮询一致：推送失败时忽略，由定时 load 对齐
  }
}

/**
 * 勿对列表中每笔订单建立 EventSource：HTTP/1.1 下同源并发连接约 6 条，
 * 多笔订单会占满槽位，导致全站 Axios 排队、其它页面长期「加载中」。
 * 仅在用户展开配送详情时订阅，收起时关闭。
 */
function ensureDeliverySse(orderId: number) {
  if (unsubscribeByOrderId.value[orderId]) return;
  const sub = subscribeDelivery(orderId, () => {
    void pushRefreshDeliveryOrder(orderId);
  });
  if (sub) {
    unsubscribeByOrderId.value[orderId] = () => sub.close();
  }
}

function toggleDeliveryDetail(orderId: number) {
  const next = !expandedDelivery.value[orderId];
  expandedDelivery.value[orderId] = next;
  if (next) {
    void ensureDeliverySse(orderId);
  } else {
    unsubscribeByOrderId.value[orderId]?.();
    delete unsubscribeByOrderId.value[orderId];
  }
}

/** 与后端自动化阶段时长对齐，用于约束「展示状态」不早于地图上骑手的物理进度 */
interface TimelineContext {
  totalMs: number;
  createdAt: number;
  leg1: number;
  leg2: number;
  leg3: number;
  elapsed: number;
}

function getTimelineContext(orderId: number): TimelineContext | null {
  const order = orders.value.find((o) => o.id === orderId);
  const summary = deliveryByOrderId.value[orderId];
  if (!order || !summary) {
    return null;
  }
  const c = parseTime(order.createdAt);
  if (Number.isNaN(c)) {
    return null;
  }
  const eta = summary.expectedArriveAt ? parseTime(summary.expectedArriveAt) : NaN;
  const totalMs =
    Number.isFinite(c) && Number.isFinite(eta) && eta > c ? eta - c : 45_000;
  const legs = demoPhaseLegMs();
  let leg1 = legs.leg1;
  let leg2 = legs.leg2;
  let leg3 = legs.leg3;
  const sum = leg1 + leg2 + leg3;
  if (sum > totalMs && totalMs > 0) {
    const scale = (totalMs * 0.95) / sum;
    leg1 *= scale;
    leg2 *= scale;
    leg3 *= scale;
  }
  const elapsed = Date.now() - c;
  return { totalMs, createdAt: c, leg1, leg2, leg3, elapsed };
}

/** 配送任务状态：与后端 summary.status 一致，用于文案与时间轴（不再做 min(后端,时间线) 压制，否则会长期「待更新」） */
function deliveryTaskStatus(orderId: number): number {
  const s = deliveryByOrderId.value[orderId];
  return Number(s?.status ?? 0);
}

function isDeliveryAssigned(orderId: number): boolean {
  return deliveryTaskStatus(orderId) >= DELIVERY_TASK_STATUS_ASSIGNED;
}

function deliveryUiStatusText(orderId: number): string {
  const summary = deliveryByOrderId.value[orderId];
  if (!summary) {
    return "待派单";
  }
  return summary.statusText?.trim() || "—";
}

type RoutePhase =
  | "waitingDispatch"
  | "toMerchant"
  | "atMerchant"
  | "pickedAtStore"
  | "toUser"
  | "done"
  | "timeout";

function clamp(n: number, lo: number, hi: number) {
  return Math.max(lo, Math.min(hi, n));
}

function hash32(a: number, b: number): number {
  let x = (Math.imul(a, 374761393) + Math.imul(b, 668265263) + 17) >>> 0;
  x = Math.imul(x ^ (x >>> 13), 1274126177) >>> 0;
  return x;
}

function merchantUserPositions(merchantId: number, routeKm: number) {
  const rk = Number(routeKm);
  const safeKm = Number.isFinite(rk) && rk > 0 ? rk : 3;
  const user = { x: 76, y: 74 };
  const seed = hash32(merchantId, 901);
  const ang = ((seed % 360) * Math.PI) / 180;
  const du = 22 + Math.min(18, safeKm * 4);
  let mx = user.x + Math.cos(ang) * du;
  let my = user.y + Math.sin(ang) * du;
  mx = clamp(mx, 14, 70);
  my = clamp(my, 14, 70);
  let sep = Math.hypot(mx - user.x, my - user.y);
  if (!Number.isFinite(sep) || sep < 24) {
    const a2 = ((seed * 17) % 360) * (Math.PI / 180);
    mx = clamp(user.x + Math.cos(a2) * 34, 14, 70);
    my = clamp(user.y + Math.sin(a2) * 34, 14, 70);
    sep = Math.hypot(mx - user.x, my - user.y);
    if (!Number.isFinite(sep) || sep < 24) {
      mx = clamp(user.x - 32, 14, 70);
      my = clamp(user.y - 28, 14, 70);
    }
  }
  return { merchant: { x: mx, y: my }, user };
}

/**
 * 起点在「用户→商家」射线、商家外侧（远离用户），避免随机在商家周围时落在用户与商家之间，
 * 导致去商家直线先经过「你」的锚点。
 */
function riderStartFor(
  orderId: number,
  merchant: { x: number; y: number },
  user: { x: number; y: number }
) {
  const ux = merchant.x - user.x;
  const uy = merchant.y - user.y;
  let len = Math.hypot(ux, uy);
  if (!Number.isFinite(len) || len < 1e-3) {
    return {
      x: clamp(merchant.x - 14, 4, 96),
      y: clamp(merchant.y - 12, 4, 96)
    };
  }
  const dx = ux / len;
  const dy = uy / len;
  const spread = 8 + (hash32(orderId, 70001) % 50) / 10;
  return {
    x: clamp(merchant.x + dx * spread, 4, 96),
    y: clamp(merchant.y + dy * spread, 4, 96)
  };
}

function mapLayoutForOrder(orderId: number) {
  const order = orders.value.find((o) => o.id === orderId);
  const routeKm = routeKmForOrder(orderId);
  const mid = order?.merchantId ?? 1;
  return merchantUserPositions(mid, routeKm);
}

function mapPinStyle(orderId: number, role: "merchant" | "user") {
  const { merchant, user } = mapLayoutForOrder(orderId);
  const p = role === "merchant" ? merchant : user;
  return {
    left: `${p.x}%`,
    top: `${p.y}%`,
    transform: "translate(-50%, -50%)"
  };
}

function routeKmForOrder(orderId: number) {
  const summary = deliveryByOrderId.value[orderId];
  const km = Number(summary?.routeDistanceKm);
  if (Number.isFinite(km) && km > 0) {
    return km;
  }
  const mid = orders.value.find((o) => o.id === orderId)?.merchantId ?? 1;
  const h = hash32(mid, 404);
  return 0.8 + (h % 420) / 100;
}

function legKm(routeKm: number) {
  return {
    toMerchant: routeKm * 0.42,
    toUser: routeKm * 0.58,
    total: routeKm
  };
}

function routeSnapshot(orderId: number) {
  tick.value;
  const summary = deliveryByOrderId.value[orderId];
  const order = orders.value.find((o) => o.id === orderId);
  const routeKm = routeKmForOrder(orderId);
  const { toMerchant: kmToM, toUser: kmToU } = legKm(routeKm);
  const mid = order?.merchantId ?? 1;
  const { merchant, user } = merchantUserPositions(mid, routeKm);
  const riderStart = riderStartFor(orderId, merchant, user);
  const startDist = Math.hypot(riderStart.x - merchant.x, riderStart.y - merchant.y) || 1;
  const legUserLen = Math.hypot(user.x - merchant.x, user.y - merchant.y) || 1;

  if (!summary) {
    return {
      x: riderStart.x,
      y: riderStart.y,
      phase: "toMerchant" as RoutePhase,
      userDistanceKm: kmToU,
      merchantDistanceKm: kmToM,
      routeKm
    };
  }
  const backendSt = Number(summary.status || 0);
  if (backendSt === 50) {
    return {
      x: user.x,
      y: user.y,
      phase: "done" as RoutePhase,
      userDistanceKm: 0,
      merchantDistanceKm: 0,
      routeKm
    };
  }
  if (backendSt === 70 && isDeliveryTimeout(summary)) {
    return {
      x: user.x,
      y: user.y,
      phase: "timeout" as RoutePhase,
      userDistanceKm: 0.2,
      merchantDistanceKm: 0,
      routeKm
    };
  }

  if (backendSt < DELIVERY_TASK_STATUS_ASSIGNED) {
    return {
      x: riderStart.x,
      y: riderStart.y,
      phase: "waitingDispatch" as RoutePhase,
      userDistanceKm: kmToU,
      merchantDistanceKm: kmToM,
      routeKm
    };
  }

  const ctx = getTimelineContext(orderId);
  const st = backendSt;
  const elapsed = ctx?.elapsed ?? 0;
  const leg1 = ctx?.leg1 ?? 1;
  const leg2 = ctx?.leg2 ?? 1;
  const leg3 = ctx?.leg3 ?? 1;
  const totalMs = ctx?.totalMs ?? 45_000;
  const shipStart = leg1 + leg2 + leg3;
  const toUserMs = Math.max(1, totalMs - shipStart);

  let phase: RoutePhase = "toMerchant";
  let x = riderStart.x;
  let y = riderStart.y;

  const distRiderMerchant = (px: number, py: number) => Math.hypot(px - merchant.x, py - merchant.y);
  const distRiderUser = (px: number, py: number) => Math.hypot(px - user.x, py - user.y);

  if (st >= DELIVERY_TASK_STATUS_ASSIGNED && st < 25) {
    const p = leg1 > 0 ? Math.min(1, elapsed / leg1) : 1;
    x = riderStart.x + (merchant.x - riderStart.x) * p;
    y = riderStart.y + (merchant.y - riderStart.y) * p;
    phase = "toMerchant";
  } else if (st === 25) {
    x = merchant.x;
    y = merchant.y;
    phase = "atMerchant";
  } else if (st === 30) {
    x = merchant.x;
    y = merchant.y;
    phase = "pickedAtStore";
  } else if (st >= 40 && st < 50) {
    const e2 = Math.max(0, elapsed - shipStart);
    const p = toUserMs > 0 ? Math.min(1, e2 / toUserMs) : 1;
    x = merchant.x + (user.x - merchant.x) * p;
    y = merchant.y + (user.y - merchant.y) * p;
    phase = "toUser";
  } else {
    const p = leg1 > 0 ? Math.min(1, elapsed / leg1) : 1;
    x = riderStart.x + (merchant.x - riderStart.x) * p;
    y = riderStart.y + (merchant.y - riderStart.y) * p;
    phase = "toMerchant";
  }

  const dM = distRiderMerchant(x, y);
  const dU = distRiderUser(x, y);
  const merchantDistanceKm = (dM / startDist) * kmToM;
  let userDistanceKm = (dU / legUserLen) * kmToU;
  if (!Number.isFinite(userDistanceKm)) {
    userDistanceKm = kmToU;
  }

  return { x, y, phase, userDistanceKm, merchantDistanceKm, routeKm };
}

function simulatedDistanceKm(orderId: number) {
  const snap = routeSnapshot(orderId);
  if (snap.phase === "waitingDispatch") return 0;
  if (snap.phase === "done") return 0;
  if (snap.phase === "timeout") return Math.max(0.1, snap.userDistanceKm);
  if (
    snap.phase === "toMerchant" ||
    snap.phase === "atMerchant" ||
    snap.phase === "pickedAtStore"
  ) {
    const km = snap.merchantDistanceKm;
    // 去店途中数值过小时仍显示约 0.1，避免误显示 0.0（与「距商家」语义一致）
    if (snap.phase === "toMerchant" && km < 0.08) {
      return Math.max(0.1, km);
    }
    return km < 0.02 ? 0 : Math.max(0.03, km);
  }
  const km = snap.userDistanceKm;
  return km < 0.02 ? 0 : Math.max(0.03, km);
}

function distanceLabel(orderId: number) {
  const snap = routeSnapshot(orderId);
  if (snap.phase === "waitingDispatch") {
    return "骑手位置";
  }
  if (
    snap.phase === "toMerchant" ||
    snap.phase === "atMerchant" ||
    snap.phase === "pickedAtStore"
  ) {
    return "距商家";
  }
  return "与你的距离";
}

function distanceText(orderId: number) {
  const s = deliveryByOrderId.value[orderId];
  if (!s) return "—";
  if (routeSnapshot(orderId).phase === "waitingDispatch") {
    return "派单后更新";
  }
  if (deliveryTaskStatus(orderId) === 50) return "0.0 km（已送达）";
  const km = simulatedDistanceKm(orderId);
  if (km <= 0) return "0.0 km";
  if (s.status === 70) return `${km.toFixed(1)} km（配送超时）`;
  return `${km.toFixed(1)} km`;
}

function deliveryProgressPercent(orderId: number) {
  const s = deliveryByOrderId.value[orderId];
  if (!s) return 0;
  if (!isDeliveryAssigned(orderId)) return 0;
  if (deliveryTaskStatus(orderId) === 50) return 100;
  if (s.status === 70) return 100;
  const order = orders.value.find((o) => o.id === orderId);
  const c = parseTime(order?.createdAt || "");
  const eta = s.expectedArriveAt ? parseTime(s.expectedArriveAt) : NaN;
  const totalMs =
    Number.isFinite(c) && Number.isFinite(eta) && eta > c ? eta - c : 45_000;
  const er = totalMs > 0 ? Math.min(1, Math.max(0, (Date.now() - c) / totalMs)) : 0;
  return Math.max(3, Math.min(99, er * 100));
}

function riderPinStyle(orderId: number) {
  const snap = routeSnapshot(orderId);
  return {
    left: `${snap.x}%`,
    top: `${snap.y}%`,
    transform: "translate(-50%, -50%)"
  };
}

function riderAvatarText(orderId: number) {
  const name = deliveryByOrderId.value[orderId]?.riderName || "骑手";
  return name.slice(0, 1);
}

function riderArrowStyle(orderId: number) {
  const latest = trackByOrderId.value[orderId]?.[trackByOrderId.value[orderId].length - 1];
  const heading = Number(latest?.heading ?? 45);
  const safeHeading = Number.isFinite(heading) ? heading : 45;
  return { transform: `rotate(${safeHeading}deg)` };
}

function formatHm(raw?: string | null) {
  if (!raw) return "—";
  const t = parseTime(raw);
  if (Number.isNaN(t)) return "—";
  const d = new Date(t);
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function formatHmFromMs(ms: number) {
  if (!Number.isFinite(ms)) return "—";
  const d = new Date(ms);
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

/** 与 deliveryDemo 阶段毫秒一致 */
function simulatedStageMs(orderId: number) {
  const order = orders.value.find((o) => o.id === orderId);
  const c = parseTime(order?.createdAt || "");
  const ctx = getTimelineContext(orderId);
  if (!ctx || Number.isNaN(c)) {
    return { arriveMs: NaN, pickupMs: NaN, shipMs: NaN };
  }
  const { leg1, leg2, leg3 } = ctx;
  return {
    arriveMs: c + leg1,
    pickupMs: c + leg1 + leg2,
    shipMs: c + leg1 + leg2 + leg3
  };
}

/**
 * 阶段展示时间不得晚于「现在」，也不得早于「下单」。
 */
function stageDisplayTimes(orderId: number) {
  tick.value;
  const order = orders.value.find((o) => o.id === orderId);
  const c = parseTime(order?.createdAt || "");
  const now = Date.now();
  const sim = simulatedStageMs(orderId);
  if (Number.isNaN(c)) {
    return { arriveMs: now, pickupMs: now, shipMs: now };
  }
  const aRaw = Number.isFinite(sim.arriveMs) ? sim.arriveMs : c + 60_000;
  const pRaw = Number.isFinite(sim.pickupMs) ? sim.pickupMs : aRaw + 30_000;
  const sRaw = Number.isFinite(sim.shipMs) ? sim.shipMs : pRaw + 30_000;
  const arriveMs = Math.min(Math.max(aRaw, c), now);
  const pickupMs = Math.min(Math.max(pRaw, arriveMs), now);
  const shipMs = Math.min(Math.max(sRaw, pickupMs), now);
  return { arriveMs, pickupMs, shipMs };
}

function formatHmTrackOrSim(
  trackRaw: string | undefined,
  simMs: number,
  orderCreatedMs: number
) {
  if (trackRaw) {
    const t = parseTime(trackRaw);
    if (!Number.isNaN(t)) {
      const capped = Math.min(Math.max(t, orderCreatedMs), Date.now());
      return formatHmFromMs(capped);
    }
  }
  return formatHmFromMs(Math.min(Math.max(simMs, orderCreatedMs), Date.now()));
}

function stageTimeText(orderId: number, key: string) {
  const summary = deliveryByOrderId.value[orderId];
  const track = trackByOrderId.value[orderId] || [];
  const order = orders.value.find((o) => o.id === orderId);
  const lastTrackRaw = track.length ? track[track.length - 1]?.createdAt : undefined;
  const orderCreatedMs = parseTime(order?.createdAt || "");
  const { arriveMs, pickupMs, shipMs } = stageDisplayTimes(orderId);
  const etaTime = formatHm(summary?.expectedArriveAt);
  const ocm = Number.isNaN(orderCreatedMs) ? 0 : orderCreatedMs;
  const st = deliveryTaskStatus(orderId);
  if (key === "accept") return formatHm(order?.createdAt);
  if (key === "arrive") {
    if (!summary || st < 25) return "待更新";
    return formatHmTrackOrSim(lastTrackRaw, arriveMs, ocm);
  }
  if (key === "pickup") {
    if (!summary || st < 30) return "待更新";
    return formatHmTrackOrSim(lastTrackRaw, pickupMs, ocm);
  }
  if (key === "shipping") {
    if (!summary || st < 40) return "待更新";
    return formatHmTrackOrSim(lastTrackRaw, shipMs, ocm);
  }
  if (key === "finish") {
    if (Number(summary?.status) === 50) {
      const ocm = Number.isNaN(orderCreatedMs) ? 0 : orderCreatedMs;
      if (lastTrackRaw) {
        return formatHmTrackOrSim(lastTrackRaw, Date.now(), ocm);
      }
      const etaMs = parseTime(summary?.expectedArriveAt || "");
      const fallbackMs = Number.isNaN(etaMs) ? Date.now() : Math.min(etaMs, Date.now());
      return formatHmFromMs(Math.max(fallbackMs, ocm));
    }
    if (Number(summary?.status) === 70) return `${etaTime} 超时`;
    return "待送达";
  }
  return "—";
}

function deliveryTimeline(orderId: number) {
  const summary = deliveryByOrderId.value[orderId];
  const st = deliveryTaskStatus(orderId);
  const snap = routeSnapshot(orderId);
  const ctx = getTimelineContext(orderId);
  const el = ctx?.elapsed ?? 0;
  const l1 = ctx?.leg1 ?? 1;
  const timeout = st === 70;
  const delivered = st === 50;
  const ridingToMerchant =
    ctx != null && st === 20 && snap.phase === "toMerchant" && el < l1;
  const stages = [
    {
      key: "accept",
      label: "已接单",
      pass: st >= 20 || delivered || timeout,
      current: ridingToMerchant
    },
    {
      key: "arrive",
      label: "已到店",
      pass: st >= 25 || delivered || timeout,
      current: st === 25 && snap.phase === "atMerchant"
    },
    {
      key: "pickup",
      label: "已取餐",
      pass: st >= 30 || delivered || timeout,
      current: st === 30 && snap.phase === "pickedAtStore"
    },
    {
      key: "shipping",
      label: "配送中",
      pass: st >= 40 || delivered || timeout,
      current: st === 40 && snap.phase === "toUser"
    },
    {
      key: "finish",
      label: timeout ? "配送超时" : "已送达",
      pass: delivered || timeout,
      current: delivered || timeout
    }
  ];
  return stages.map((s) => ({
    key: s.key,
    label: s.label,
    done: s.pass,
    current: s.current,
    timeText: stageTimeText(orderId, s.key)
  }));
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
      const err = e as { code?: string; message?: string };
      const isTimeout =
        err?.code === "ECONNABORTED" ||
        /timeout of \d+ms exceeded/i.test(String(err?.message || ""));
      if (!isTimeout) {
        showToast("error", getApiErrorMessage(e, "订单列表更新失败"));
      }
    } else {
      error.value = getApiErrorMessage(e, "加载失败");
    }
    return;
  } finally {
    if (!silent) loading.value = false;
  }
  try {
    await refreshDeliverySummary();
  } catch {
    if (silent) {
      showToast("error", "配送信息刷新失败，可稍后下拉或刷新页面");
    }
  }
}

async function refreshDeliverySummary() {
  const currentOrderIds = new Set(orders.value.map((o) => o.id));
  for (const key of Object.keys(unsubscribeByOrderId.value)) {
    const orderId = Number(key);
    if (!currentOrderIds.has(orderId)) {
      unsubscribeByOrderId.value[orderId]?.();
      delete unsubscribeByOrderId.value[orderId];
      delete deliveryByOrderId.value[orderId];
      delete trackByOrderId.value[orderId];
      delete expandedDelivery.value[orderId];
    }
  }
  for (const o of orders.value) {
    if (orderStatus(o) === ORDER_STATUS_PENDING || orderStatus(o) === ORDER_STATUS_CANCELLED) {
      continue;
    }
    try {
      deliveryByOrderId.value[o.id] = await fetchDeliverySummary(o.id);
      trackByOrderId.value[o.id] = await fetchDeliveryTrack(o.id, 60);
      if (expandedDelivery.value[o.id]) {
        ensureDeliverySse(o.id);
      }
    } catch {
      // 未创建配送任务时忽略
    }
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
  for (const stop of Object.values(unsubscribeByOrderId.value)) stop();
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
.order-delivery-box {
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 8px;
  background: #eef2ff;
}
.order-delivery-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}
.order-delivery-line + .order-delivery-line {
  margin-top: 4px;
}
.order-delivery-line--mono {
  font-variant-numeric: tabular-nums;
}
.order-delivery-line--danger span:last-child {
  color: #b91c1c;
  font-weight: 700;
}
.order-delivery-toggle {
  margin-top: 8px;
  border: none;
  background: #3730a3;
  color: #fff;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
}
.order-delivery-detail {
  margin-top: 8px;
  border-top: 1px dashed #c7d2fe;
  padding-top: 8px;
}
.order-map {
  position: relative;
  height: 120px;
  border: 1px solid #c7d2fe;
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
  margin-bottom: 8px;
  overflow: hidden;
}
.order-map-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(to right, rgba(99, 102, 241, 0.12) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(99, 102, 241, 0.12) 1px, transparent 1px);
  background-size: 24px 24px;
}
.order-route-line {
  position: absolute;
  left: 14%;
  top: 20%;
  width: 70%;
  height: 62%;
  border: 2px dashed #94a3b8;
  border-radius: 999px;
  transform: rotate(10deg);
  opacity: 0.7;
}
.order-map-label {
  position: absolute;
  z-index: 2;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #475569;
  border: 1px solid #cbd5e1;
}
/* 位置由 mapPinStyle 与模拟坐标一致 */
.order-map-label--start {
  z-index: 2;
}
.order-map-waiting {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 2;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #c7d2fe;
  font-size: 12px;
  font-weight: 600;
  color: #4338ca;
  text-align: center;
  max-width: 88%;
}
.order-map-distance {
  position: absolute;
  right: 8px;
  top: 8px;
  z-index: 2;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #f8fafc;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.order-user-pin {
  position: absolute;
  transform: translate(-50%, -50%);
  z-index: 2;
  font-size: 11px;
  padding: 3px 6px;
  border-radius: 999px;
  font-weight: 700;
  background: #059669;
  color: #fff;
}
.order-rider-pin {
  position: absolute;
  z-index: 3;
  font-size: 11px;
  font-weight: 700;
  background: transparent;
  color: #fff;
  transition: left 0.9s linear, top 0.9s linear;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
}
.order-rider-avatar {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  border: 2px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.4);
}
.order-rider-arrow {
  color: #1d4ed8;
  font-size: 12px;
  line-height: 1;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
  transform-origin: center;
  display: inline-block;
}
.order-progress {
  margin: 8px 0;
}
.order-progress-track {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: #dbeafe;
  overflow: hidden;
}
.order-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb 0%, #16a34a 100%);
  transition: width 0.9s linear;
}
.order-progress-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 12px;
  color: #4b5563;
}
.order-delivery-timeline {
  margin-top: 10px;
  border-top: 1px dashed #c7d2fe;
  padding-top: 8px;
  display: grid;
  gap: 6px;
}
.order-delivery-step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 13px;
}
.order-delivery-step-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #cbd5e1;
  flex-shrink: 0;
}
.order-delivery-step.is-done .order-delivery-step-dot {
  background: #22c55e;
}
.order-delivery-step.is-current .order-delivery-step-dot {
  background: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.18);
}
.order-delivery-step.is-current .order-delivery-step-label {
  color: #1d4ed8;
  font-weight: 700;
}
.order-delivery-step-label {
  flex: 1;
}
.order-delivery-step-time {
  font-size: 12px;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}
.order-delivery-step.is-current .order-delivery-step-time {
  color: #1d4ed8;
  font-weight: 600;
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
