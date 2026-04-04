<template>
  <div class="shop">
    <div
      v-if="toast.visible"
      class="toast"
      :class="toast.type"
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      {{ toast.message }}
    </div>
    <p class="shop-back">
      <RouterLink to="/">← 返回首页</RouterLink>
    </p>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-else-if="loading" class="muted">加载中…</p>

    <template v-else-if="merchant">
      <header class="shop-header card">
        <div class="shop-header-row">
          <h2 class="shop-title">{{ merchant.name }}</h2>
          <button
            v-if="auth.token"
            type="button"
            class="draft-badge-btn"
            aria-label="待下单"
            @click="openDraftPanel"
          >
            <span class="draft-icon" aria-hidden="true">待</span>
            <span v-if="draftTotalQty > 0" class="draft-badge-count">{{ draftTotalQty }}</span>
          </button>
        </div>
        <p class="shop-meta">
          <span>评分 {{ Number(merchant.rating).toFixed(1) }}</span>
          <span>月售 {{ merchant.monthlySales }}</span>
        </p>
        <p class="shop-fee">
          起送 ¥{{ Number(merchant.minDeliveryAmount).toFixed(2) }} · 配送 ¥{{
            Number(merchant.deliveryFee).toFixed(2)
          }}
        </p>
      </header>

      <section class="merchant-seckill card" aria-label="限时秒杀券">
        <h3 class="merchant-seckill-title">限时秒杀券</h3>
        <p v-if="!seckillCoupons.length" class="merchant-seckill-empty muted">
          暂无可用秒杀券（可能未上架、已抢完或不在有效期内）
        </p>
        <ul v-else class="merchant-seckill-list">
          <li v-for="c in seckillCoupons" :key="c.id" class="merchant-seckill-row">
            <div class="merchant-seckill-main">
              <p class="merchant-seckill-rule">
                满¥{{ Number(c.thresholdAmount).toFixed(2) }}减¥{{
                  Number(c.discountAmount).toFixed(2)
                }}
              </p>
              <p class="merchant-seckill-meta muted">
                有效期至 {{ formatCouponUntil(c.validUntil) }} · 剩余 {{ c.stockRemain }}
              </p>
            </div>
            <button
              type="button"
              class="merchant-seckill-claim"
              :class="{ 'merchant-seckill-claim--done': isSeckillClaimed(c.id) }"
              :disabled="
                !!claimLoading[c.id] || c.stockRemain < 1 || isSeckillClaimed(c.id)
              "
              :aria-busy="!!claimLoading[c.id]"
              @click="claimCoupon(c.id)"
            >
              {{
                claimLoading[c.id]
                  ? "…"
                  : isSeckillClaimed(c.id)
                    ? "已抢"
                    : "抢"
              }}
            </button>
          </li>
        </ul>
      </section>

      <nav v-if="merchant" class="shop-tabs card" aria-label="店铺内容" role="tablist">
        <button
          id="shop-tab-products"
          type="button"
          class="shop-tab"
          role="tab"
          :class="{ active: shopTab === 'products' }"
          :aria-selected="shopTab === 'products'"
          aria-controls="shop-panel-products"
          @click="shopTab = 'products'"
        >
          菜品
        </button>
        <button
          id="shop-tab-reviews"
          type="button"
          class="shop-tab"
          role="tab"
          :class="{ active: shopTab === 'reviews' }"
          :aria-selected="shopTab === 'reviews'"
          aria-controls="shop-panel-reviews"
          @click="shopTab = 'reviews'"
        >
          评价
        </button>
      </nav>

      <div
        id="shop-panel-products"
        v-show="shopTab === 'products'"
        class="shop-body"
        role="tabpanel"
        aria-labelledby="shop-tab-products"
      >
        <aside v-if="categories.length" class="shop-cats" aria-label="商品分类">
          <button
            v-for="c in categories"
            :key="c.id"
            type="button"
            class="cat-btn"
            :class="{ active: activeCategoryId === c.id }"
            @click="selectCategory(c.id)"
          >
            {{ c.name }}
          </button>
        </aside>

        <section class="shop-products">
          <p v-if="!products.length" class="muted">本店暂无商品</p>
          <p v-else-if="!filteredProducts.length" class="muted">该分类下暂无商品</p>
          <ul v-else class="product-list">
            <li v-for="p in filteredProducts" :key="p.id" class="product-row">
              <div class="product-main">
                <h3 class="product-name">{{ p.name }}</h3>
                <p v-if="p.description" class="product-desc">{{ p.description }}</p>
                <p class="product-sub muted">
                  月售 {{ p.sales }} ·
                  <template v-if="productOutOfStock(p)">
                    <span class="product-stock-out">已售罄</span>
                  </template>
                  <template v-else>库存 {{ p.stock }}</template>
                </p>
              </div>
              <div class="product-side">
                <span class="product-price">¥{{ Number(p.price).toFixed(2) }}</span>
                <div class="product-actions">
                  <button
                    type="button"
                    class="add-cart-btn"
                    :class="{ 'remove-cart-btn': !!inCart[p.id] }"
                    :disabled="
                      !!addLoading[p.id] ||
                      !!removeLoading[p.id] ||
                      (!inCart[p.id] && productOutOfStock(p))
                    "
                    @click="toggleCart(p.id)"
                  >
                    {{
                      addLoading[p.id]
                        ? "加入中…"
                        : removeLoading[p.id]
                          ? "删除中…"
                          : inCart[p.id]
                            ? "从购物车删除"
                            : "加入购物车"
                    }}
                  </button>
                  <button
                    type="button"
                    class="draft-order-btn"
                    :disabled="!!draftAddLoading[p.id] || productOutOfStock(p)"
                    @click="addToDraft(p.id)"
                  >
                    {{ draftAddLoading[p.id] ? "加入中…" : "下单" }}
                  </button>
                </div>
              </div>
            </li>
          </ul>
        </section>
      </div>

      <section
        id="shop-panel-reviews"
        v-show="shopTab === 'reviews'"
        class="merchant-reviews card"
        role="tabpanel"
        aria-labelledby="shop-tab-reviews"
      >
        <h3 class="merchant-reviews-title">用户评价</h3>

        <div v-if="auth.token" class="merchant-review-form">
          <p class="merchant-review-form-title">写评价</p>
          <div class="merchant-review-rating-row">
            <span id="merchant-review-rating-label" class="muted">评分</span>
            <div
              class="merchant-review-stars"
              role="radiogroup"
              aria-labelledby="merchant-review-rating-label"
            >
              <button
                v-for="n in 5"
                :key="n"
                type="button"
                class="merchant-review-star-btn"
                role="radio"
                :class="{ on: n <= reviewRating }"
                :aria-checked="n === reviewRating"
                :aria-label="`${n} 星`"
                @click="reviewRating = n"
              >
                ★
              </button>
            </div>
            <span class="muted">{{ reviewRating }} 分</span>
          </div>
          <label for="merchant-review-content" class="merchant-review-field-label"
            >评价正文</label
          >
          <textarea
            id="merchant-review-content"
            v-model="reviewContent"
            class="merchant-review-textarea"
            rows="4"
            maxlength="2000"
            placeholder="口味、配送、包装等（最多 2000 字）"
          />
          <p class="muted merchant-review-hint">图片（选填，本地上传，最多 6 张）</p>
          <input
            ref="reviewImageInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            class="merchant-review-file-input"
            @change="onReviewImagesPick"
          />
          <div class="merchant-review-image-actions">
            <button
              type="button"
              class="merchant-review-pick-btn"
              :disabled="reviewImageUploading || reviewImageUrls.length >= 6"
              @click="triggerReviewImagePick"
            >
              {{ reviewImageUploading ? "上传中…" : "选择图片" }}
            </button>
            <span v-if="reviewImageUrls.length" class="muted merchant-review-img-count"
              >已选 {{ reviewImageUrls.length }}/6</span
            >
          </div>
          <div v-if="reviewImageUrls.length" class="merchant-review-previews">
            <div v-for="(url, idx) in reviewImageUrls" :key="url + idx" class="merchant-review-preview-cell">
              <img :src="url" alt="" class="merchant-review-preview-img" @error="onReviewImgError" />
              <button
                type="button"
                class="merchant-review-preview-remove"
                title="移除"
                aria-label="移除该图片"
                @click="removeReviewImage(idx)"
              >
                ×
              </button>
            </div>
          </div>
          <p class="muted merchant-review-hint">推荐菜品（选填，可勾选多项）</p>
          <div class="merchant-review-rec-panel">
            <button
              type="button"
              class="merchant-review-rec-toggle"
              :aria-expanded="reviewRecommendExpanded"
              aria-controls="merchant-review-rec-list"
              @click="reviewRecommendExpanded = !reviewRecommendExpanded"
            >
              <span>{{ reviewRecommendExpanded ? "收起" : "展开" }}推荐菜品列表</span>
              <span class="merchant-review-rec-chevron" aria-hidden="true">{{
                reviewRecommendExpanded ? "▲" : "▼"
              }}</span>
            </button>
            <p v-show="!reviewRecommendExpanded" class="merchant-review-rec-collapsed-hint muted">
              {{ reviewRecommendSummaryText }}
            </p>
            <div
              id="merchant-review-rec-list"
              v-show="reviewRecommendExpanded"
              class="merchant-review-rec-scroll"
              role="group"
              aria-label="推荐菜品"
            >
              <label v-for="p in products" :key="p.id" class="merchant-review-rec-row">
                <input v-model="reviewRecommendIds" type="checkbox" :value="p.id" />
                <span>{{ p.name }}</span>
              </label>
              <p v-if="!products.length" class="muted merchant-review-rec-empty">暂无商品</p>
            </div>
          </div>
          <button
            type="button"
            class="merchant-review-submit"
            :disabled="reviewSubmitting"
            @click="submitReview"
          >
            {{ reviewSubmitting ? "提交中…" : "发布评价" }}
          </button>
        </div>
        <p v-else class="muted merchant-review-login-hint">
          <RouterLink to="/auth/login">登录</RouterLink> 后可为该商家写评价
        </p>

        <p v-if="reviewsLoading" class="muted">评价加载中…</p>
        <p v-else-if="reviewsLoadError" class="error-text merchant-review-load-error">
          {{ reviewsLoadError }}
          <button type="button" class="merchant-review-retry-btn" @click="retryReviews">
            重试
          </button>
        </p>
        <ul v-else-if="reviewRecords.length" class="merchant-review-list">
          <li v-for="r in reviewRecords" :key="r.id" class="merchant-review-item">
            <div class="merchant-review-head">
              <span class="merchant-review-user">{{ r.userDisplayName }}</span>
              <span class="merchant-review-time muted">{{ formatReviewTime(r.createdAt) }}</span>
              <span class="merchant-review-score" :title="String(r.rating) + ' 分'">{{
                starText(r.rating)
              }}</span>
            </div>
            <p class="merchant-review-body">{{ r.content }}</p>
            <div v-if="r.imageUrls?.length" class="merchant-review-images">
              <a
                v-for="(url, idx) in r.imageUrls"
                :key="idx"
                :href="url"
                target="_blank"
                rel="noopener noreferrer"
                class="merchant-review-img-link"
              >
                <img :src="url" alt="" class="merchant-review-img" @error="onReviewImgError" />
              </a>
            </div>
            <div v-if="r.recommendProducts?.length" class="merchant-review-rec-out">
              <span class="muted">推荐菜：</span>
              <span>{{ r.recommendProducts.map((x) => x.productName).join("、") }}</span>
            </div>
          </li>
        </ul>
        <p
          v-if="!reviewsLoading && !reviewsLoadError && !reviewRecords.length"
          class="muted"
        >
          暂无评价，欢迎首评
        </p>
      </section>
    </template>

    <!-- 待下单面板（当前商家） -->
    <div
      v-if="draftPanelOpen && merchant"
      class="draft-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="待下单"
      @click.self="closeDraftPanel"
    >
      <div class="draft-panel card">
        <div class="draft-panel-head">
          <h3 class="draft-panel-title">待下单 · {{ merchant.name }}</h3>
          <button
            type="button"
            class="draft-panel-close"
            aria-label="关闭待下单"
            @click="closeDraftPanel"
          >
            ×
          </button>
        </div>

        <p v-if="draftPanelLoading" class="muted">加载中…</p>
        <template v-else>
          <p v-if="!draftPayload || !draftPayload.items.length" class="muted">暂无待下单商品</p>
          <ul v-else class="draft-panel-list">
            <li v-for="it in draftPayload.items" :key="it.productId" class="draft-panel-row">
              <div class="draft-line-main">
                <span class="draft-line-name">{{ it.productName }}</span>
                <span class="muted draft-line-unit"
                  >单价 ¥{{ draftLineUnitPrice(it).toFixed(2) }}</span
                >
              </div>
              <div class="draft-line-actions">
                <div class="qty-control">
                  <button
                    type="button"
                    class="qty-btn"
                    :disabled="it.quantity <= 1 || !!draftAdjusting[it.productId]"
                    title="数量为 1 时不可再减"
                    @click="decreaseDraftQty(it.productId)"
                  >
                    -
                  </button>
                  <span class="qty-value">{{ it.quantity }}</span>
                  <button
                    type="button"
                    class="qty-btn"
                    :disabled="!!draftAdjusting[it.productId]"
                    @click="increaseDraftQty(it.productId)"
                  >
                    +
                  </button>
                </div>
                <span class="draft-line-sub"
                  >小计 ¥{{ Number(it.subtotal).toFixed(2) }}</span
                >
                <button
                  type="button"
                  class="delete-cart-btn"
                  :disabled="!!draftRemoving[it.productId] || draftRemoveLoading"
                  @click="openDraftRemoveConfirm(it.productId, it.productName)"
                >
                  {{ draftRemoving[it.productId] ? "…" : "删" }}
                </button>
              </div>
            </li>
          </ul>

          <section
            v-if="draftPayload && draftPayload.items.length"
            class="draft-coupon-block"
          >
            <p class="draft-coupon-title">优惠券</p>
            <p class="draft-coupon-line">
              <span class="draft-coupon-rule">{{
                draftPayload.couponRuleText || "—"
              }}</span>
              <span class="draft-coupon-hint muted">{{ draftPayload.couponHint || "无" }}</span>
            </p>
            <p
              v-if="draftPayload.couponLineStatus === 'applied'"
              class="draft-coupon-amount muted"
            >
              本单减免 ¥{{ Number(draftPayload.couponAmount).toFixed(2) }}
            </p>
          </section>

          <div v-if="draftPayload && draftPayload.items.length" class="draft-pay-bar">
            <div class="draft-pay-sum">
              <span>商品合计 ¥{{ draftGoodsSumFromLines.toFixed(2) }}</span>
              <span>配送费 ¥{{ Number(draftPayload.deliveryFee).toFixed(2) }}</span>
              <span class="draft-pay-total"
                >应付 ¥{{ Number(draftPayload.payableAmount).toFixed(2) }}</span
              >
            </div>
            <div class="draft-pay-actions">
              <button
                type="button"
                class="draft-pay-btn"
                :disabled="payLoading || cancelLoading"
                @click="payDraft"
              >
                {{ payLoading ? "支付中…" : "模拟支付" }}
              </button>
              <button
                type="button"
                class="draft-cancel-btn"
                title="不立即支付，生成待支付订单，30 分钟内需完成支付"
                :disabled="payLoading || cancelLoading"
                @click="cancelAsPendingOrder"
              >
                {{ cancelLoading ? "处理中…" : "取消" }}
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <div
      v-if="draftRemoveConfirmOpen"
      class="merchant-draft-delete-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="merchant-draft-remove-title"
      @click.self="closeDraftRemoveConfirm"
    >
      <div class="merchant-draft-delete-dialog card" @click.stop>
        <p id="merchant-draft-remove-title" class="merchant-draft-delete-title">
          确认从待下单中移除该商品？
        </p>
        <p class="merchant-draft-delete-hint muted">商品仍在店铺中，可再次点击「下单」加入。</p>
        <p v-if="draftRemoveProductName" class="merchant-draft-delete-name muted">
          {{ draftRemoveProductName }}
        </p>
        <div class="merchant-draft-delete-actions">
          <button
            type="button"
            class="merchant-draft-delete-btn-no"
            :disabled="draftRemoveLoading"
            @click="closeDraftRemoveConfirm"
          >
            否
          </button>
          <button
            type="button"
            class="merchant-draft-delete-btn-yes"
            :disabled="draftRemoveLoading"
            @click="confirmRemoveDraftLine"
          >
            {{ draftRemoveLoading ? "…" : "是" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  claimSeckillCoupon,
  fetchMerchant,
  fetchMerchantCategories,
  fetchMerchantProducts,
  fetchMerchantSeckillCoupons
} from "../../api/merchant";
import { fetchClaimedSeckillIds } from "../../api/userCoupon";
import { addCartItem, fetchCartItems, removeCartItem } from "../../api/cart";
import {
  addDraftItem,
  checkoutDraft,
  checkoutPendingDraft,
  decreaseDraftItem,
  fetchMerchantDraft,
  increaseDraftItem,
  removeDraftItem
} from "../../api/merchantDraft";
import { useAuthStore } from "../../store/auth";
import type { DraftItem, DraftMerchantPayload } from "../../types/draft";
import type {
  MerchantListItem,
  ProductCategory,
  ProductListItem,
  SeckillCoupon
} from "../../types/merchant";
import type { MerchantReviewItem } from "../../types/merchantReview";
import {
  fetchMerchantReviews,
  postMerchantReview,
  uploadMerchantReviewImages
} from "../../api/merchantReview";
import { getApiErrorMessage } from "../../utils/apiError";

const route = useRoute();
const router = useRouter();
const merchantId = computed(() => Number(route.params.id));

const loading = ref(true);
const error = ref("");
const merchant = ref<MerchantListItem | null>(null);
const categories = ref<ProductCategory[]>([]);
const products = ref<ProductListItem[]>([]);
const activeCategoryId = ref<number | null>(null);
const addLoading = ref<Record<number, boolean>>({});
const inCart = ref<Record<number, boolean>>({});
const removeLoading = ref<Record<number, boolean>>({});
const auth = useAuthStore();

const draftPayload = ref<DraftMerchantPayload | null>(null);
const draftAddLoading = ref<Record<number, boolean>>({});
const draftPanelOpen = ref(false);
const draftPanelLoading = ref(false);
const draftAdjusting = ref<Record<number, boolean>>({});
const draftRemoving = ref<Record<number, boolean>>({});
const draftRemoveConfirmOpen = ref(false);
const draftRemoveProductId = ref<number | null>(null);
const draftRemoveProductName = ref("");
const draftRemoveLoading = ref(false);
const payLoading = ref(false);
const cancelLoading = ref(false);
const seckillCoupons = ref<SeckillCoupon[]>([]);
const claimLoading = ref<Record<number, boolean>>({});
/** 当前商家下已领取的秒杀券模板 id */
const claimedSeckillIds = ref<number[]>([]);

const shopTab = ref<"products" | "reviews">("products");
const reviewsLoading = ref(false);
const reviewsLoadError = ref("");
/** 当前商家 id 已成功拉取过评价列表时等于该 id，用于懒加载 */
const reviewsLoadedForMerchantId = ref<number | null>(null);
const reviewRecords = ref<MerchantReviewItem[]>([]);
const reviewRating = ref(5);
const reviewContent = ref("");
const reviewImageUrls = ref<string[]>([]);
const reviewImageUploading = ref(false);
const reviewImageInputRef = ref<HTMLInputElement | null>(null);
const reviewRecommendIds = ref<number[]>([]);
/** 推荐菜品列表默认收起，减少写评价区占位 */
const reviewRecommendExpanded = ref(false);
const reviewSubmitting = ref(false);

const reviewRecommendSummaryText = computed(() => {
  const ids = reviewRecommendIds.value;
  if (!ids.length) {
    return "未选择推荐菜";
  }
  const names = ids
    .map((id) => products.value.find((p) => p.id === id)?.name)
    .filter((n): n is string => Boolean(n));
  if (names.length <= 2) {
    return `已选：${names.join("、")}`;
  }
  return `已选 ${names.length} 项：${names.slice(0, 2).join("、")}…`;
});

const draftTotalQty = computed(() => draftPayload.value?.totalQuantity ?? 0);

/** 单价 = 小计÷数量，与后端单价一致且不随展示误显示为行总价 */
function draftLineUnitPrice(it: DraftItem): number {
  const q = Number(it.quantity) || 0;
  const st = Number(it.subtotal);
  if (q > 0 && Number.isFinite(st)) {
    return Math.round((st / q) * 100) / 100;
  }
  return Number(it.price) || 0;
}

/** 商品合计 = 各行（单价×数量）之和，即各行小计之和 */
const draftGoodsSumFromLines = computed(() => {
  const p = draftPayload.value;
  if (!p?.items?.length) return 0;
  return p.items.reduce((acc, it) => acc + (Number(it.subtotal) || 0), 0);
});

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

const filteredProducts = computed(() => {
  if (activeCategoryId.value == null) {
    return products.value;
  }
  return products.value.filter((p) => p.categoryId === activeCategoryId.value);
});

function selectCategory(id: number) {
  activeCategoryId.value = id;
}

function productOutOfStock(p: ProductListItem) {
  return (p.stock ?? 0) < 1;
}

async function loadReviews() {
  const id = merchantId.value;
  if (!Number.isFinite(id) || id < 1) return;
  reviewsLoading.value = true;
  reviewsLoadError.value = "";
  try {
    const page = await fetchMerchantReviews(id, 1, 30);
    reviewRecords.value = page.records;
    reviewsLoadedForMerchantId.value = id;
  } catch (e: unknown) {
    reviewRecords.value = [];
    reviewsLoadedForMerchantId.value = null;
    reviewsLoadError.value = getApiErrorMessage(e, "评价加载失败");
  } finally {
    reviewsLoading.value = false;
  }
}

function retryReviews() {
  reviewsLoadedForMerchantId.value = null;
  void loadReviews();
}

function triggerReviewImagePick() {
  reviewImageInputRef.value?.click();
}

async function onReviewImagesPick(e: Event) {
  const input = e.target as HTMLInputElement;
  const files = input.files;
  if (!files?.length) return;
  const remaining = 6 - reviewImageUrls.value.length;
  if (remaining <= 0) {
    showToast("error", "最多 6 张图片");
    input.value = "";
    return;
  }
  const picked = Array.from(files).slice(0, remaining);
  if (!auth.token) {
    router.push("/auth/login");
    input.value = "";
    return;
  }
  reviewImageUploading.value = true;
  try {
    const urls = await uploadMerchantReviewImages(picked);
    reviewImageUrls.value = [...reviewImageUrls.value, ...urls].slice(0, 6);
  } catch (err: unknown) {
    showToast("error", getApiErrorMessage(err, "图片上传失败"));
  } finally {
    reviewImageUploading.value = false;
    input.value = "";
  }
}

function removeReviewImage(idx: number) {
  reviewImageUrls.value = reviewImageUrls.value.filter((_, i) => i !== idx);
}

async function submitReview() {
  const id = merchantId.value;
  if (!Number.isFinite(id) || id < 1) return;
  const content = reviewContent.value.trim();
  if (!content) {
    showToast("error", "请填写评价内容");
    return;
  }
  if (!auth.token) {
    router.push("/auth/login");
    return;
  }
  if (reviewSubmitting.value) return;
  reviewSubmitting.value = true;
  try {
    const recIds = reviewRecommendIds.value
      .map((x) => Number(x))
      .filter((n) => Number.isFinite(n) && n > 0);
    await postMerchantReview({
      merchantId: id,
      rating: reviewRating.value,
      content,
      imageUrls: reviewImageUrls.value.slice(0, 6),
      recommendProductIds: recIds
    });
    showToast("success", "评价已发布");
    reviewContent.value = "";
    reviewImageUrls.value = [];
    reviewRecommendIds.value = [];
    reviewRating.value = 5;
    await loadReviews();
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "发布失败"));
  } finally {
    reviewSubmitting.value = false;
  }
}

