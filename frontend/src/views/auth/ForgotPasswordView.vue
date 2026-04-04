<template>
  <section class="card auth-card">
    <div
      v-if="toast.visible"
      class="toast"
      :class="toast.type"
      role="status"
      aria-live="polite"
    >
      {{ toast.message }}
    </div>
    <h2 class="auth-title">重置密码</h2>
    <p class="auth-lead">
      请输入注册手机号与新密码。若手机号未注册，请先注册账号。
    </p>
    <form class="auth-form" :aria-busy="submitting" @submit.prevent="handleReset">
      <div class="form-row auth-field">
        <label class="auth-label" for="forgot-mobile">手机号</label>
        <input
          id="forgot-mobile"
          v-model="mobile"
          class="auth-input"
          type="tel"
          name="mobile"
          maxlength="11"
          inputmode="numeric"
          placeholder="11 位手机号"
          autocomplete="tel"
          :disabled="submitting"
        />
      </div>
      <div class="form-row auth-field">
        <label class="auth-label" for="forgot-new-password">新密码</label>
        <input
          id="forgot-new-password"
          v-model="newPassword"
          class="auth-input"
          type="password"
          name="newPassword"
          placeholder="6～32 位"
          autocomplete="new-password"
          :disabled="submitting"
        />
      </div>
      <div class="form-row auth-field">
        <label class="auth-label" for="forgot-confirm-password">确认新密码</label>
        <input
          id="forgot-confirm-password"
          v-model="confirmPassword"
          class="auth-input"
          type="password"
          name="confirmPassword"
          placeholder="再次输入新密码"
          autocomplete="new-password"
          :disabled="submitting"
        />
      </div>
      <div class="form-row auth-actions">
        <button
          class="auth-submit"
          type="submit"
          :disabled="submitting"
          :aria-busy="submitting"
        >
          {{ submitting ? "提交中…" : "确认重置" }}
        </button>
      </div>
    </form>
    <p class="auth-links">
      <RouterLink to="/auth/login">返回登录</RouterLink>
      <span class="auth-links-sep" aria-hidden="true">·</span>
      <RouterLink to="/auth/register">去注册</RouterLink>
    </p>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { resetPasswordApi } from "../../api/auth";
import { getApiErrorMessage } from "../../utils/apiError";

const MOBILE_RE = /^1\d{10}$/;

const router = useRouter();
const mobile = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const submitting = ref(false);

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
  }, 2400);
}

function validateClient(): boolean {
  const m = mobile.value.trim();
  if (!m) {
    showToast("error", "请输入手机号");
    return false;
  }
  if (!MOBILE_RE.test(m)) {
    showToast("error", "手机号格式不正确");
    return false;
  }
  if (!newPassword.value) {
    showToast("error", "请输入新密码");
    return false;
  }
  if (newPassword.value.length < 6 || newPassword.value.length > 32) {
    showToast("error", "新密码长度应为 6～32 位");
    return false;
  }
  if (!confirmPassword.value) {
    showToast("error", "请再次输入新密码");
    return false;
  }
  if (newPassword.value !== confirmPassword.value) {
    showToast("error", "两次输入的新密码不一致");
    return false;
  }
  return true;
}

