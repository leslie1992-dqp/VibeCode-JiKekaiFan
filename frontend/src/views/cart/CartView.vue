<template>
  <div class="cart cart-page">
    <div
      v-if="toast.visible"
      class="cart-page-toast"
      :class="toast.type"
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      {{ toast.message }}
    </div>

    <RouterLink to="/" class="cart-page-back">
      <span class="cart-page-back-icon" aria-hidden="true">←</span>
      返回首页
    </RouterLink>

    <header class="cart-page-hero" aria-labelledby="cart-page-title">
      <div class="cart-page-hero-glow" aria-hidden="true" />
      <div class="cart-page-hero-row">
        <div>
          <h2 id="cart-page-title" class="cart-page-title">购物车</h2>
          <p class="cart-page-sub muted">勾选商品后预览优惠与配送，一键结算</p>
        </div>
        <button
          v-if="!loading && !error && grouped.length > 0"
          type="button"
          class="cart-clear-all-btn"
          @click="openClearConfirm"
        >
          一键清空
        </button>
      </div>
    </header>

    <div
      v-if="!loading && !error && grouped.length > 0"
      class="cart-pay-bar card cart-page-pay"
    >
      <div class="cart-pay-bar-sum">
        <span class="muted">已选应付</span>
        <div ref="selectedPayWrap" class="cart-selected-pay-wrap">
          <div class="cart-selected-pay-main">
            <span class="cart-pay-bar-amount">{{
              previewGrandPayable != null ? "¥" + previewGrandPayable.toFixed(2) : "—"
            }}</span>
            <button
              type="button"
              class="cart-selected-pay-toggle"
              :aria-expanded="selectedPayDropdownOpen"
              aria-haspopup="true"
              @click.stop="toggleSelectedPayDropdown"
            >
              已选商品 ({{ selectedCount }})
              <span class="cart-selected-pay-caret" aria-hidden="true">{{
                selectedPayDropdownOpen ? "▲" : "▼"
              }}</span>
            </button>
          </div>
          <div
            v-show="selectedPayDropdownOpen"
            class="cart-selected-pay-dropdown card"
            role="menu"
            @click.stop
          >
            <p v-if="selectedLineItems.length === 0" class="cart-selected-pay-empty muted">
              暂无已选商品，请在下方勾选
            </p>
            <ul v-else class="cart-selected-pay-list">
              <li
                v-for="item in selectedLineItems"
                :key="item.productId"
                class="cart-selected-pay-row"
              >
                <div class="cart-selected-pay-info">
                  <span class="cart-selected-pay-name">{{ item.productName }}</span>
                  <span class="cart-selected-pay-merchant muted">{{
                    item.merchantName || `店铺 #${item.merchantId}`
                  }}</span>
                </div>
                <div class="cart-selected-pay-meta">
                  <span class="cart-selected-pay-qty">×{{ item.quantity }}</span>
                  <span class="cart-selected-pay-sub"
                    >¥{{ Number(item.subtotal).toFixed(2) }}</span
                  >
                  <button
                    type="button"
                    class="cart-selected-pay-remove"
                    @click="toggleProduct(item.productId)"
                  >
                    移除
                  </button>
                </div>
              </li>
            </ul>
            <div v-if="selectedLineItems.length > 0" class="cart-selected-pay-footer">
              <button type="button" class="cart-selected-pay-clear" @click="clearAllSelected">
                清空已选
              </button>
            </div>
          </div>
        </div>
      </div>
      <button
        type="button"
        class="cart-go-pay-btn"
        :disabled="selectedCount === 0 || previewLoading"
        @click="openCheckoutPanel"
      >
        {{ previewLoading ? "计算中…" : "去支付" }}
      </button>
    </div>

    <p v-if="error" class="error-text cart-page-error" role="alert">{{ error }}</p>

    <div
      v-else-if="loading"
      class="cart-page-loading card"
      aria-busy="true"
      aria-label="正在加载购物车"
    >
      <span class="cart-page-loading-label">加载中</span>
      <span class="cart-page-loading-dots" aria-hidden="true">
        <span class="cart-page-loading-dot" />
        <span class="cart-page-loading-dot" />
        <span class="cart-page-loading-dot" />
      </span>
    </div>

    <template v-else>
      <div v-if="grouped.length === 0" class="cart-page-empty card" role="status">
        <div class="cart-page-empty-icon" aria-hidden="true">🛒</div>
        <p class="cart-page-empty-title">购物车是空的</p>
        <p class="cart-page-empty-hint muted">逛逛首页，把好味道装进车里</p>
        <RouterLink to="/" class="cart-page-empty-btn">去选购</RouterLink>
      </div>

      <div v-else class="cart-group cart-page-group">
        <div
          v-for="(g, gIdx) in grouped"
          :key="g.merchantId"
          class="cart-merchant cart-page-store card"
          :style="{ animationDelay: `${Math.min(gIdx * 65, 400)}ms` }"
        >
          <div class="cart-merchant-head cart-page-store-head">
            <label class="cart-check-label">
              <input
                type="checkbox"
                class="cart-check-input"
                :checked="merchantAllSelected(g)"
                :aria-label="`全选 ${g.merchantName || '店铺 #' + g.merchantId} 的商品`"
                @change="toggleMerchant(g)"
              />
              <h3 class="cart-merchant-title">{{ g.merchantName || `店铺 #${g.merchantId}` }}</h3>
            </label>
          </div>

          <div class="cart-page-lines-wrap">
            <div class="cart-page-line cart-page-line--head" aria-hidden="true">
              <span class="cart-page-col-check" />
              <span class="cart-page-col-name">商品</span>
              <span class="cart-page-col-price">单价</span>
              <span class="cart-page-col-qty">数量</span>
              <span class="cart-page-col-sub">小计</span>
              <span class="cart-page-col-act" />
            </div>
            <div
              v-for="(item, lineIdx) in g.items"
              :key="item.productId"
              class="cart-page-line"
              :style="{ animationDelay: `${Math.min(gIdx * 65 + lineIdx * 40, 520)}ms` }"
            >
              <div class="cart-page-col-check">
                <input
                  type="checkbox"
                  class="cart-check-input"
                  :checked="!!selected[item.productId]"
                  :aria-label="`选择 ${item.productName}`"
                  @change="toggleProduct(item.productId)"
                />
              </div>
              <div class="cart-page-col-name">
                <span class="cart-page-product-name">{{ item.productName }}</span>
              </div>
              <div class="cart-page-col-price">¥{{ Number(item.price).toFixed(2) }}</div>
              <div class="cart-page-col-qty">
                <div class="qty-control cart-page-qty">
                  <button
                    type="button"
                    class="qty-btn cart-page-qty-btn"
                    :disabled="item.quantity <= 1 || !!adjusting[item.productId]"
                    @click="decreaseQty(item.productId)"
                  >
                    −
                  </button>
                  <span class="qty-value">{{ item.quantity }}</span>
                  <button
                    type="button"
                    class="qty-btn cart-page-qty-btn"
                    :disabled="!!adjusting[item.productId]"
                    @click="increaseQty(item.productId)"
                  >
                    +
                  </button>
                </div>
              </div>
              <div class="cart-page-col-sub">¥{{ Number(item.subtotal).toFixed(2) }}</div>
              <div class="cart-page-col-act">
                <button
                  type="button"
                  class="cart-page-delete"
                  :disabled="!!removing[item.productId]"
                  @click="openCartRemoveConfirm(item.productId, item.productName)"
                >
                  {{ removing[item.productId] ? "…" : "删除" }}
                </button>
              </div>
            </div>
          </div>

          <p class="cart-merchant-total cart-page-store-total">
            <span class="cart-page-store-total-inner">
              <span class="cart-page-store-total-label">店铺小计</span>
              <span class="cart-page-store-total-num">¥{{ Number(g.total).toFixed(2) }}</span>
            </span>
          </p>
        </div>
      </div>

      <p v-if="grouped.length > 0" class="cart-grand-total cart-page-grand">
        <span class="cart-page-grand-label">购物车商品合计（全部）</span>
        <span class="cart-page-grand-num">¥{{ Number(grandTotal).toFixed(2) }}</span>
      </p>
    </template>

    <div
      v-if="clearConfirmOpen"
      class="cart-clear-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cart-clear-title"
      @click.self="closeClearConfirm"
    >
      <div class="cart-clear-dialog card" @click.stop>
        <p id="cart-clear-title" class="cart-clear-msg">确定一键清空购物车吗</p>
        <div class="cart-clear-actions">
          <button type="button" class="cart-clear-btn-think" @click="closeClearConfirm">
            我再想想
          </button>
          <button
            type="button"
            class="cart-clear-btn-ruthless"
            :disabled="clearAllLoading"
            @click="confirmClearAll"
          >
            {{ clearAllLoading ? "…" : "残忍清空" }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="cartRemoveConfirmOpen"
      class="cart-checkout-delete-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cart-remove-title"
      @click.self="closeCartRemoveConfirm"
    >
      <div class="cart-clear-dialog card cart-checkout-delete-dialog" @click.stop>
        <p id="cart-remove-title" class="cart-checkout-delete-title">
          确认从购物车删除该商品？
        </p>
        <p class="cart-checkout-delete-hint muted">删除后需重新加入购物车才能购买。</p>
        <p v-if="cartRemoveProductName" class="cart-checkout-delete-name muted">
          {{ cartRemoveProductName }}
        </p>
        <div class="cart-clear-actions">
          <button
            type="button"
            class="cart-clear-btn-think"
            :disabled="cartRemoveLoading"
            @click="closeCartRemoveConfirm"
          >
            否
          </button>
          <button
            type="button"
            class="cart-clear-btn-ruthless"
            :disabled="cartRemoveLoading"
            @click="confirmRemoveCartItem"
          >
            {{ cartRemoveLoading ? "…" : "是" }}
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="checkoutPanelOpen"
      class="draft-overlay cart-checkout-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="结算"
      @click.self="closeCheckoutPanel"
    >
      <div class="draft-panel card cart-checkout-panel" @click.stop>
        <div class="draft-panel-head">
          <h3 class="draft-panel-title">确认结算</h3>
          <button
            type="button"
            class="draft-panel-close"
            aria-label="关闭结算"
            @click="closeCheckoutPanel"
          >
            ×
          </button>
        </div>

        <p v-if="checkoutPreviewLoading" class="muted">加载中…</p>
        <template v-else-if="checkoutPreview">
          <div
            v-for="m in checkoutPreview.merchants"
            :key="m.merchantId"
            class="cart-checkout-merchant-block"
          >
            <p class="cart-checkout-merchant-name">
              {{ m.merchantName || `商家 #${m.merchantId}` }}
            </p>
            <ul class="draft-panel-list">
              <li v-for="it in m.items" :key="it.productId" class="draft-panel-row cart-checkout-draft-row">
                <div class="draft-line-main">
                  <span class="draft-line-name">{{ it.productName }}</span>
                  <span class="muted draft-line-unit">¥{{ Number(it.price).toFixed(2) }}</span>
                </div>
                <div class="cart-checkout-line-bottom">
                  <div class="qty-control cart-checkout-qty">
                    <button
                      type="button"
                      class="qty-btn"
                      :disabled="
                        lineQty(it.productId) <= 1 ||
                        !!adjusting[it.productId] ||
                        payLoading ||
                        cancelLoading
                      "
                      @click="checkoutDecreaseQty(it.productId)"
                    >
                      -
                    </button>
                    <span class="qty-value">{{ it.quantity }}</span>
                    <button
                      type="button"
                      class="qty-btn"
                      :disabled="
                        !!adjusting[it.productId] || payLoading || cancelLoading
                      "
                      @click="checkoutIncreaseQty(it.productId)"
                    >
                      +
                    </button>
                  </div>
                  <div class="cart-checkout-line-right">
                    <span class="draft-line-sub">¥{{ Number(it.subtotal).toFixed(2) }}</span>
                    <button
                      type="button"
                      class="cart-checkout-delete-line"
                      :disabled="checkoutDeleteLoading || payLoading || cancelLoading"
                      @click="openCheckoutDeleteConfirm(it.productId, it.productName)"
                    >
                      删
                    </button>
                  </div>
                </div>
              </li>
            </ul>
            <div class="cart-checkout-fee">
              <span>商品合计 ¥{{ Number(m.goodsAmount).toFixed(2) }}</span>
              <span>配送费 ¥{{ Number(m.deliveryFee).toFixed(2) }}</span>
            </div>
            <div v-if="m.items.length" class="draft-coupon-block">
              <p class="draft-coupon-title">优惠券</p>
              <p class="draft-coupon-line">
                <span class="draft-coupon-rule">{{ m.couponRuleText || "—" }}</span>
                <span class="draft-coupon-hint muted">{{ m.couponHint || "无" }}</span>
              </p>
              <p v-if="m.couponLineStatus === 'applied'" class="draft-coupon-amount muted">
                本单减免 ¥{{ Number(m.couponAmount).toFixed(2) }}
              </p>
            </div>
            <p class="cart-checkout-merchant-pay">
              本店应付 ¥{{ Number(m.payableAmount).toFixed(2) }}
            </p>
          </div>

          <p class="cart-checkout-grand">
            合计应付 ¥{{ Number(checkoutPreview.grandPayable).toFixed(2) }}
          </p>

          <div class="cart-checkout-actions">
            <button
              type="button"
              class="draft-pay-btn"
              :disabled="payLoading || cancelLoading"
              @click="payCartCheckout"
            >
              {{ payLoading ? "支付中…" : "模拟支付" }}
            </button>
            <button
              type="button"
              class="draft-cancel-btn"
              title="生成待支付订单，30 分钟内需完成支付"
              :disabled="payLoading || cancelLoading"
              @click="pendingCartCheckout"
            >
              {{ cancelLoading ? "处理中…" : "取消" }}
            </button>
          </div>
        </template>
        <p v-else class="muted">无法预览，请重试</p>
      </div>
    </div>

    <div
      v-if="checkoutDeleteConfirmOpen"
      class="cart-checkout-delete-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="checkout-delete-title"
      @click.self="closeCheckoutDeleteConfirm"
    >
      <div class="cart-clear-dialog card cart-checkout-delete-dialog" @click.stop>
        <p id="checkout-delete-title" class="cart-checkout-delete-title">
          确认从本次结算中移除？
        </p>
        <p class="cart-checkout-delete-hint muted">商品仍将保留在购物车中。</p>
        <p v-if="checkoutDeleteProductName" class="cart-checkout-delete-name muted">
          {{ checkoutDeleteProductName }}
        </p>
        <div class="cart-clear-actions">
          <button
            type="button"
            class="cart-clear-btn-think"
            :disabled="checkoutDeleteLoading"
            @click="closeCheckoutDeleteConfirm"
          >
            否
          </button>
          <button
            type="button"
            class="cart-clear-btn-ruthless"
            :disabled="checkoutDeleteLoading"
            @click="confirmCheckoutDeleteProduct"
          >
            {{ checkoutDeleteLoading ? "…" : "是" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import {
  clearCart,
  decreaseCartItem,
  fetchCartItems,
  increaseCartItem,
  postCartCheckout,
  postCartCheckoutPreview,
  removeCartItem
} from "../../api/cart";
import type { CartCheckoutPreview } from "../../types/draft";
import type { CartItem } from "../../types/cart";

const loading = ref(true);
const error = ref("");
const items = ref<CartItem[]>([]);
const removing = ref<Record<number, boolean>>({});
const adjusting = ref<Record<number, boolean>>({});
const clearConfirmOpen = ref(false);
const clearAllLoading = ref(false);

const selected = ref<Record<number, boolean>>({});
const previewLoading = ref(false);
const previewGrandPayable = ref<number | null>(null);
let previewDebounce: number | undefined;

const checkoutPanelOpen = ref(false);
const checkoutPreviewLoading = ref(false);
const checkoutPreview = ref<CartCheckoutPreview | null>(null);
const payLoading = ref(false);
const cancelLoading = ref(false);

const selectedPayDropdownOpen = ref(false);
const selectedPayWrap = ref<HTMLElement | null>(null);

const checkoutDeleteConfirmOpen = ref(false);
const checkoutDeleteProductId = ref<number | null>(null);
const checkoutDeleteProductName = ref("");
const checkoutDeleteLoading = ref(false);

const cartRemoveConfirmOpen = ref(false);
const cartRemoveProductId = ref<number | null>(null);
const cartRemoveProductName = ref("");
const cartRemoveLoading = ref(false);

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
  }, 1600);
}