function formatReviewTime(iso: string) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function starText(rating: number) {
  const r = Math.min(5, Math.max(0, Math.round(Number(rating))));
  return "★".repeat(r) + "☆".repeat(5 - r);
}

function onReviewImgError(e: Event) {
  const el = e.target as HTMLImageElement;
  el.style.display = "none";
}

function isSeckillClaimed(couponTemplateId: number) {
  return claimedSeckillIds.value.includes(couponTemplateId);
}

async function syncClaimedSeckillIds() {
  if (!auth.token) {
    claimedSeckillIds.value = [];
    return;
  }
  const id = merchantId.value;
  if (!Number.isFinite(id) || id < 1) return;
  try {
    claimedSeckillIds.value = await fetchClaimedSeckillIds(id);
  } catch {
    claimedSeckillIds.value = [];
  }
}

function formatCouponUntil(iso: string) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

async function claimCoupon(couponId: number) {
  if (!auth.token) {
    router.push("/auth/login");
    return;
  }
  if (!Number.isFinite(couponId) || couponId < 1) return;
  if (claimLoading.value[couponId]) return;
  claimLoading.value[couponId] = true;
  try {
    await claimSeckillCoupon(couponId);
    showToast("success", "领取成功");
    const id = merchantId.value;
    if (Number.isFinite(id) && id >= 1) {
      seckillCoupons.value = await fetchMerchantSeckillCoupons(id);
      await syncClaimedSeckillIds();
    }
    await syncDraft();
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "领取失败"));
  } finally {
    claimLoading.value[couponId] = false;
  }
}

