<template>
  <div class="layout">
    <header class="header">
      <h1>即刻开饭</h1>
      <nav class="header-nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/orders">订单</RouterLink>
        <template v-if="navAuthLoading">
          <span class="nav-user nav-user--loading muted">加载中…</span>
        </template>
        <template v-else-if="isLoggedIn">
          <RouterLink to="/coupons">优惠券</RouterLink>
          <RouterLink to="/cart">购物车</RouterLink>
          <span class="nav-user">{{ auth.userInfo?.nickname || auth.userInfo?.mobile || "已登录" }}</span>
          <RouterLink
            to="/profile"
            class="nav-avatar-link"
            title="个人主页"
            aria-label="个人主页"
          >
            <img
              v-if="auth.userInfo?.avatarUrl && !navAvatarBroken"
              :src="auth.userInfo.avatarUrl"
              alt=""
              class="nav-avatar-img"
              @error="onNavAvatarError"
            />
            <span v-else class="nav-avatar-placeholder">{{ navAvatarLetter }}</span>
          </RouterLink>
        </template>
        <template v-else>
          <RouterLink to="/auth/login" class="nav-auth-btn nav-auth-btn--login">登录</RouterLink>
          <RouterLink to="/auth/register" class="nav-auth-btn nav-auth-btn--register">注册</RouterLink>
        </template>
      </nav>
    </header>
    <main class="main">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useAuthStore } from "./store/auth";

const route = useRoute();
const auth = useAuthStore();
const navAvatarBroken = ref(false);

/** 有 token 且已拉到用户信息才算登录，避免仅有 token、userInfo 为空时顶栏出现「已登录」和「?」 */
const isLoggedIn = computed(() => {
  const t = auth.token;
  return typeof t === "string" && t.trim().length > 0 && auth.userInfo != null;
});

/** 有 token 但尚未拿到用户信息（刷新后拉取 /me 期间） */
const navAuthLoading = computed(() => {
  const t = auth.token;
  return typeof t === "string" && t.trim().length > 0 && auth.userInfo == null;
});

const navAvatarLetter = computed(() => {
  const u = auth.userInfo;
  if (!u) return "?";
  const n = u.nickname?.trim();
  if (n) return n.slice(0, 1);
  if (u.mobile?.length) return u.mobile.slice(-1);
  return "?";
});

watch(
  () => auth.userInfo?.avatarUrl,
  () => {
    navAvatarBroken.value = false;
  }
);

onMounted(() => {
  const t = auth.token?.trim();
  if (t && !auth.userInfo) {
    auth.loadMe().catch(() => {
      auth.logout();
    });
  }
});

function onNavAvatarError() {
  navAvatarBroken.value = true;
}
</script>