async function handleReset() {
  if (submitting.value) return;
  if (!validateClient()) return;
  submitting.value = true;
  try {
    await resetPasswordApi({
      mobile: mobile.value.trim(),
      newPassword: newPassword.value
    });
    showToast("success", "密码已重置，请使用新密码登录");
    window.setTimeout(() => {
      router.push("/auth/login");
    }, 900);
  } catch (err: unknown) {
    showToast("error", getApiErrorMessage(err, "重置失败"));
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
@keyframes auth-card-in {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes auth-fade-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes auth-accent-pulse {
  from {
    opacity: 0.86;
  }
  to {
    opacity: 0.98;
  }
}

@keyframes auth-bar-shimmer {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 200% 50%;
  }
}

@keyframes auth-orb-drift {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 0.72;
  }
  100% {
    transform: translate(12px, -10px) scale(1.08);
    opacity: 0.92;
  }
}

@keyframes auth-shine-sweep {
  from {
    left: -120%;
    opacity: 0.85;
  }
  to {
    left: 160%;
    opacity: 0;
  }
}

.auth-card {
  position: relative;
  isolation: isolate;
  max-width: 400px;
  margin: 0 auto;
  padding: clamp(22px, 4vw, 28px) clamp(20px, 4vw, 26px);
  overflow: hidden;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(ellipse 110% 85% at 100% -5%, rgba(59, 130, 246, 0.2), transparent 52%),
    radial-gradient(ellipse 95% 75% at -8% 105%, rgba(251, 146, 60, 0.12), transparent 55%),
    linear-gradient(165deg, #ffffff 0%, #f1f5f9 38%, #e0f2fe 68%, #fff7ed 100%);
  border: 1px solid rgba(37, 99, 235, 0.28);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.55) inset,
    0 1px 2px rgba(28, 25, 23, 0.05),
    0 8px 28px rgba(37, 99, 235, 0.12),
    0 20px 56px rgba(14, 116, 233, 0.14),
    0 0 80px rgba(37, 99, 235, 0.06);
  transition:
    box-shadow var(--duration-normal) var(--ease-out),
    border-color var(--duration-normal) var(--ease-out);
  animation: auth-card-in 0.52s var(--ease-out-expo) both;
}

.auth-card > * {
  position: relative;
  z-index: 1;
}

.auth-card::before {
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
  opacity: 0.96;
  animation:
    auth-accent-pulse 5s ease-in-out infinite alternate,
    auth-bar-shimmer 7s linear infinite;
}

.auth-card::after {
  content: "";
  position: absolute;
  pointer-events: none;
  right: -18%;
  top: 14%;
  z-index: 0;
  width: min(340px, 95vw);
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(
    circle at 40% 40%,
    rgba(96, 165, 250, 0.35) 0%,
    rgba(37, 99, 235, 0.12) 42%,
    transparent 68%
  );
  filter: blur(0.5px);
  animation: auth-orb-drift 16s ease-in-out infinite alternate;
}

.auth-card:focus-within {
  border-color: rgba(37, 99, 235, 0.48);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.6) inset,
    0 1px 2px rgba(28, 25, 23, 0.06),
    0 12px 36px rgba(37, 99, 235, 0.18),
    0 24px 64px rgba(14, 165, 233, 0.2),
    0 0 100px rgba(37, 99, 235, 0.1);
}

.auth-title {
  margin: 0 0 8px;
  padding-top: 4px;
  font-size: clamp(1.35rem, 3.2vw, 1.6rem);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.03em;
  text-align: center;
  background: linear-gradient(118deg, #1e3a8a 0%, #2563eb 28%, #0ea5e9 55%, #ea580c 88%);
  background-size: 140% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  filter: drop-shadow(0 3px 18px rgba(37, 99, 235, 0.22));
  animation: auth-fade-up 0.42s var(--ease-out-expo) 0.06s both;
}

.auth-lead {
  margin: 0 0 22px;
  font-size: 13px;
  line-height: 1.55;
  text-align: center;
  color: #64748b;
  animation: auth-fade-up 0.42s var(--ease-out-expo) 0.11s both;
}

.auth-form {
  margin: 0;
}

.auth-card .form-row.auth-field {
  margin-bottom: 16px;
}

.auth-card .form-row.auth-actions {
  margin-top: 8px;
  margin-bottom: 0;
}

.auth-form .auth-field:nth-child(1) {
  animation: auth-fade-up 0.42s var(--ease-out-expo) 0.17s both;
}

.auth-form .auth-field:nth-child(2) {
  animation: auth-fade-up 0.42s var(--ease-out-expo) 0.23s both;
}

.auth-form .auth-field:nth-child(3) {
  animation: auth-fade-up 0.42s var(--ease-out-expo) 0.29s both;
}

.auth-form .auth-actions {
  animation: auth-fade-up 0.42s var(--ease-out-expo) 0.35s both;
}

.auth-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #334155;
}

.auth-input {
  font-size: 15px;
  line-height: 1.45;
  border-width: 1px;
  border-color: rgba(37, 99, 235, 0.22);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 1px 2px rgba(37, 99, 235, 0.07);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out),
    opacity var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out);
}