async function syncDraft() {
  if (!auth.token) {
    draftPayload.value = null;
    return;
  }
  const id = merchantId.value;
  if (!Number.isFinite(id) || id < 1) return;
  try {
    draftPayload.value = await fetchMerchantDraft(id);
  } catch {
    draftPayload.value = null;
  }
}

async function addToDraft(productId: number) {
  if (!auth.token) {
    router.push("/auth/login");
    return;
  }
  if (!Number.isFinite(productId) || productId < 1) return;
  if (draftAddLoading.value[productId]) return;
  draftAddLoading.value[productId] = true;
  try {
    await addDraftItem(productId, 1);
    await syncDraft();
    showToast("success", "已加入待下单");
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "加入失败"));
  } finally {
    draftAddLoading.value[productId] = false;
  }
}

async function openDraftPanel() {
  if (!auth.token) {
    router.push("/auth/login");
    return;
  }
  draftPanelOpen.value = true;
  draftPanelLoading.value = true;
  try {
    await syncDraft();
  } finally {
    draftPanelLoading.value = false;
  }
}

function closeDraftPanel() {
  if (draftRemoveLoading.value || payLoading.value || cancelLoading.value) return;
  draftRemoveConfirmOpen.value = false;
  draftPanelOpen.value = false;
}

function openDraftRemoveConfirm(productId: number, productName: string) {
  if (draftRemoving.value[productId]) return;
  draftRemoveProductId.value = productId;
  draftRemoveProductName.value = productName || "";
  draftRemoveConfirmOpen.value = true;
}

