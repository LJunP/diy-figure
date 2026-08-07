/**
 * Vue Router 配置
 *
 * 路由结构:
 * / - 用户端(设计工作台/系列/订单/支付)
 * /admin - 运营后台(终审/报价/质检/发货/取消处理)
 *
 * 路由守卫:
 * - 需要登录的页面:检查 token,未登录跳转 /login
 * - 管理员页面:检查 role=ADMIN,非管理员跳转首页
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', requiresAuth: false }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'series',
        name: 'SeriesList',
        component: () => import('@/views/series/SeriesList.vue'),
        meta: { title: '我的系列', requiresAuth: true }
      },
      {
        path: 'series/create',
        name: 'SeriesCreate',
        component: () => import('@/views/series/SeriesCreate.vue'),
        meta: { title: '创建系列', requiresAuth: true }
      },
      {
        path: 'series/:id',
        name: 'SeriesDetail',
        component: () => import('@/views/series/SeriesDetail.vue'),
        meta: { title: '系列详情', requiresAuth: true }
      },
      {
        path: 'canvases/:id',
        name: 'CanvasDesign',
        component: () => import('@/views/canvas/CanvasDesign.vue'),
        meta: { title: 'AI 设计工作台', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'payment/:orderId',
        name: 'PaymentPage',
        component: () => import('@/views/payment/PaymentPage.vue'),
        meta: { title: '支付', requiresAuth: true }
      },
      {
        path: 'refill/:orderId',
        name: 'RefillPage',
        component: () => import('@/views/refill/RefillPage.vue'),
        meta: { title: '补购', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '404', requiresAuth: false }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '运营后台' }
      },
      {
        path: 'reviews',
        name: 'AdminReviewList',
        component: () => import('@/views/admin/ReviewList.vue'),
        meta: { title: '待终审列表' }
      },
      {
        path: 'quotes',
        name: 'AdminQuoteList',
        component: () => import('@/views/admin/QuoteList.vue'),
        meta: { title: '待报价列表' }
      },
      {
        path: 'production',
        name: 'AdminProduction',
        component: () => import('@/views/admin/ProductionManage.vue'),
        meta: { title: '生产物流管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// ===== 全局前置守卫 =====
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - DIY 定制盲盒` : 'DIY 定制盲盒手办平台'

  const userStore = useUserStore()

  // 需要登录但未登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 需要管理员权限但非管理员
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next({ name: 'Home' })
    return
  }

  next()
})

export default router
