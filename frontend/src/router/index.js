import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth.js'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/app',
    name: 'App',
    component: () => import('../views/AppView.vue'),
    meta: { requiresAuth: true, role: 'USER' }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/AdminView.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/',
    redirect: '/app'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const { isLoggedIn, isAdmin } = useAuth()

  if (to.meta.requiresAuth && !isLoggedIn.value) {
    return { name: 'Login' }
  }

  if (!to.meta.requiresAuth && isLoggedIn.value) {
    return isAdmin.value ? { name: 'Admin' } : { name: 'App' }
  }

  if (to.meta.role === 'USER' && isAdmin.value) {
    return { name: 'Admin' }
  }

  if (to.meta.role === 'ADMIN' && !isAdmin.value) {
    return { name: 'App' }
  }
})

export default router