.auth-input:hover:not(:disabled) {
  border-color: rgba(37, 99, 235, 0.32);
}

.auth-input:focus {
  border-color: rgba(37, 99, 235, 0.75);
  box-shadow:
    inset 0 1px 2px rgba(37, 99, 235, 0.06),
    0 0 0 3px rgba(37, 99, 235, 0.22),
    0 0 28px rgba(14, 165, 233, 0.2);
  background: #fff;
}

.auth-input:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.auth-submit {
  position: relative;
  overflow: hidden;
  width: 100%;
  margin-top: 4px;
  padding: 12px 18px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.03em;
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  background: linear-gradient(
    135deg,
    #1e3a8a 0%,
    #1d4ed8 22%,
    #2563eb 48%,
    #0ea5e9 78%,
    #38bdf8 100%
  );
  background-size: 160% 160%;
  box-shadow:
    0 3px 12px rgba(30, 58, 138, 0.45),
    0 8px 28px rgba(37, 99, 235, 0.38),
    0 0 40px rgba(14, 165, 233, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.28),
    inset 0 -1px 0 rgba(15, 23, 42, 0.12);
  transition:
    filter var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out),
    opacity var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out),
    background-position var(--duration-normal) var(--ease-out);
}

.auth-submit::before {
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

.auth-submit:hover:not(:disabled) {
  filter: brightness(1.08) saturate(1.05);
  transform: translateY(-2px);
  background-position: 88% 50%;
  box-shadow:
    0 6px 16px rgba(30, 58, 138, 0.42),
    0 14px 40px rgba(37, 99, 235, 0.48),
    0 0 52px rgba(14, 165, 233, 0.32),
    inset 0 1px 0 rgba(255, 255, 255, 0.32),
    inset 0 -1px 0 rgba(15, 23, 42, 0.1);
}

.auth-submit:hover:not(:disabled)::before {
  animation: auth-shine-sweep 0.75s var(--ease-out-expo) forwards;
}

.auth-submit:active:not(:disabled) {
  transform: translateY(0) scale(0.992);
  filter: brightness(1.02);
}

.auth-submit:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(14, 165, 233, 0.55),
    0 0 0 6px rgba(37, 99, 235, 0.18),
    0 10px 36px rgba(37, 99, 235, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.auth-submit:disabled {
  opacity: 0.72;
  cursor: not-allowed;
  transform: none;
  filter: grayscale(0.15);
  box-shadow: none;
}

.auth-links {
  margin: 22px 0 0;
  padding-top: 18px;
  border-top: 1px solid rgba(37, 99, 235, 0.18);
  box-shadow: 0 -1px 0 rgba(255, 255, 255, 0.65);
  text-align: center;
  font-size: 13px;
  line-height: 1.65;
  color: #64748b;
  animation: auth-fade-up 0.42s var(--ease-out-expo) 0.41s both;
}

.auth-links a {
  font-weight: 600;
  color: #2563eb;
  text-decoration: none;
  border-radius: 4px;
  transition:
    color var(--duration-fast) var(--ease-out),
    text-shadow var(--duration-fast) var(--ease-out);
}

.auth-links a:hover {
  color: #1d4ed8;
  text-decoration: underline;
  text-shadow: 0 0 12px rgba(37, 99, 235, 0.25);
}

.auth-links a:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.auth-links-sep {
  margin: 0 10px;
  color: rgba(37, 99, 235, 0.28);
  font-weight: 400;
}

@media (prefers-reduced-motion: reduce) {
  .auth-card,
  .auth-title,
  .auth-lead,
  .auth-form .auth-field,
  .auth-form .auth-actions,
  .auth-links {
    animation: none !important;
  }

  .auth-card::before {
    animation: none;
    background-size: 100% 100%;
  }

  .auth-card::after {
    animation: none;
  }

  .auth-submit::before {
    animation: none !important;
  }

  .auth-submit:hover:not(:disabled) {
    background-position: 50% 50%;
  }

  .auth-card,
  .auth-input,
  .auth-submit {
    transition: none;
  }
}
</style>
