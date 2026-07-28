import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

const USER_ID = 0

// --- Recipe ---
export function getRecipes() {
  return api.get('/recipes').then(r => r.data)
}

export function getRecipeDetail(id) {
  return api.get(`/recipes/${id}`).then(r => r.data)
}

export function createRecipe(data) {
  return api.post('/recipes', data).then(r => r.data)
}

export function updateRecipe(id, data) {
  return api.put(`/recipes/${id}`, data).then(r => r.data)
}

export function deleteRecipe(id) {
  return api.delete(`/recipes/${id}`)
}

export function favoriteRecipe(id) {
  return api.post(`/recipes/${id}/favorite`, null, { params: { userId: USER_ID } })
}

export function unfavoriteRecipe(id) {
  return api.delete(`/recipes/${id}/favorite`, { params: { userId: USER_ID } })
}

export function getFavoriteRecipes() {
  return api.get('/recipes/favorites', { params: { userId: USER_ID } }).then(r => r.data)
}

export function aiRecognize(data) {
  // urlHint: 自动从 URL 提取域名，辅助 Python 选择解析策略
  if (!data.urlHint && data.sourceType === 'link') {
    try { data.urlHint = new URL(data.content).hostname } catch {}
  }
  return api.post('/recipes/ai-recognize', data, { timeout: 60000 }).then(r => r.data)
}

// --- Ingredient ---
export function getIngredients() {
  return api.get('/ingredients').then(r => r.data)
}

export function resolveIngredient(name) {
  return api.post('/ingredients', null, { params: { name } }).then(r => r.data)
}

// --- Seasoning ---
export function getSeasonings() {
  return api.get('/seasonings').then(r => r.data)
}

export function resolveSeasoning(name) {
  return api.post('/seasonings', null, { params: { name } }).then(r => r.data)
}

// --- MealPlan ---
export function getMealPlans() {
  return api.get('/meal-plans').then(r => r.data)
}

export function getMealPlanDetail(id) {
  return api.get(`/meal-plans/${id}`).then(r => r.data)
}

export function getMealPlanRecipe(id) {
  return api.get(`/meal-plans/${id}/recipe`).then(r => r.data)
}

export function createMealPlan(data) {
  return api.post('/meal-plans', data).then(r => r.data)
}

export function updateMealPlan(id, data) {
  return api.put(`/meal-plans/${id}`, data).then(r => r.data)
}

export function deleteMealPlan(id) {
  return api.delete(`/meal-plans/${id}`)
}

// --- CookingLog ---
export function getCookingLogs() {
  return api.get('/cooking-logs').then(r => r.data)
}

export function createCookingLog(data) {
  return api.post('/cooking-logs', data).then(r => r.data)
}

export function updateCookingLog(id, data) {
  return api.put(`/cooking-logs/${id}`, data).then(r => r.data)
}

export function deleteCookingLog(id) {
  return api.delete(`/cooking-logs/${id}`)
}

// --- File Upload ---
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  }).then(r => r.data.url)
}