const selectedProductIds = computed(() => {
  const out: number[] = [];
  for (const it of items.value) {
    if (selected.value[it.productId]) {
      out.push(it.productId);
    }
  }
  return out;
});

const selectedCount = computed(() => selectedProductIds.value.length);

/** 当前勾选中的购物车行（用于顶部「已选商品」下拉管理） */
const selectedLineItems = computed(() => {
  const out: CartItem[] = [];
  for (const it of items.value) {
    if (selected.value[it.productId]) {
      out.push(it);
    }
  }
  return out;
});

function toggleSelectedPayDropdown() {
  selectedPayDropdownOpen.value = !selectedPayDropdownOpen.value;
}

function handleDocClickOutside(ev: MouseEvent) {
  if (!selectedPayDropdownOpen.value) return;
  const el = selectedPayWrap.value;
  const t = ev.target;
  if (el && t instanceof Node && !el.contains(t)) {
    selectedPayDropdownOpen.value = false;
  }
}

function clearAllSelected() {
  const next = { ...selected.value };
  for (const it of items.value) {
    next[it.productId] = false;
  }
  selected.value = next;
}

function syncSelectionFromItems() {
  const prev = { ...selected.value };
  const next: Record<number, boolean> = {};
  for (const it of items.value) {
    next[it.productId] = prev[it.productId] ?? false;
  }
  selected.value = next;
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    items.value = await fetchCartItems();
    syncSelectionFromItems();
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    error.value = err?.response?.data?.message || err?.message || "加载失败";
    items.value = [];
  } finally {
    loading.value = false;
  }
}

