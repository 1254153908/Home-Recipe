import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/RecipeList.vue'),
    meta: { tab: 'recipes', requiresAuth: true }
  },
  {
    path: '/recipe/new',
    name: 'RecipeCreate',
    component: () => import('../views/RecipeForm.vue'),
    meta: { tab: 'recipes', requiresAuth: true }
  },
  {
    path: '/recipe/:id',
    name: 'RecipeDetail',
    component: () => import('../views/RecipeDetail.vue'),
    meta: { tab: 'recipes', requiresAuth: true }
  },
  {
    path: '/recipe/:id/edit',
    name: 'RecipeEdit',
    component: () => import('../views/RecipeForm.vue'),
    meta: { tab: 'recipes', requiresAuth: true }
  },
  {
    path: '/plans',
    name: 'Plans',
    component: () => import('../views/MealPlanHome.vue'),
    meta: { tab: 'plans', requiresAuth: true }
  },
  {
    path: '/shopping-list',
    name: 'ShoppingList',
    component: () => import('../views/ShoppingList.vue'),
    meta: { tab: 'list', requiresAuth: true }
  },
  {
    path: '/meal-plan/new',
    name: 'MealPlanCreate',
    component: () => import('../views/MealPlanForm.vue'),
    meta: { tab: 'plans', requiresAuth: true }
  },
  {
    path: '/meal-plan/:id/edit',
    name: 'MealPlanEdit',
    component: () => import('../views/MealPlanForm.vue'),
    meta: { tab: 'plans', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
  } else {
    next()
  }
})

export default router
