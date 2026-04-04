<template>
  <div class="home">
    <section class="home-hero card" aria-label="搜索与排序">
      <p v-if="auth.token && auth.userInfo" class="welcome">
        {{ greetingPrefix }}，{{ auth.userInfo.nickname || auth.userInfo.mobile }}
      </p>
      <p v-else class="welcome muted">
        登录后可同步订单与地址；不登录也能先逛逛。
      </p>
      <div class="home-search-field">
        <form
          class="home-search-form"
          role="search"
          :aria-busy="loading"
          @submit.prevent="search"
        >
          <div class="search-row">
            <input
              id="home-merchant-search"
              v-model="keyword"
              type="search"
              name="merchantKeyword"
              placeholder="输入商家名称"
              class="search-input"
              aria-label="搜索商家名称"
              autocomplete="off"
              inputmode="search"
              spellcheck="false"
            />
            <button
              type="submit"
              class="btn-secondary home-search-submit"
              :disabled="loading"
              :aria-busy="loading"
            >
              搜索
            </button>
          </div>
        </form>
      </div>
      <div
        ref="sortGroupRef"
        class="home-sort"
        role="radiogroup"
        aria-label="排序方式"
        @keydown="onSortGroupKeydown"
      >
        <button
          v-for="opt in sortOptions"
          :key="opt.value"
          type="button"
          class="home-sort-btn"
          :class="{
            active: sortMode === opt.value,
            'home-sort-btn--delight': sortDelight === opt.value
          }"
          role="radio"
          :aria-checked="sortMode === opt.value"
          :tabindex="sortMode === opt.value ? 0 : -1"
          :data-sort-value="opt.value"
          @click="setSort(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
    </section>

    <section
      class="home-merchants"
      aria-labelledby="home-merchants-heading"
      :aria-busy="loading"
    >
      <h2 id="home-merchants-heading" class="home-section-title">商家列表</h2>

      <div
        v-if="error || loading"
        role="status"
        aria-live="polite"
        aria-atomic="true"
        class="home-status"
        :aria-busy="loading && !error"
      >
        <p v-if="error" class="error-text">{{ error }}</p>
        <p v-else class="muted home-loading" aria-label="正在加载商家列表">
          <span class="home-loading-label">加载中</span>
          <span class="home-loading-dots" aria-hidden="true">
            <span class="home-loading-dot"></span>
            <span class="home-loading-dot"></span>
            <span class="home-loading-dot"></span>
          </span>
        </p>
      </div>

      <ul v-if="!loading && !error" class="merchant-grid">
        <li
          v-for="(m, idx) in records"
          :key="m.id"
          class="merchant-card"
          :style="{ animationDelay: `${Math.min(idx * LIST_ANIM_STEP_MS, LIST_ANIM_MAX_MS)}ms` }"
        >
          <RouterLink
            :to="`/merchants/${m.id}`"
            class="merchant-card-link"
            :aria-label="`${m.name}，评分 ${formatRating(m.rating)}，进入店铺`"
          >
            <div class="merchant-card-body">
              <h3>{{ m.name }}</h3>
              <p class="meta">
                <span>评分 {{ formatRating(m.rating) }}</span>
                <span>月售 {{ m.monthlySales }}</span>
                <span v-if="m.distanceKm != null" class="muted"
                  >距您约 {{ formatKm(m.distanceKm) }} km</span
                >
              </p>
              <p class="fee">
                起送 ¥{{ formatMoney(m.minDeliveryAmount) }} · 配送 ¥{{
                  formatMoney(m.deliveryFee)
                }}
              </p>
            </div>
          </RouterLink>
        </li>
      </ul>

      <p v-if="!loading && !error && records.length === 0" class="home-empty muted">
        {{ emptyMessage }}
      </p>

      <nav
        v-if="!loading && !error && records.length > 0 && totalPages > 1"
        class="home-pagination"
        aria-label="分页"
      >
        <button
          type="button"
          class="btn-secondary home-page-btn"
          :disabled="pageNo <= 1 || loading"
          aria-label="上一页"
          @click="goPage(pageNo - 1)"
        >
          上一页
        </button>
        <span class="home-page-info">第 {{ pageNo }} / {{ totalPages }} 页 · 共 {{ total }} 家</span>
        <button
          type="button"
          class="btn-secondary home-page-btn"
          :disabled="pageNo >= totalPages || loading"
          aria-label="下一页"
          @click="goPage(pageNo + 1)"
        >
          下一页
        </button>
      </nav>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import { fetchMerchantPage, type MerchantSortMode } from "../../api/merchant";