async function debouncedPreview() {
  if (previewDebounce) window.clearTimeout(previewDebounce);
  previewDebounce = window.setTimeout(() => runPreview(), 320);
}

async function runPreview() {
  const ids = selectedProductIds.value;
  if (ids.length === 0) {
    previewGrandPayable.value = null;
    previewLoading.value = false;
    return;
  }
  previewLoading.value = true;
  try {
    const p = await postCartCheckoutPreview(ids);
    previewGrandPayable.value = Number(p.grandPayable);
  } catch {
    previewGrandPayable.value = null;
  } finally {
    previewLoading.value = false;
  }
}

watch(
  () => selected.value,
  () => {
    debouncedPreview();
  },
  { deep: true }
);

onMounted(() => {
  load();
  document.addEventListener("click", handleDocClickOutside);
});
onUnmounted(() => {
  document.removeEventListener("click", handleDocClickOutside);
});

function merchantAllSelected(g: { items: CartItem[] }) {
  if (!g.items.length) return false;
  return g.items.every((it) => selected.value[it.productId]);
}

function toggleMerchant(g: { items: CartItem[] }) {
  const all = merchantAllSelected(g);
  const next = { ...selected.value };
  for (const it of g.items) {
    next[it.productId] = !all;
  }
  selected.value = next;
}