function closeDraftRemoveConfirm() {
  if (draftRemoveLoading.value) return;
  draftRemoveConfirmOpen.value = false;
  draftRemoveProductId.value = null;
  draftRemoveProductName.value = "";
}

async function confirmRemoveDraftLine() {
  const pid = draftRemoveProductId.value;
  if (pid == null || draftRemoveLoading.value) return;
  const mid = merchantId.value;
  if (!Number.isFinite(mid) || mid < 1) return;
  draftRemoveLoading.value = true;
  draftRemoving.value[pid] = true;
  try {
    await removeDraftItem(mid, pid);
    await syncDraft();
    draftRemoveConfirmOpen.value = false;
    draftRemoveProductId.value = null;
    draftRemoveProductName.value = "";
    showToast("success", "已从待下单移除");
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "删除失败"));
  } finally {
    draftRemoveLoading.value = false;
    draftRemoving.value[pid] = false;
  }
}

async function increaseDraftQty(productId: number) {
  const mid = merchantId.value;
  if (!Number.isFinite(mid) || mid < 1) return;
  if (draftAdjusting.value[productId]) return;
  draftAdjusting.value[productId] = true;
  try {
    await increaseDraftItem(mid, productId, 1);
    await syncDraft();
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "操作失败"));
  } finally {
    draftAdjusting.value[productId] = false;
  }
}

