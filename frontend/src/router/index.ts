import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/home/HomeView.vue";
import MerchantDetailView from "../views/merchant/MerchantDetailView.vue";
import LoginView from "../views/auth/LoginView.vue";
import RegisterView from "../views/auth/RegisterView.vue";
import ForgotPasswordView from "../views/auth/ForgotPasswordView.vue";
import OrderListView from "../views/order/OrderListView.vue";
import CartView from "../views/cart/CartView.vue";
import UserCouponsView from "../views/coupon/UserCouponsView.vue";
import ProfileView from "../views/profile/ProfileView.vue";
import { useAuthStore } from "../store/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: HomeView },
    { path: "/merchants/:id", component: MerchantDetailView },
    { path: "/auth/login", component: LoginView },
    { path: "/auth/register", component: RegisterView },
    { path: "/auth/forgot-password", component: ForgotPasswordView },
    { path: "/orders", component: OrderListView, meta: { requiresAuth: true } },
    { path: "/cart", component: CartView, meta: { requiresAuth: true } },
    { path: "/coupons", component: UserCouponsView, meta: { requiresAuth: true } },
    { path: "/profile", component: ProfileView, meta: { requiresAuth: true } }
  ]
});

router.beforeEach((to) => {
  const store = useAuthStore();
  if (to.meta.requiresAuth && !store.token) {
    return "/auth/login";
  }
  return true;
});

export default router;