function toggleProduct(productId: number) {
  selected.value = {
    ...selected.value,
    [productId]: !selected.value[productId]
  };
}

function openClearConfirm() {
  clearConfirmOpen.value = true;
}

function closeClearConfirm() {
  if (clearAllLoading.value) return;
  clearConfirmOpen.value = false;
}

async function confirmClearAll() {
  if (clearAllLoading.value) return;
  clearAllLoading.value = true;
  try {
    await clearCart();
    clearConfirmOpen.value = false;
    await load();
    showToast("success", "购物车已清空");
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "清空失败");
  } finally {
    clearAllLoading.value = false;
  }
}

function openCartRemoveConfirm(productId: number, productName: string) {
  if (removing.value[productId]) return;
  cartRemoveProductId.value = productId;
  cartRemoveProductName.value = productName || "";
  cartRemoveConfirmOpen.value = true;
}

function closeCartRemoveConfirm() {
  if (cartRemoveLoading.value) return;
  cartRemoveConfirmOpen.value = false;
  cartRemoveProductId.value = null;
  cartRemoveProductName.value = "";
}

async function confirmRemoveCartItem() {
  const pid = cartRemoveProductId.value;
  if (pid == null || cartRemoveLoading.value) return;
  cartRemoveLoading.value = true;
  removing.value[pid] = true;
  try {
    await removeCartItem(pid);
    await load();
    cartRemoveConfirmOpen.value = false;
    cartRemoveProductId.value = null;
    cartRemoveProductName.value = "";
    await refreshCheckoutPreviewIfOpen();
    showToast("success", "已从购物车删除");
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "删除失败");
  } finally {
    cartRemoveLoading.value = false;
    removing.value[pid] = false;
  }
}

async function increaseQty(productId: number) {
  if (adjusting.value[productId]) return;
  adjusting.value[productId] = true;
  try {
    await increaseCartItem(productId, 1);
    await load();
    await refreshCheckoutPreviewIfOpen();
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "增加失败");
  } finally {
    adjusting.value[productId] = false;
  }
}

async function decreaseQty(productId: number) {
  if (adjusting.value[productId]) return;
  const item = items.value.find((x) => x.productId === productId);
  if (!item || item.quantity <= 1) return;

  adjusting.value[productId] = true;
  try {
    await decreaseCartItem(productId, 1);
    await load();
    await refreshCheckoutPreviewIfOpen();
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "减少失败");
  } finally {
    adjusting.value[productId] = false;
  }
}