async function decreaseDraftQty(productId: number) {
  const mid = merchantId.value;
  if (!Number.isFinite(mid) || mid < 1) return;
  const line = draftPayload.value?.items.find((x) => x.productId === productId);
  if (line && line.quantity <= 1) return;
  if (draftAdjusting.value[productId]) return;
  draftAdjusting.value[productId] = true;
  try {
    await decreaseDraftItem(mid, productId, 1);
    await syncDraft();
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "操作失败"));
  } finally {
    draftAdjusting.value[productId] = false;
  }
}

/** 支付成功后后端已更新销量，重新拉取商家与商品列表使页面数字立即刷新 */
async function refreshMerchantCatalog() {
  const id = merchantId.value;
  if (!Number.isFinite(id) || id < 1) return;
  try {
    const [m, prods] = await Promise.all([fetchMerchant(id), fetchMerchantProducts(id)]);
    merchant.value = m;
    products.value = prods;
  } catch {
    /* 忽略刷新失败，避免打断支付成功提示 */
  }
}

async function payDraft() {
  const mid = merchantId.value;
  if (!Number.isFinite(mid) || mid < 1) return;
  if (payLoading.value) return;
  payLoading.value = true;
  try {
    await checkoutDraft(mid);
    showToast("success", "支付成功");
    await syncDraft();
    await refreshMerchantCatalog();
    closeDraftPanel();
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "支付失败"));
  } finally {
    payLoading.value = false;
  }
}

