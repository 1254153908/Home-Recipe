import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/RecipeList.vue'),
    meta: { tab: 'recipes' }
  },
  {
    path: '/recipe/new',
    name: 'RecipeCreate',
    component: () => import('../views/RecipeForm.vue'),
    meta: { tab: 'recipes' }
  },
  {
    path: '/recipe/:id',
    name: 'RecipeDetail',
    component: () => import('../views/RecipeDetail.vue'),
    meta: { tab: 'recipes' }
  },
  {
    path: '/recipe/:id/edit',
    name: 'RecipeEdit',
    component: () => import('../views/RecipeForm.vue'),
    meta: { tab: 'recipes' }
  },
  {
    path: '/plans',
    name: 'Plans',
    component: () => import('../views/MealPlanHome.vue'),
    meta: { tab: 'plans' }
  },
  {
    path: '/meal-plan/new',
    name: 'MealPlanCreate',
    component: () => import('../views/MealPlanForm.vue'),
    meta: { tab: 'plans' }
  },
  {
    path: '/meal-plan/:id/edit',
    name: 'MealPlanEdit',
    component: () => import('../views/MealPlanForm.vue'),
    meta: { tab: 'plans' }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('../views/User.vue'),
    meta: { tab: 'user' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