/** 购物车列表中的实时数量（用于结算弹层里减号是否可点） */
function lineQty(productId: number): number {
  const row = items.value.find((x) => x.productId === productId);
  return row?.quantity ?? 0;
}

/** 无已选商品时：error=提示异常；silent=仅关弹层（用户主动取消勾选导致） */
async function refreshCheckoutPreviewIfOpen(
  emptyMode: "error" | "silent" = "error"
) {
  if (!checkoutPanelOpen.value) return;
  const ids = selectedProductIds.value;
  if (ids.length === 0) {
    checkoutPreview.value = null;
    checkoutPanelOpen.value = false;
    if (emptyMode === "error") {
      showToast("error", "暂无已选商品");
    }
    return;
  }
  try {
    const p = await postCartCheckoutPreview(ids);
    checkoutPreview.value = p;
    previewGrandPayable.value = Number(p.grandPayable);
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "刷新结算信息失败");
  }
}

async function checkoutIncreaseQty(productId: number) {
  if (adjusting.value[productId]) return;
  adjusting.value[productId] = true;
  try {
    await increaseCartItem(productId, 1);
    await load();
    await refreshCheckoutPreviewIfOpen();
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "增加失败");
  } finally {
    adjusting.value[productId] = false;
  }
}

async function checkoutDecreaseQty(productId: number) {
  if (adjusting.value[productId]) return;
  const item = items.value.find((x) => x.productId === productId);
  if (!item || item.quantity <= 1) return;

  adjusting.value[productId] = true;
  try {
    await decreaseCartItem(productId, 1);
    await load();
    await refreshCheckoutPreviewIfOpen();
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "减少失败");
  } finally {
    adjusting.value[productId] = false;
  }
}

const grouped = computed(() => {
  const map = new Map<number, { merchantId: number; merchantName: string | null; items: CartItem[]; total: number }>();
  for (const item of items.value) {
    const key = item.merchantId;
    if (!map.has(key)) {
      map.set(key, { merchantId: item.merchantId, merchantName: item.merchantName, items: [], total: 0 });
    }
    const g = map.get(key)!;
    g.items.push(item);
    g.total += item.subtotal;
  }
  return Array.from(map.values());
});

const grandTotal = computed(() => {
  return grouped.value.reduce((sum, g) => sum + g.total, 0);
});

function openCheckoutDeleteConfirm(productId: number, productName: string) {
  if (payLoading.value || cancelLoading.value) return;
  checkoutDeleteProductId.value = productId;
  checkoutDeleteProductName.value = productName || "";
  checkoutDeleteConfirmOpen.value = true;
}

function closeCheckoutDeleteConfirm() {
  if (checkoutDeleteLoading.value) return;
  checkoutDeleteConfirmOpen.value = false;
  checkoutDeleteProductId.value = null;
  checkoutDeleteProductName.value = "";
}

async function confirmCheckoutDeleteProduct() {
  const pid = checkoutDeleteProductId.value;
  if (pid == null || checkoutDeleteLoading.value) return;
  checkoutDeleteLoading.value = true;
  try {
    selected.value = { ...selected.value, [pid]: false };
    checkoutDeleteConfirmOpen.value = false;
    checkoutDeleteProductId.value = null;
    checkoutDeleteProductName.value = "";

    const ids = selectedProductIds.value;
    if (ids.length === 0) {
      await refreshCheckoutPreviewIfOpen("silent");
      showToast("success", "已全部从结算中移除，商品仍在购物车");
      return;
    }
    await refreshCheckoutPreviewIfOpen();
    showToast("success", "已从本次结算中移除");
  } finally {
    checkoutDeleteLoading.value = false;
  }
}

async function openCheckoutPanel() {
  if (selectedProductIds.value.length === 0) return;
  selectedPayDropdownOpen.value = false;
  checkoutPanelOpen.value = true;
  checkoutPreviewLoading.value = true;
  checkoutPreview.value = null;
  try {
    checkoutPreview.value = await postCartCheckoutPreview(selectedProductIds.value);
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "预览失败");
    checkoutPanelOpen.value = false;
  } finally {
    checkoutPreviewLoading.value = false;
  }
}

function closeCheckoutPanel() {
  if (payLoading.value || cancelLoading.value) return;
  if (checkoutDeleteLoading.value) return;
  checkoutDeleteConfirmOpen.value = false;
  checkoutDeleteProductId.value = null;
  checkoutDeleteProductName.value = "";
  checkoutPanelOpen.value = false;
}

async function payCartCheckout() {
  if (selectedProductIds.value.length === 0) return;
  if (payLoading.value) return;
  payLoading.value = true;
  try {
    await postCartCheckout(selectedProductIds.value, true);
    showToast("success", "支付成功");
    checkoutPanelOpen.value = false;
    await load();
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "支付失败");
  } finally {
    payLoading.value = false;
  }
}

async function pendingCartCheckout() {
  if (selectedProductIds.value.length === 0) return;
  if (cancelLoading.value) return;
  cancelLoading.value = true;
  try {
    await postCartCheckout(selectedProductIds.value, false);
    showToast("success", "已生成待支付订单，请在 30 分钟内完成支付");
    checkoutPanelOpen.value = false;
    await load();
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    showToast("error", err?.response?.data?.message || err?.message || "操作失败");
  } finally {
    cancelLoading.value = false;
  }
}
</script>

<style scoped>
@keyframes cart-page-card-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes cart-page-hero-glow {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(8%, -6%) scale(1.12);
  }
}