async function cancelAsPendingOrder() {
  const mid = merchantId.value;
  if (!Number.isFinite(mid) || mid < 1) return;
  if (cancelLoading.value) return;
  cancelLoading.value = true;
  try {
    await checkoutPendingDraft(mid);
    showToast("success", "已生成待支付订单，请在 30 分钟内完成支付");
    await syncDraft();
    closeDraftPanel();
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "操作失败"));
  } finally {
    cancelLoading.value = false;
  }
}

async function addToCart(productId: number) {
  if (!auth.token) {
    router.push("/auth/login");
    return;
  }
  if (!Number.isFinite(productId) || productId < 1) return;
  if (addLoading.value[productId]) return;

  addLoading.value[productId] = true;

  try {
    await addCartItem(productId, 1);
    inCart.value[productId] = true;
    showToast("success", "已加入购物车");
    syncCartFlags().catch(() => {});
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "加入失败"));
  } finally {
    addLoading.value[productId] = false;
  }
}

async function removeFromCart(productId: number) {
  if (!auth.token) {
    router.push("/auth/login");
    return;
  }
  if (!Number.isFinite(productId) || productId < 1) return;
  if (removeLoading.value[productId]) return;
  removeLoading.value[productId] = true;

  try {
    await removeCartItem(productId);
    inCart.value[productId] = false;
    showToast("success", "已从购物车删除");
    await syncCartFlags();
  } catch (e: unknown) {
    showToast("error", getApiErrorMessage(e, "删除失败"));
  } finally {
    removeLoading.value[productId] = false;
  }
}