import type { MerchantListItem } from "../../types/merchant";
import { useAuthStore } from "../../store/auth";
import { getApiErrorMessage } from "../../utils/apiError";

const auth = useAuthStore();
const keyword = ref("");
const loading = ref(false);
const error = ref("");
const records = ref<MerchantListItem[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = 10;

const sortMode = ref<MerchantSortMode>("recommend");
const sortGroupRef = ref<HTMLElement | null>(null);
/** 仅在用户切换排序时触发一次弹跳，避免首屏加载误触 */
const sortDelight = ref<MerchantSortMode | null>(null);
let sortDelightTimer: number | undefined;

const LIST_ANIM_STEP_MS = 30;
const LIST_ANIM_MAX_MS = 300;

function formatRating(n: number) {
  return Number(n).toFixed(1);
}

function formatMoney(n: number) {
  return Number(n).toFixed(2);
}

function formatKm(n: number) {
  return Number(n).toFixed(1);
}

const sortOptions: { value: MerchantSortMode; label: string }[] = [
  { value: "recommend", label: "推荐" },
  { value: "distance", label: "距离" },
  { value: "rating", label: "评分" },
  { value: "sales", label: "月售" }
];

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const greetingPrefix = computed(() => {
  const h = new Date().getHours();
  if (h >= 0 && h < 5) return "夜深了";
  if (h < 11) return "早上好";
  if (h < 13) return "中午好";
  if (h < 18) return "下午好";
  return "晚上好";
});

const emptyMessage = computed(() => {
  const kw = keyword.value.trim();
  if (kw) {
    return `没有找到与「${kw}」相关的商家。换个关键词，或清空搜索栏再试试。`;
  }
  if (sortMode.value !== "recommend") {
    return "当前排序下暂无商家，换个排序或稍后再来看看。";
  }
  return "暂无商家。若刚部署环境，请确认数据库迁移已执行。";
});

function focusSortValue(mode: MerchantSortMode) {
  nextTick(() => {
    const root = sortGroupRef.value;
    if (!root) return;
    const btn = root.querySelector<HTMLButtonElement>(`[data-sort-value="${mode}"]`);
    btn?.focus();
  });
}

function setSort(mode: MerchantSortMode) {
  if (loading.value) return;
  if (sortMode.value === mode) return;
  sortMode.value = mode;
  pageNo.value = 1;
  if (sortDelightTimer) window.clearTimeout(sortDelightTimer);
  sortDelight.value = mode;
  sortDelightTimer = window.setTimeout(() => {
    sortDelight.value = null;
  }, 600);
  load();
  focusSortValue(mode);
}

function onSortGroupKeydown(e: KeyboardEvent) {
  const t = e.target as HTMLButtonElement;
  if (t.getAttribute("role") !== "radio") return;
  const val = t.dataset.sortValue as MerchantSortMode | undefined;
  if (!val) return;

  const idx = sortOptions.findIndex((o) => o.value === val);
  if (idx < 0) return;

  const move = (delta: number) => {
    e.preventDefault();
    const n = (idx + delta + sortOptions.length) % sortOptions.length;
    setSort(sortOptions[n]!.value);
  };

  if (e.key === "ArrowRight" || e.key === "ArrowDown") {
    move(1);
  } else if (e.key === "ArrowLeft" || e.key === "ArrowUp") {
    move(-1);
  } else if (e.key === "Home") {
    e.preventDefault();
    setSort(sortOptions[0]!.value);
  } else if (e.key === "End") {
    e.preventDefault();
    setSort(sortOptions[sortOptions.length - 1]!.value);
  }
}

function goPage(n: number) {
  if (loading.value) return;
  if (n < 1 || n > totalPages.value) return;
  pageNo.value = n;
  load();
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const data = await fetchMerchantPage({
      keyword: keyword.value.trim() || undefined,
      pageNo: pageNo.value,
      pageSize,
      sort: sortMode.value
    });
    records.value = data.records;
    total.value = data.total;
    if (typeof data.pageNo === "number" && data.pageNo >= 1) {
      pageNo.value = data.pageNo;
    }
  } catch (e: unknown) {
    error.value = getApiErrorMessage(e, "加载失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  if (loading.value) return;
  pageNo.value = 1;
  load();
}

onMounted(() => {
  load();
});

onUnmounted(() => {
  if (sortDelightTimer) window.clearTimeout(sortDelightTimer);
});
</script>

<style scoped>
@keyframes home-hero-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes home-fade-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes home-hero-accent-pulse {
  from {
    opacity: 0.88;
  }
  to {
    opacity: 0.98;
  }
}

@keyframes home-bar-shimmer {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 200% 50%;
  }
}

@keyframes home-orb-drift {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 0.68;
  }
  100% {
    transform: translate(10px, -8px) scale(1.06);
    opacity: 0.88;
  }
}

