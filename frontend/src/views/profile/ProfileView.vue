<template>
  <div class="profile">
    <p class="profile-back">
      <RouterLink to="/">← 返回首页</RouterLink>
    </p>

    <p v-if="error" class="error-text">{{ error }}</p>

    <template v-else-if="auth.token && profileReady">
      <section class="profile-card card">
        <h2 class="profile-title">个人主页</h2>
        <div class="profile-head">
          <div class="profile-avatar-wrap">
            <img
              v-if="auth.userInfo?.avatarUrl && !avatarImgBroken"
              :src="auth.userInfo.avatarUrl"
              alt=""
              class="profile-avatar-img"
              @error="onAvatarError"
            />
            <div v-else class="profile-avatar-placeholder" aria-hidden="true">
              {{ avatarLetter }}
            </div>
          </div>
          <div class="profile-meta">
            <p class="profile-name">{{ auth.userInfo?.nickname || "未设置昵称" }}</p>
            <p class="profile-mobile muted">手机 {{ auth.userInfo?.mobile || "—" }}</p>
            <input
              ref="avatarInputRef"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="profile-file-input"
              @change="onAvatarPick"
            />
            <button
              type="button"
              class="profile-upload-btn"
              :disabled="avatarUploading"
              @click="triggerAvatarPick"
            >
              {{ avatarUploading ? "上传中…" : "更换头像" }}
            </button>
          </div>
        </div>
        <div class="profile-card-footer">
          <button type="button" class="profile-logout-btn" @click="logout">退出登录</button>
        </div>
      </section>

      <section class="profile-reviews card">
        <h3 class="profile-reviews-title">我的评价</h3>
        <p v-if="reviewsLoading && !reviewRecords.length" class="muted">加载中…</p>
        <ul v-else-if="reviewRecords.length" class="profile-review-list">
          <li v-for="r in reviewRecords" :key="r.id" class="profile-review-item">
            <div class="profile-review-top">
              <RouterLink
                v-if="r.merchantId"
                :to="'/merchants/' + r.merchantId"
                class="profile-review-merchant"
              >
                {{ r.merchantName || "商家" }}
              </RouterLink>
              <span v-else class="muted">{{ r.merchantName || "—" }}</span>
              <span class="profile-review-time muted">{{ formatTime(r.createdAt) }}</span>
              <span class="profile-review-stars" :title="String(r.rating) + ' 分'">{{
                starText(r.rating)
              }}</span>
            </div>
            <p class="profile-review-body">{{ r.content }}</p>
            <div v-if="r.imageUrls?.length" class="profile-review-images">
              <a
                v-for="(url, idx) in r.imageUrls"
                :key="idx"
                :href="url"
                target="_blank"
                rel="noopener noreferrer"
                class="profile-review-img-link"
              >
                <img :src="url" alt="" class="profile-review-img" @error="onReviewImgError" />
              </a>
            </div>
            <div v-if="r.recommendProducts?.length" class="profile-review-rec">
              <span class="muted">推荐菜：</span>
              <span>{{ r.recommendProducts.map((x) => x.productName).join("、") }}</span>
            </div>
          </li>
        </ul>
        <p v-else-if="!reviewsLoading" class="muted">暂无评价</p>
        <div v-if="reviewHasMore" class="profile-load-more">
          <button
            type="button"
            class="profile-more-btn"
            :disabled="reviewsLoading"
            @click="loadMoreReviews"
          >
            {{ reviewsLoading ? "加载中…" : "加载更多" }}
          </button>
        </div>
      </section>
    </template>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMyReviews, uploadUserAvatar } from "../../api/userProfile";
import { useAuthStore } from "../../store/auth";
import type { MerchantReviewItem } from "../../types/merchantReview";
import { getApiErrorMessage } from "../../utils/apiError";

const auth = useAuthStore();
const router = useRouter();

function logout() {
  auth.logout();
  router.push("/");
}

const profileReady = ref(false);
const error = ref("");
const avatarInputRef = ref<HTMLInputElement | null>(null);
const avatarUploading = ref(false);
const avatarImgBroken = ref(false);

const reviewRecords = ref<MerchantReviewItem[]>([]);
const reviewsLoading = ref(false);
const reviewPageNo = ref(1);
const reviewTotal = ref(0);
const pageSize = 10;