function toggleCart(productId: number) {
  if (inCart.value[productId]) {
    return removeFromCart(productId);
  }
  return addToCart(productId);
}

async function syncCartFlags() {
  if (!auth.token) return;
  const cartItems = await fetchCartItems();
  const next: Record<number, boolean> = {};
  for (const it of cartItems) {
    if (it && typeof it.productId === "number") {
      next[it.productId] = true;
    }
  }
  inCart.value = next;
}

async function load() {
  loading.value = true;
  error.value = "";
  reviewRecords.value = [];
  reviewsLoadError.value = "";
  reviewsLoadedForMerchantId.value = null;
  const id = merchantId.value;
  if (!Number.isFinite(id) || id < 1) {
    error.value = "无效的商家";
    loading.value = false;
    return;
  }
  try {
    const [m, cats, prods] = await Promise.all([
      fetchMerchant(id),
      fetchMerchantCategories(id),
      fetchMerchantProducts(id)
    ]);
    merchant.value = m;
    categories.value = cats;
    products.value = prods;
    try {
      seckillCoupons.value = await fetchMerchantSeckillCoupons(id);
    } catch {
      seckillCoupons.value = [];
    }
    activeCategoryId.value = cats.length ? cats[0].id : null;

    syncCartFlags().catch(() => {});
    syncDraft().catch(() => {});
    syncClaimedSeckillIds().catch(() => {});
  } catch (e: unknown) {
    error.value = getApiErrorMessage(e, "加载失败");
    merchant.value = null;
  } finally {
    loading.value = false;
  }
}

function onGlobalKeydown(ev: KeyboardEvent) {
  if (ev.key !== "Escape") return;
  if (draftRemoveConfirmOpen.value) {
    closeDraftRemoveConfirm();
    ev.preventDefault();
    return;
  }
  if (draftPanelOpen.value) {
    closeDraftPanel();
    ev.preventDefault();
  }
}

onMounted(() => {
  window.addEventListener("keydown", onGlobalKeydown);
  void load();
});
onUnmounted(() => {
  window.removeEventListener("keydown", onGlobalKeydown);
});

watch(merchantId, () => {
  shopTab.value = "products";
  reviewRecommendExpanded.value = false;
  void load();
});

watch([shopTab, loading, merchant], () => {
  if (shopTab.value !== "reviews") return;
  if (loading.value) return;
  if (!merchant.value) return;
  const id = merchantId.value;
  if (!Number.isFinite(id) || id < 1) return;
  if (reviewsLoadedForMerchantId.value === id) return;
  void loadReviews();
});

watch(
  () => auth.token,
  (t) => {
    if (t) {
      syncClaimedSeckillIds().catch(() => {});
    } else {
      claimedSeckillIds.value = [];
    }
  }
);
</script>