@keyframes cart-page-dot {
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

.cart-page.cart {
  max-width: 920px;
  margin: 0 auto;
  padding-bottom: 36px;
  /* 与暖色页面底 (#f5f3f0) 区分：冷灰边 + 浮起阴影 */
  --cart-card-bg: #ffffff;
  --cart-card-inner: #eef2f7;
  --cart-border-strong: #9ca3af;
  --cart-border: #cbd5e1;
  --cart-shadow:
    0 1px 0 rgba(255, 255, 255, 0.85) inset,
    0 2px 8px rgba(15, 23, 42, 0.07),
    0 10px 28px rgba(15, 23, 42, 0.08);
}

.cart-page-toast {
  position: fixed;
  top: calc(var(--header-height) + 12px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 12500;
  padding: 10px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 10px 36px rgba(15, 23, 42, 0.12);
  max-width: min(92vw, 380px);
  text-align: center;
}

.cart-page-toast.success {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  color: #047857;
  border: 1px solid #a7f3d0;
}

.cart-page-toast.error {
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.cart-page-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 18px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(37, 99, 235, 0.2);
  text-decoration: none;
  transition:
    transform 0.22s var(--ease-out),
    box-shadow 0.22s var(--ease-out);
}

.cart-page-back:hover {
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.12);
  transform: translateX(-3px);
  color: var(--color-primary-strong);
}

.cart-page-hero {
  position: relative;
  padding: 22px 22px 20px;
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(99, 102, 241, 0.18);
  background: linear-gradient(
    128deg,
    rgba(238, 242, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.92) 48%,
    rgba(224, 242, 254, 0.75) 100%
  );
  box-shadow: 0 12px 40px rgba(79, 70, 229, 0.08);
  overflow: hidden;
}

.cart-page-hero-glow {
  position: absolute;
  width: 120%;
  height: 120%;
  top: -45%;
  right: -35%;
  background: radial-gradient(
    circle at 35% 45%,
    rgba(99, 102, 241, 0.2) 0%,
    rgba(14, 165, 233, 0.08) 40%,
    transparent 58%
  );
  pointer-events: none;
  animation: cart-page-hero-glow 16s ease-in-out infinite;
}

.cart-page-hero-row {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.cart-page-title {
  margin: 0;
  font-size: clamp(1.35rem, 3.5vw, 1.65rem);
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.cart-page-sub {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.45;
  max-width: 420px;
}

button.cart-clear-all-btn {
  padding: 8px 16px;
  border: 1px solid rgba(248, 113, 113, 0.55);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #b91c1c;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.08);
  transition:
    background 0.2s var(--ease-out),
    transform 0.2s var(--ease-out),
    box-shadow 0.2s var(--ease-out);
}

button.cart-clear-all-btn:hover:not(:disabled) {
  background: #fef2f2;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(220, 38, 38, 0.12);
}

.cart-page-error {
  margin: 0 0 16px;
}

.cart-page-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px 20px;
  margin-bottom: 16px;
  max-width: none;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--cart-border);
  border-radius: 12px;
  background: var(--cart-card-bg);
  box-shadow: var(--cart-shadow);
  animation: cart-page-card-in 0.45s var(--ease-out-expo) backwards;
}

.cart-page-loading-label {
  font-size: 15px;
  font-weight: 600;
  color: #64748b;
}

.cart-page-loading-dots {
  display: inline-flex;
  gap: 6px;
}

.cart-page-loading-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #0ea5e9);
  animation: cart-page-dot 1s ease-in-out infinite;
}

.cart-page-loading-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.cart-page-loading-dot:nth-child(3) {
  animation-delay: 0.3s;
}

.cart-page-empty {
  text-align: center;
  padding: 40px 24px 36px;
  margin-bottom: 8px;
  max-width: none;
  width: 100%;
  box-sizing: border-box;
  border-radius: 12px;
  border: 2px dashed var(--cart-border);
  background: var(--cart-card-bg);
  box-shadow: var(--cart-shadow);
  animation: cart-page-card-in 0.55s var(--ease-out-expo) backwards;
}

.cart-page-empty-icon {
  font-size: 2.5rem;
  line-height: 1;
  margin-bottom: 12px;
}

.cart-page-empty-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.cart-page-empty-hint {
  margin: 0 0 22px;
  font-size: 14px;
}

.cart-page-empty-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 24px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  text-decoration: none;
  background: linear-gradient(135deg, #4f46e5, #2563eb, #0ea5e9);
  background-size: 140% 140%;
  box-shadow: 0 8px 28px rgba(79, 70, 229, 0.35);
  transition:
    transform 0.22s var(--ease-out),
    box-shadow 0.22s var(--ease-out);
}

.cart-page-empty-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.38);
  color: #fff;
}

.cart-page-group {
  gap: 18px;
}

.cart-page-store.card {
  position: relative;
  max-width: none;
  width: 100%;
  box-sizing: border-box;
  padding: 26px 20px 18px;
  border: 1px solid var(--cart-border-strong);
  border-radius: 12px;
  background: #ffffff;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.96) inset,
    0 2px 10px rgba(15, 23, 42, 0.05),
    0 10px 28px rgba(15, 23, 42, 0.08);
  animation: cart-page-card-in 0.52s var(--ease-out-expo) backwards;
  transition:
    transform 0.28s var(--ease-out),
    box-shadow 0.28s var(--ease-out),
    border-color 0.28s var(--ease-out);
}

.cart-page-store.card:hover {
  transform: translateY(-1px);
  border-color: #6b7280;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.96) inset,
    0 4px 18px rgba(15, 23, 42, 0.07),
    0 14px 38px rgba(15, 23, 42, 0.1);
}