@keyframes home-shine-sweep {
  from {
    left: -120%;
    opacity: 0.85;
  }
  to {
    left: 160%;
    opacity: 0;
  }
}

.home-hero.card {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(37, 99, 235, 0.3);
  animation: home-hero-in 0.52s var(--ease-out-expo) both;
  background:
    radial-gradient(ellipse 110% 85% at 100% -6%, rgba(59, 130, 246, 0.22), transparent 52%),
    radial-gradient(ellipse 95% 75% at -8% 110%, rgba(251, 146, 60, 0.12), transparent 55%),
    linear-gradient(165deg, #ffffff 0%, #f1f5f9 38%, #e0f2fe 70%, #fff7ed 100%);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.58) inset,
    0 1px 2px rgba(28, 25, 23, 0.05),
    0 8px 28px rgba(37, 99, 235, 0.12),
    0 20px 56px rgba(14, 116, 233, 0.13),
    0 0 80px rgba(37, 99, 235, 0.07);
  transition:
    box-shadow var(--duration-normal) var(--ease-out),
    border-color var(--duration-normal) var(--ease-out);
}

.home-hero.card > * {
  position: relative;
  z-index: 1;
}

.home-hero.card > *:nth-child(1) {
  animation: home-fade-up 0.44s var(--ease-out-expo) 0.08s both;
}

.home-hero.card > *:nth-child(2) {
  animation: home-fade-up 0.44s var(--ease-out-expo) 0.14s both;
}

.home-hero.card > *:nth-child(3) {
  animation: home-fade-up 0.44s var(--ease-out-expo) 0.2s both;
}

.home-hero.card::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  z-index: 2;
  height: 6px;
  background: linear-gradient(
    90deg,
    #1e40af 0%,
    #2563eb 18%,
    #0ea5e9 42%,
    #f97316 72%,
    #1d4ed8 100%
  );
  background-size: 200% 100%;
  opacity: 0.97;
  animation:
    home-hero-accent-pulse 5s ease-in-out infinite alternate,
    home-bar-shimmer 7s linear infinite;
}

.home-hero.card::after {
  content: "";
  position: absolute;
  pointer-events: none;
  right: -12%;
  top: 14%;
  z-index: 0;
  width: min(320px, 55vw);
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(
    circle at 40% 40%,
    rgba(96, 165, 250, 0.32) 0%,
    rgba(37, 99, 235, 0.12) 42%,
    transparent 68%
  );
  filter: blur(0.5px);
  animation: home-orb-drift 18s ease-in-out infinite alternate;
}

.home-hero.card:focus-within {
  border-color: rgba(37, 99, 235, 0.48);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.62) inset,
    0 1px 2px rgba(28, 25, 23, 0.06),
    0 12px 36px rgba(37, 99, 235, 0.18),
    0 24px 64px rgba(14, 165, 233, 0.18),
    0 0 100px rgba(37, 99, 235, 0.09);
}