<style scoped>
.merchant-draft-delete-overlay {
  position: fixed;
  inset: 0;
  z-index: 10050;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.merchant-draft-delete-dialog {
  max-width: 360px;
  width: 100%;
  padding: 20px 22px;
}
.merchant-draft-delete-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
  text-align: center;
}
.merchant-draft-delete-hint {
  margin: 0 0 8px;
  font-size: 13px;
  text-align: center;
  line-height: 1.4;
}
.merchant-draft-delete-name {
  margin: 0 0 18px;
  font-size: 14px;
  text-align: center;
  line-height: 1.4;
}
.merchant-draft-delete-actions {
  display: flex;
  gap: 10px;
  justify-content: stretch;
}
.merchant-draft-delete-actions button {
  flex: 1;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.merchant-draft-delete-btn-no {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}
.merchant-draft-delete-btn-no:hover:not(:disabled) {
  background: #f9fafb;
}
.merchant-draft-delete-btn-yes {
  border: none;
  background: #dc2626;
  color: #fff;
}
.merchant-draft-delete-btn-yes:hover:not(:disabled) {
  background: #b91c1c;
}
.merchant-draft-delete-btn-no:disabled,
.merchant-draft-delete-btn-yes:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.shop-tabs {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  margin-top: 0;
  margin-bottom: 12px;
  border-radius: 10px;
}
.shop-tab {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  color: #6b7280;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.shop-tab:hover {
  background: #f3f4f6;
}
.shop-tab.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}
.merchant-reviews {
  margin-top: 16px;
}
.merchant-reviews-title {
  margin: 0 0 12px;
  font-size: 17px;
}
.merchant-review-form {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}
.merchant-review-form-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
}
.merchant-review-rating-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.merchant-review-stars {
  display: flex;
  gap: 2px;
}
.merchant-review-star-btn {
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: #d1d5db;
  padding: 0 2px;
}
.merchant-review-star-btn.on {
  color: #f59e0b;
}
.merchant-review-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  margin-bottom: 8px;
}
.merchant-review-hint {
  margin: 0 0 6px;
  font-size: 13px;
}
.merchant-review-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}
.merchant-review-image-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.merchant-review-img-count {
  font-size: 13px;
}
.merchant-review-pick-btn {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
}
.merchant-review-pick-btn:hover:not(:disabled) {
  background: #f9fafb;
}
.merchant-review-pick-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.merchant-review-previews {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}
.merchant-review-preview-cell {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
}
.merchant-review-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.merchant-review-preview-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.65);
  color: #fff;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}
.merchant-review-preview-remove:hover {
  background: rgba(17, 24, 39, 0.85);
}
.merchant-review-rec-panel {
  margin-bottom: 12px;
}
.merchant-review-rec-toggle {
  display: flex;
  width: 100%;
  box-sizing: border-box;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}
.merchant-review-rec-toggle:hover {
  background: #f3f4f6;
}
.merchant-review-rec-chevron {
  flex-shrink: 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1;
}
.merchant-review-rec-collapsed-hint {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.45;
}
.merchant-review-rec-scroll {
  max-height: 220px;
  overflow-y: auto;
  margin-top: 8px;
  margin-bottom: 0;
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}
.merchant-review-rec-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 4px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  border-radius: 6px;
}
.merchant-review-rec-row:hover {
  background: #f9fafb;
}
.merchant-review-rec-row input[type="checkbox"] {
  width: auto;
  max-width: none;
  margin-top: 3px;
  flex: 0 0 auto;
  padding: 0;
  cursor: pointer;
}
.merchant-review-rec-row span {
  flex: 1 1 auto;
  min-width: 0;
  line-height: 1.4;
}
.merchant-review-rec-empty {
  margin: 8px 0;
  font-size: 13px;
}
.merchant-review-submit {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.merchant-review-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.merchant-review-login-hint {
  margin: 0 0 16px;
}
.merchant-review-login-hint a {
  color: #2563eb;
}
.merchant-review-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.merchant-review-item {
  padding: 14px 0;
  border-bottom: 1px solid #f3f4f6;
}
.merchant-review-item:last-child {
  border-bottom: none;
}
.merchant-review-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  margin-bottom: 8px;
}
.merchant-review-user {
  font-weight: 600;
  color: #111827;
}
.merchant-review-time {
  font-size: 13px;
}
.merchant-review-score {
  margin-left: auto;
  color: #f59e0b;
  letter-spacing: 1px;
}
.merchant-review-body {
  margin: 0 0 10px;
  font-size: 14px;
  line-height: 1.55;
  color: #374151;
  white-space: pre-wrap;
}
.merchant-review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.merchant-review-img-link {
  display: block;
  width: 88px;
  height: 88px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f3f4f6;
  background: #f9fafb;
}
.merchant-review-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.merchant-review-rec-out {
  font-size: 13px;
  color: #6b7280;
}
.merchant-review-field-label {
  display: block;
  margin: 10px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
}
.product-stock-out {
  color: #dc2626;
  font-weight: 600;
}
.merchant-review-load-error {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.merchant-review-retry-btn {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  cursor: pointer;
}
.merchant-review-retry-btn:hover {
  background: #f9fafb;
}
</style>