.cart-page.cart .cart-merchant {
  background: transparent;
  border: none;
  padding: 0;
  border-radius: 0;
}

/* 店名略下移、略靠左；商品列表仍保持原左内边距 */
.cart-page-store-head {
  margin: 18px 0 10px;
  padding: 10px 20px 14px 14px;
  border-bottom: 1px solid #f1f5f9;
}

.cart-page-store-head .cart-check-label {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  width: 100%;
  margin: 0;
  cursor: pointer;
}

.cart-page-store-head .cart-check-input {
  justify-self: center;
}

.cart-page.cart .cart-merchant-title {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.35;
}

.cart-page-lines-wrap {
  overflow-x: auto;
  margin: 0;
  padding: 4px 0 8px;
  -webkit-overflow-scrolling: touch;
}

.cart-page-line {
  --cart-line-cols: 44px minmax(180px, 1fr) 84px 136px 92px 80px;
  display: grid;
  grid-template-columns: var(--cart-line-cols);
  align-items: center;
  gap: 0 12px;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f9;
  min-width: 640px;
  box-sizing: border-box;
  animation: cart-page-card-in 0.48s var(--ease-out-expo) backwards;
}

.cart-page-line:last-child {
  border-bottom: none;
}

.cart-page-line--head {
  min-width: 640px;
  padding: 0 20px 10px;
  box-sizing: border-box;
  align-items: end;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #94a3b8;
  border-bottom: 2px solid #e2e8f0;
}

.cart-page-line--head .cart-page-col-name {
  text-align: left;
}

.cart-page-line--head .cart-page-col-price,
.cart-page-line--head .cart-page-col-sub {
  text-align: right;
}

.cart-page-line--head .cart-page-col-qty {
  text-align: center;
}

.cart-page-col-check {
  display: flex;
  justify-content: center;
  align-items: center;
}

.cart-page-col-name {
  min-width: 0;
  text-align: left;
}

.cart-page-product-name {
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
  line-height: 1.4;
  display: block;
}

.cart-page-col-price,
.cart-page-col-sub {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  font-size: 14px;
  font-variant-numeric: tabular-nums;
  color: #334155;
  text-align: right;
}

.cart-page-col-sub {
  font-weight: 700;
  color: #0f172a;
}

.cart-page-col-qty {
  display: flex;
  justify-content: center;
  align-items: center;
}

.cart-page-col-act {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.cart-page-qty {
  justify-content: center;
}

button.cart-page-qty-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(180deg, #f1f5f9, #e2e8f0);
  color: #0f172a;
  font-weight: 800;
  font-size: 16px;
  line-height: 1;
  padding: 0;
  cursor: pointer;
  transition:
    background 0.18s var(--ease-out),
    transform 0.18s var(--ease-out),
    box-shadow 0.18s var(--ease-out);
}

button.cart-page-qty-btn:hover:not(:disabled) {
  background: linear-gradient(180deg, #e0e7ff, #c7d2fe);
  transform: scale(1.06);
  box-shadow: none;
}

button.cart-page-qty-btn:active:not(:disabled) {
  transform: scale(0.96);
}

button.cart-page-qty-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.cart-page-delete {
  padding: 6px 12px;
  border: 1px solid rgba(252, 165, 165, 0.7);
  border-radius: 999px;
  background: #fff;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.2s var(--ease-out),
    transform 0.2s var(--ease-out);
}

.cart-page-delete:hover:not(:disabled) {
  background: #fef2f2;
  transform: scale(1.03);
}

.cart-page-delete:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.cart-page-store-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin: 14px 0 0;
  padding: 12px 20px;
  box-sizing: border-box;
  width: 100%;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.9) inset;
}

.cart-page-store-total-inner {
  display: inline-flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.cart-page-store-total-label {
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
}

.cart-page-store-total-num {
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}

.cart-page-grand {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  margin-top: 22px;
  padding: 18px 20px;
  border-radius: 12px;
  background: var(--cart-card-bg);
  border: 1px solid var(--cart-border-strong);
  box-shadow: var(--cart-shadow);
  text-align: right;
  animation: cart-page-card-in 0.55s var(--ease-out-expo) 0.08s backwards;
}

.cart-page-grand-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.cart-page-grand-num {
  font-size: 22px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: #dc2626;
}

.cart-pay-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 14px 18px;
  position: sticky;
  top: calc(var(--header-height) + 8px);
  z-index: 30;
  max-width: none;
  width: 100%;
  box-sizing: border-box;
  border-radius: 12px;
  border: 1px solid var(--cart-border-strong);
  background: var(--cart-card-bg);
  box-shadow: var(--cart-shadow);
}

