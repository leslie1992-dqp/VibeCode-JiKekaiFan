<template>
  <div class="coupons-view">
    <RouterLink to="/" class="coupons-back">
      <span class="coupons-back-icon" aria-hidden="true">←</span>
      返回首页
    </RouterLink>

    <header class="coupons-hero" aria-labelledby="coupons-heading">
      <div class="coupons-hero-glow" aria-hidden="true" />
      <h2 id="coupons-heading" class="coupons-hero-title">我的优惠券</h2>
      <p v-if="auth.userInfo" class="coupons-hero-sub">
        {{ auth.userInfo.nickname || auth.userInfo.mobile }} · 下单时自动择优抵扣
      </p>
      <p v-else class="coupons-hero-sub muted">登录后领取的券会显示在这里</p>
    </header>

    <p v-if="error" class="error-text coupons-error" role="alert">{{ error }}</p>

    <div
      v-else-if="loading"
      class="coupons-loading card"
      aria-busy="true"
      aria-label="正在加载优惠券"
    >
      <span class="coupons-loading-label">加载中</span>
      <span class="coupons-loading-dots" aria-hidden="true">
        <span class="coupons-loading-dot" />
        <span class="coupons-loading-dot" />
        <span class="coupons-loading-dot" />
      </span>
    </div>

    <ul
      v-else-if="items.length"
      class="coupons-list"
      aria-label="优惠券列表"
    >
      <li
        v-for="(c, idx) in items"
        :key="c.id"
        class="coupons-card"
        :class="{ 'coupons-card--used': c.status === 2, 'coupons-card--lock': c.status === 3 }"
        :style="{ animationDelay: `${Math.min(idx * 55, 500)}ms` }"
      >
        <div class="coupons-card__left" aria-hidden="true">
          <span class="coupons-card__label">立减</span>
          <span class="coupons-card__amount-wrap">
            <span class="coupons-card__currency">¥</span>
            <span class="coupons-card__amount">{{ formatMoneyInt(c.discountAmount) }}</span>
          </span>
          <span class="coupons-card__threshold">满{{ formatMoneyInt(c.thresholdAmount) }}可用</span>
        </div>
        <div class="coupons-card__tear" aria-hidden="true" />
        <div class="coupons-card__body">
          <span class="coupons-card__status" :class="statusClass(c.status)">{{
            statusText(c.status)
          }}</span>
          <p class="coupons-card__merchant">{{ c.merchantName }}</p>
          <p class="coupons-card__title">{{ c.title }}</p>
          <p class="coupons-card__rule">
            满¥{{ Number(c.thresholdAmount).toFixed(2) }}减¥{{
              Number(c.discountAmount).toFixed(2)
            }}
          </p>
          <p class="coupons-card__expire">
            <span class="coupons-card__expire-icon" aria-hidden="true">⏱</span>
            有效期至 {{ formatExpire(c.expireAt) }}
          </p>
        </div>
      </li>
    </ul>

    <div v-else class="coupons-empty card" role="status">
      <div class="coupons-empty-icon" aria-hidden="true">🎫</div>
      <p class="coupons-empty-title">暂无优惠券</p>
      <p class="coupons-empty-hint muted">
        去商家详情页抢限时秒杀券，下单更省。
      </p>
      <RouterLink to="/" class="coupons-empty-btn">去逛逛</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { fetchMyCoupons } from "../../api/userCoupon";
import { getApiErrorMessage } from "../../utils/apiError";
import { useAuthStore } from "../../store/auth";
import type { UserCouponItem } from "../../types/coupon";

const auth = useAuthStore();

const loading = ref(true);
const error = ref("");
const items = ref<UserCouponItem[]>([]);

function formatMoneyInt(n: number) {
  const x = Number(n);
  if (!Number.isFinite(x)) return "—";
  return Number.isInteger(x) ? String(x) : x.toFixed(1);
}

function formatExpire(iso: string) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function statusText(s: number) {
  if (s === 1) return "未使用";
  if (s === 2) return "已使用";
  if (s === 3) return "待支付占用";
  return "—";
}

function statusClass(s: number) {
  if (s === 1) return "coupons-card__status--ok";
  if (s === 2) return "coupons-card__status--used";
  if (s === 3) return "coupons-card__status--lock";
  return "";
}

async function load() {
  if (!auth.token) {
    items.value = [];
    loading.value = false;
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    items.value = await fetchMyCoupons();
  } catch (e: unknown) {
    error.value = getApiErrorMessage(e, "加载失败");
    items.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void load();
});

watch(
  () => auth.token,
  (t) => {
    if (t) void load();
    else {
      items.value = [];
      error.value = "";
    }
  }
);
</script>

<style scoped>
@keyframes coupons-card-in {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes coupons-hero-shift {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(6%, -4%) scale(1.08);
  }
}

@keyframes coupons-dot-pulse {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

.coupons-view {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 0 2px;
  box-sizing: border-box;
}

.coupons-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 18px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(37, 99, 235, 0.18);
  text-decoration: none;
  transition:
    background 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out),
    transform 0.2s var(--ease-out);
}

.coupons-back:hover {
  background: #fff;
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.12);
  transform: translateX(-2px);
}

.coupons-back-icon {
  transition: transform 0.2s var(--ease-out);
}

.coupons-back:hover .coupons-back-icon {
  transform: translateX(-3px);
}

.coupons-hero {
  position: relative;
  padding: 22px 22px 20px;
  margin-bottom: 22px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(37, 99, 235, 0.2);
  background: linear-gradient(
    125deg,
    rgba(239, 246, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.9) 45%,
    rgba(255, 247, 237, 0.85) 100%
  );
  box-shadow: 0 10px 36px rgba(37, 99, 235, 0.08);
  overflow: hidden;
}