.home-search-field {
  margin-top: 8px;
}

.home-search-form {
  margin: 0;
}

.home .search-input {
  border-color: rgba(37, 99, 235, 0.2);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 1px 2px rgba(37, 99, 235, 0.06);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out);
}

.home .search-input:hover:not(:disabled) {
  border-color: rgba(37, 99, 235, 0.32);
}

.home .search-input:focus-visible {
  outline: none;
  border-color: rgba(37, 99, 235, 0.75);
  background: #fff;
  box-shadow:
    inset 0 1px 2px rgba(37, 99, 235, 0.06),
    0 0 0 3px rgba(37, 99, 235, 0.24),
    0 0 28px rgba(14, 165, 233, 0.2);
}

.home-search-submit {
  position: relative;
  overflow: hidden;
}

.home-search-submit:not(:disabled) {
  background: linear-gradient(
    135deg,
    #1e3a8a 0%,
    #1d4ed8 22%,
    #2563eb 48%,
    #0ea5e9 78%,
    #38bdf8 100%
  );
  background-size: 160% 160%;
  color: #fff;
  border: none;
  box-shadow:
    0 3px 12px rgba(30, 58, 138, 0.42),
    0 8px 26px rgba(37, 99, 235, 0.36),
    0 0 36px rgba(14, 165, 233, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.28),
    inset 0 -1px 0 rgba(15, 23, 42, 0.1);
  transition:
    filter var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out),
    background-position var(--duration-normal) var(--ease-out);
}

.home-search-submit::before {
  content: "";
  position: absolute;
  top: -20%;
  bottom: -20%;
  left: -100%;
  width: 55%;
  background: linear-gradient(
    105deg,
    transparent 0%,
    rgba(255, 255, 255, 0.45) 45%,
    transparent 100%
  );
  transform: skewX(-18deg);
  pointer-events: none;
  opacity: 0;
}

.home-search-submit:hover:not(:disabled) {
  filter: brightness(1.08) saturate(1.05);
  transform: translateY(-2px);
  background-position: 88% 50%;
  box-shadow:
    0 6px 16px rgba(30, 58, 138, 0.4),
    0 14px 38px rgba(37, 99, 235, 0.45),
    0 0 48px rgba(14, 165, 233, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.32),
    inset 0 -1px 0 rgba(15, 23, 42, 0.08);
}

.home-search-submit:hover:not(:disabled)::before {
  animation: home-shine-sweep 0.75s var(--ease-out-expo) forwards;
}

.home-search-submit:active:not(:disabled) {
  transform: translateY(0) scale(0.992);
  filter: brightness(1.02);
}

.home-search-submit:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(14, 165, 233, 0.55),
    0 0 0 6px rgba(37, 99, 235, 0.16),
    0 10px 34px rgba(37, 99, 235, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.home-search-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  filter: grayscale(0.15);
  transform: none;
  box-shadow: none;
}

.home-section-title {
  margin: 20px 0 6px;
  font-size: clamp(1.125rem, 2.4vw, 1.25rem);
  font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1.28;
  background: linear-gradient(118deg, #1e3a8a 0%, #2563eb 28%, #0ea5e9 55%, #ea580c 88%);
  background-size: 140% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 3px 18px rgba(37, 99, 235, 0.22));
  animation: home-fade-up 0.44s var(--ease-out-expo) 0.28s both;
}

.home-status {
  animation: home-fade-up 0.4s var(--ease-out-expo) 0.32s both;
}

.home-status p {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.55;
}

.home-merchants .merchant-grid {
  animation: home-fade-up 0.45s var(--ease-out-expo) both;
}

.home-empty {
  margin: 14px 0 0;
  max-width: 36rem;
  font-size: 14px;
  line-height: 1.6;
  animation: home-fade-up 0.42s var(--ease-out-expo) both;
}

.home-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(37, 99, 235, 0.18);
  box-shadow: 0 -1px 0 rgba(255, 255, 255, 0.7);
  animation: home-fade-up 0.42s var(--ease-out-expo) both;
}