.cart-pay-bar-sum {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}
.cart-selected-pay-wrap {
  position: relative;
  min-width: 0;
}
.cart-selected-pay-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 10px;
}
.cart-pay-bar-amount {
  font-size: 22px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  color: #dc2626;
}
button.cart-selected-pay-toggle {
  padding: 6px 12px;
  border: 1px solid rgba(148, 163, 184, 0.55);
  border-radius: 999px;
  background: #fff;
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition:
    background 0.2s var(--ease-out),
    border-color 0.2s var(--ease-out),
    transform 0.2s var(--ease-out);
}
button.cart-selected-pay-toggle:hover {
  background: #f8fafc;
  border-color: #818cf8;
  transform: translateY(-1px);
}
.cart-selected-pay-caret {
  font-size: 10px;
  color: #6b7280;
}
.cart-selected-pay-dropdown {
  position: absolute;
  left: 0;
  top: calc(100% + 10px);
  z-index: 50;
  width: min(100vw - 48px, 380px);
  max-height: min(56vh, 320px);
  overflow: auto;
  padding: 12px 14px;
  box-sizing: border-box;
  box-shadow:
    0 2px 8px rgba(15, 23, 42, 0.08),
    0 16px 40px rgba(15, 23, 42, 0.12);
  border: 1px solid var(--cart-border-strong);
  border-radius: 12px;
  background: var(--cart-card-bg);
  animation: cart-page-card-in 0.32s var(--ease-out-expo) backwards;
}
.cart-selected-pay-empty {
  margin: 8px 0;
  font-size: 13px;
}
.cart-selected-pay-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.cart-selected-pay-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
}
.cart-selected-pay-row:last-child {
  border-bottom: none;
}
.cart-selected-pay-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cart-selected-pay-name {
  font-weight: 600;
  font-size: 14px;
  color: #111827;
  line-height: 1.3;
}
.cart-selected-pay-merchant {
  font-size: 12px;
}
.cart-selected-pay-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
}
.cart-selected-pay-qty {
  color: #6b7280;
}
.cart-selected-pay-sub {
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}
button.cart-selected-pay-remove {
  margin-left: auto;
  padding: 5px 11px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background 0.2s var(--ease-out),
    color 0.2s var(--ease-out);
}
button.cart-selected-pay-remove:hover {
  background: #fef2f2;
  color: #b91c1c;
  border-color: #fecaca;
}
.cart-selected-pay-footer {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f3f4f6;
}
.cart-selected-pay-clear {
  width: 100%;
  padding: 8px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
}
.cart-selected-pay-clear:hover {
  background: #f9fafb;
  color: #374151;
}
button.cart-go-pay-btn {
  padding: 12px 26px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #4f46e5 0%, #2563eb 45%, #0ea5e9 100%);
  background-size: 140% 140%;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 28px rgba(79, 70, 229, 0.38);
  transition:
    transform 0.22s var(--ease-out),
    box-shadow 0.22s var(--ease-out),
    filter 0.22s var(--ease-out);
}
button.cart-go-pay-btn:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 12px 36px rgba(37, 99, 235, 0.45);
  filter: saturate(1.05);
}
button.cart-go-pay-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.99);
}
button.cart-go-pay-btn:disabled {
  background: #94a3b8;
  box-shadow: none;
  cursor: not-allowed;
  filter: none;
  transform: none;
}
.cart-check-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  margin: 0;
}
.cart-check-input {
  width: 18px;
  height: 18px;
  cursor: pointer;
  flex-shrink: 0;
  accent-color: #4f46e5;
}
.cart-clear-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  animation: cart-page-card-in 0.25s var(--ease-out) backwards;
}
.cart-clear-dialog {
  max-width: 360px;
  width: 100%;
  padding: 22px 24px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.2);
}
.cart-clear-msg {
  margin: 0 0 20px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  text-align: center;
  line-height: 1.5;
}
.cart-clear-actions {
  display: flex;
  gap: 10px;
  justify-content: stretch;
}
.cart-clear-actions button {
  flex: 1;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.cart-clear-btn-think {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}
.cart-clear-btn-think:hover {
  background: #f9fafb;
}
.cart-clear-btn-ruthless {
  border: none;
  background: #dc2626;
  color: #fff;
}
.cart-clear-btn-ruthless:hover:not(:disabled) {
  background: #b91c1c;
}
.cart-clear-btn-ruthless:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.cart-checkout-overlay {
  z-index: 2100;
}
.cart-checkout-panel {
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
}
.cart-checkout-merchant-block {
  border-bottom: 1px solid #f3f4f6;
  padding-bottom: 12px;
  margin-bottom: 12px;
}
.cart-checkout-merchant-block:last-of-type {
  border-bottom: none;
}
.cart-checkout-merchant-name {
  margin: 0 0 8px;
  font-weight: 700;
  color: #111827;
}
.cart-checkout-fee {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #6b7280;
  margin: 8px 0;
}
.cart-checkout-merchant-pay {
  margin: 8px 0 0;
  padding-top: 8px;
  font-weight: 700;
  text-align: right;
  color: #0f172a;
}
.cart-checkout-grand {
  margin: 16px 0;
  text-align: right;
  font-size: 1.05rem;
  font-weight: 800;
  color: #dc2626;
}
.cart-checkout-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cart-checkout-draft-row {
  gap: 10px;
}
.cart-checkout-line-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.cart-checkout-qty {
  flex-shrink: 0;
}
.cart-checkout-line-right {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}
.cart-checkout-delete-line {
  padding: 4px 10px;
  border: 1px solid #fecaca;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: #b91c1c;
  cursor: pointer;
  flex-shrink: 0;
}
.cart-checkout-delete-line:hover:not(:disabled) {
  background: #fef2f2;
}
.cart-checkout-delete-line:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.cart-checkout-delete-overlay {
  position: fixed;
  inset: 0;
  z-index: 2200;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  animation: cart-page-card-in 0.25s var(--ease-out) backwards;
}
.cart-checkout-delete-dialog {
  max-width: 360px;
}
.cart-checkout-delete-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  text-align: center;
}
.cart-checkout-delete-hint {
  margin: 0 0 8px;
  font-size: 13px;
  text-align: center;
  line-height: 1.4;
}
.cart-checkout-delete-name {
  margin: 0 0 18px;
  font-size: 14px;
  text-align: center;
  line-height: 1.4;
}
</style>