.coupons-hero-glow {
  position: absolute;
  width: 140%;
  height: 140%;
  top: -60%;
  right: -40%;
  background: radial-gradient(
    circle at 30% 40%,
    rgba(59, 130, 246, 0.22) 0%,
    rgba(14, 165, 233, 0.08) 35%,
    transparent 55%
  );
  pointer-events: none;
  animation: coupons-hero-shift 14s ease-in-out infinite;
}

.coupons-hero-title {
  position: relative;
  margin: 0 0 8px;
  font-size: clamp(1.35rem, 4vw, 1.6rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.coupons-hero-sub {
  position: relative;
  margin: 0;
  font-size: 14px;
  line-height: 1.45;
  color: #475569;
}

.coupons-error {
  margin: 0 0 16px;
}

.coupons-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 28px 20px;
  margin: 0;
  max-width: none;
  width: 100%;
  box-sizing: border-box;
}

.coupons-loading-label {
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
}

.coupons-loading-dots {
  display: inline-flex;
  gap: 5px;
  align-items: center;
}

.coupons-loading-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: coupons-dot-pulse 1s ease-in-out infinite;
}

.coupons-loading-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.coupons-loading-dot:nth-child(3) {
  animation-delay: 0.3s;
}

.coupons-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.coupons-card {
  --coupons-card-radius: 16px;
  position: relative;
  display: flex;
  align-items: stretch;
  min-height: 118px;
  border-radius: var(--coupons-card-radius);
  background: #fff;
  border: 1px solid rgba(231, 229, 228, 0.95);
  box-shadow:
    0 1px 2px rgba(28, 25, 23, 0.04),
    0 8px 28px rgba(28, 25, 23, 0.06);
  overflow: hidden;
  animation: coupons-card-in 0.55s var(--ease-out-expo) backwards;
  transition:
    transform 0.28s var(--ease-out),
    box-shadow 0.28s var(--ease-out),
    border-color 0.28s var(--ease-out);
}

.coupons-card::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  opacity: 0;
  background: linear-gradient(
    105deg,
    transparent 40%,
    rgba(255, 255, 255, 0.45) 50%,
    transparent 60%
  );
  background-size: 200% 100%;
  transition: opacity 0.3s ease;
}

.coupons-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 4px 8px rgba(37, 99, 235, 0.06),
    0 16px 44px rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.22);
}

.coupons-card:hover::after {
  opacity: 1;
  animation: coupons-shine 1.2s ease-out;
}

@keyframes coupons-shine {
  from {
    background-position: 100% 0;
  }
  to {
    background-position: -100% 0;
  }
}

.coupons-card--used {
  opacity: 0.92;
}

.coupons-card--used:hover {
  box-shadow:
    0 1px 2px rgba(28, 25, 23, 0.04),
    0 8px 24px rgba(28, 25, 23, 0.05);
  border-color: rgba(231, 229, 228, 0.95);
}

.coupons-card--used .coupons-card__left {
  filter: saturate(0.65) brightness(0.98);
}

.coupons-card__left {
  flex: 0 0 118px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 10px;
  text-align: center;
  background: linear-gradient(165deg, #fff7ed 0%, #ffedd5 35%, #fdba74 90%);
  color: #9a3412;
}

.coupons-card__label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  opacity: 0.85;
  margin-bottom: 2px;
}

.coupons-card__amount-wrap {
  display: flex;
  align-items: baseline;
  gap: 1px;
  line-height: 1;
  margin: 4px 0 6px;
}

.coupons-card__currency {
  font-size: 15px;
  font-weight: 800;
}

.coupons-card__amount {
  font-size: 28px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.03em;
}

.coupons-card__threshold {
  font-size: 11px;
  font-weight: 600;
  opacity: 0.88;
  line-height: 1.25;
  max-width: 100px;
}

.coupons-card__tear {
  flex: 0 0 0;
  width: 0;
  align-self: stretch;
  margin: 14px 0;
  border-left: 2px dashed rgba(203, 213, 225, 0.95);
}

.coupons-card__body {
  flex: 1;
  min-width: 0;
  padding: 16px 18px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.coupons-card__status {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}

.coupons-card__status--ok {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  color: #047857;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.6) inset;
}

.coupons-card__status--used {
  background: #f1f5f9;
  color: #64748b;
}

.coupons-card__status--lock {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #b45309;
}

.coupons-card__merchant {
  margin: 0 0 6px;
  padding-right: 88px;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.35;
}

.coupons-card__title {
  margin: 0 0 8px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}

.coupons-card__rule {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
  color: #c2410c;
  line-height: 1.4;
}

.coupons-card__expire {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
  display: flex;
  align-items: center;
  gap: 6px;
}

.coupons-card__expire-icon {
  font-size: 13px;
  opacity: 0.75;
}

.coupons-empty {
  text-align: center;
  padding: 36px 24px 32px;
  max-width: none;
  width: 100%;
  box-sizing: border-box;
  border-style: dashed;
  border-width: 2px;
  border-color: rgba(37, 99, 235, 0.2);
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.5) 0%, #fff 70%);
  animation: coupons-card-in 0.5s var(--ease-out-expo) backwards;
}

.coupons-empty-icon {
  font-size: 2.25rem;
  line-height: 1;
  margin-bottom: 12px;
  filter: grayscale(0.15);
}

.coupons-empty-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
}

.coupons-empty-hint {
  margin: 0 auto 20px;
  font-size: 14px;
  line-height: 1.5;
  max-width: 280px;
}

.coupons-empty-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 22px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #0ea5e9);
  text-decoration: none;
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
  transition:
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out);
}

.coupons-empty-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(37, 99, 235, 0.4);
  color: #fff;
}

.coupons-empty-btn:active {
  transform: translateY(0);
}
</style>