.home-page-btn {
  min-width: 5.5rem;
  font-size: 14px;
  line-height: 1.35;
  background: var(--color-surface);
  color: var(--color-primary-strong);
  border: 1px solid rgba(37, 99, 235, 0.48);
  box-shadow:
    0 1px 2px rgba(28, 25, 23, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
  transition:
    background var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);
}

.home-page-btn:hover:not(:disabled) {
  background: var(--color-primary-soft);
  border-color: rgba(37, 99, 235, 0.62);
  color: var(--color-primary-strong);
  transform: translateY(-1px);
  box-shadow:
    0 4px 16px rgba(37, 99, 235, 0.2),
    0 0 24px rgba(14, 165, 233, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.home-page-btn:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(14, 165, 233, 0.5),
    0 0 0 6px rgba(37, 99, 235, 0.12),
    0 4px 16px rgba(37, 99, 235, 0.18);
}

.home-page-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.99);
}

.home-page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  background: var(--color-surface);
  transform: none;
  box-shadow: none;
}

.home-page-info {
  font-size: 13px;
  line-height: 1.45;
  letter-spacing: 0.02em;
  font-variant-numeric: tabular-nums;
  color: #475569;
}

.home-sort {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(37, 99, 235, 0.16);
}
.home-sort-btn {
  padding: 7px 16px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: 0.02em;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}
.home-sort-btn:hover {
  border-color: rgba(37, 99, 235, 0.35);
  background: rgba(37, 99, 235, 0.06);
}
.home-sort-btn:focus {
  outline: none;
}
.home-sort-btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
.home-sort-btn:active {
  transform: scale(0.97);
}

@keyframes home-sort-delight {
  0% {
    transform: scale(0.94);
  }
  55% {
    transform: scale(1.04);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes home-dot-dance {
  0%,
  70%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  35% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.home-sort-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-strong);
  box-shadow:
    inset 0 0 0 1px rgba(37, 99, 235, 0.18),
    0 0 22px rgba(37, 99, 235, 0.14);
}

.home-sort-btn.home-sort-btn--delight {
  animation: home-sort-delight 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.home-loading {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0;
  margin: 0;
}

.home-loading-label {
  margin-right: 1px;
}

.home-loading-dots {
  display: inline-flex;
  gap: 1px;
}

.home-loading-dot {
  display: inline-block;
  width: 0.35em;
  text-align: center;
  font-weight: 700;
  animation: home-dot-dance 1.1s ease-in-out infinite;
}

.home-loading-dot:nth-child(1) {
  animation-delay: 0s;
}

.home-loading-dot:nth-child(2) {
  animation-delay: 0.14s;
}

.home-loading-dot:nth-child(3) {
  animation-delay: 0.28s;
}

.home-loading-dot::before {
  content: ".";
}

@media (prefers-reduced-motion: reduce) {
  .home-hero.card,
  .home-hero.card > *:nth-child(1),
  .home-hero.card > *:nth-child(2),
  .home-hero.card > *:nth-child(3),
  .home-section-title,
  .home-status,
  .home-merchants .merchant-grid,
  .home-empty,
  .home-pagination {
    animation: none !important;
  }

  .home-hero.card {
    transition: none;
  }

  .home-hero.card::before {
    animation: none;
    background-size: 100% 100%;
  }

  .home-hero.card::after {
    animation: none;
  }

  .home .search-input {
    transition: none;
  }

  .home-search-submit::before {
    animation: none !important;
  }

  .home-search-submit:hover:not(:disabled) {
    background-position: 50% 50%;
    transform: none;
  }

  .home-search-submit:active:not(:disabled) {
    transform: none;
  }

  .home-page-btn {
    transition: none;
  }

  .home-page-btn:hover:not(:disabled),
  .home-page-btn:active:not(:disabled) {
    transform: none;
  }

  .home-sort-btn {
    transition: none;
  }

  .home-sort-btn:active {
    transform: none;
  }

  .home-sort-btn.home-sort-btn--delight {
    animation: none !important;
  }

  .home-loading-dot {
    animation: none !important;
    transform: none;
    opacity: 1;
  }
}
</style>