const avatarLetter = computed(() => {
  const n = auth.userInfo?.nickname?.trim();
  if (n) return n.slice(0, 1);
  const m = auth.userInfo?.mobile;
  if (m && m.length) return m.slice(-1);
  return "?";
});

const reviewHasMore = computed(() => {
  return reviewRecords.value.length < reviewTotal.value;
});

function triggerAvatarPick() {
  avatarInputRef.value?.click();
}

function onAvatarError() {
  avatarImgBroken.value = true;
}

async function onAvatarPick(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";
  if (!file) return;
  avatarUploading.value = true;
  avatarImgBroken.value = false;
  try {
    const info = await uploadUserAvatar(file);
    auth.setUserInfo(info);
  } catch (err: unknown) {
    error.value = getApiErrorMessage(err, "头像上传失败");
  } finally {
    avatarUploading.value = false;
  }
}

function formatTime(iso: string) {
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

async function loadReviewsPage(append: boolean) {
  if (!auth.token) return;
  reviewsLoading.value = true;
  error.value = "";
  try {
    const page = await fetchMyReviews(reviewPageNo.value, pageSize);
    reviewTotal.value = page.total;
    if (append) {
      reviewRecords.value = reviewRecords.value.concat(page.records);
    } else {
      reviewRecords.value = page.records;
    }
  } catch (e: unknown) {
    error.value = getApiErrorMessage(e, "评价加载失败");
    if (!append) reviewRecords.value = [];
  } finally {
    reviewsLoading.value = false;
  }
}

async function loadMoreReviews() {
  if (!reviewHasMore.value || reviewsLoading.value) return;
  reviewPageNo.value += 1;
  await loadReviewsPage(true);
}

onMounted(async () => {
  try {
    await auth.loadMe();
    profileReady.value = true;
    reviewPageNo.value = 1;
    await loadReviewsPage(false);
  } catch (e: unknown) {
    error.value = getApiErrorMessage(e, "加载失败");
    profileReady.value = true;
  }
});
</script>

<style scoped>
.profile {
  max-width: 720px;
  margin: 0 auto;
}
.profile-back {
  margin: 0 0 12px;
}
.profile-card {
  max-width: none;
  margin-bottom: 16px;
}
.profile-title {
  margin: 0 0 12px;
  font-size: 18px;
}
.profile-card-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 4px;
}
.profile-logout-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: #dc2626;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.profile-logout-btn:hover {
  background: #b91c1c;
}
.profile-logout-btn:active {
  background: #991b1b;
}
.profile-head {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.profile-avatar-wrap {
  flex-shrink: 0;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #e5e7eb;
  background: #f3f4f6;
}
.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.profile-avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  color: #6b7280;
  background: linear-gradient(145deg, #e0e7ff, #fae8ff);
}
.profile-meta {
  flex: 1;
  min-width: 0;
}
.profile-name {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 700;
  color: #111827;
}
.profile-mobile {
  margin: 0 0 12px;
  font-size: 14px;
}
.profile-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}
.profile-upload-btn {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.profile-upload-btn:hover:not(:disabled) {
  background: #f9fafb;
}
.profile-upload-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
.profile-reviews {
  max-width: none;
}
.profile-reviews-title {
  margin: 0 0 14px;
  font-size: 17px;
}
.profile-review-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.profile-review-item {
  padding: 14px 0;
  border-bottom: 1px solid #f3f4f6;
}
.profile-review-item:last-child {
  border-bottom: none;
}
.profile-review-top {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  margin-bottom: 8px;
}
.profile-review-merchant {
  font-weight: 600;
  color: #2563eb;
  text-decoration: none;
}
.profile-review-merchant:hover {
  text-decoration: underline;
}
.profile-review-time {
  font-size: 13px;
}
.profile-review-stars {
  margin-left: auto;
  color: #f59e0b;
  letter-spacing: 1px;
}
.profile-review-body {
  margin: 0 0 10px;
  font-size: 14px;
  line-height: 1.55;
  color: #374151;
  white-space: pre-wrap;
}
.profile-review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.profile-review-img-link {
  display: block;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #f3f4f6;
  background: #f9fafb;
}
.profile-review-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.profile-review-rec {
  font-size: 13px;
  color: #6b7280;
}
.profile-load-more {
  margin-top: 12px;
  text-align: center;
}
.profile-more-btn {
  padding: 8px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 14px;
  cursor: pointer;
}
.profile-more-btn:hover:not(:disabled) {
  background: #f9fafb;
}
.profile-more-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
