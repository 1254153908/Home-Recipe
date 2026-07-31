<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRecipes, createMealPlan, updateMealPlan, getMealPlanDetail } from '../api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => route.name === 'MealPlanEdit')

const recipes = ref([])
const recipeId = ref(null)
const now = new Date()
const planDate = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`)
const status = ref('not_started')
const remark = ref('')
const review = ref('')
const imageUrl = ref('')
const saving = ref(false)
const toast = ref('')

const STATUSES = [
  { value: 'not_started', label: 'Not Started' },
  { value: 'prepping', label: 'Prepping' },
  { value: 'cooking', label: 'Cooking' },
  { value: 'done', label: 'Done' }
]

function showToast(msg) {
  toast.value = msg
  setTimeout(() => { toast.value = '' }, 2000)
}

onMounted(async () => {
  recipes.value = await getRecipes()

  // 支持从 week view 点日期传入 ?date=yyyy-MM-dd
  const dateParam = route.query.date
  if (dateParam) {
    planDate.value = dateParam
  }

  if (isEdit.value) {
    const plan = await getMealPlanDetail(route.params.id)
    recipeId.value = plan.recipeId
    planDate.value = plan.planDate || ''
    status.value = plan.status || 'not_started'
    remark.value = plan.remark || ''
    review.value = plan.review || ''
    imageUrl.value = plan.imageUrl || ''
  }
})

async function handleSubmit() {
  if (!recipeId.value) {
    showToast('Please choose a recipe')
    return
  }
  saving.value = true
  try {
    const data = {
      recipeId: Number(recipeId.value),
      planDate: planDate.value,
      status: status.value,
      remark: remark.value.trim(),
      review: review.value.trim(),
      imageUrl: imageUrl.value.trim()
    }
    if (isEdit.value) {
      await updateMealPlan(route.params.id, data)
      showToast('Updated')
    } else {
      await createMealPlan(data)
      showToast('Created')
    }
    router.replace('/plans')
  } catch {
    showToast('Save failed')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>{{ isEdit ? 'Edit Meal Plan' : 'New Meal Plan' }}</h1>
    </div>

    <div class="form-group">
      <label class="form-label">Recipe *</label>
      <select v-model="recipeId" class="form-input">
        <option :value="null" disabled>Choose a recipe</option>
        <option v-for="r in recipes" :key="r.id" :value="r.id">{{ r.title }}</option>
      </select>
    </div>

    <div class="form-group">
      <label class="form-label">Plan date</label>
      <input v-model="planDate" type="date" class="form-input" />
    </div>

    <div class="form-group">
      <label class="form-label">Status</label>
      <select v-model="status" class="form-input">
        <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
    </div>

    <div class="form-group">
      <label class="form-label">Remark</label>
      <textarea v-model="remark" class="form-input" placeholder="e.g. Remember to buy green onions..." rows="2" />
    </div>

    <div class="form-group">
      <label class="form-label">Image URL</label>
      <input v-model="imageUrl" class="form-input" placeholder="https://..." />
    </div>

    <div class="form-actions">
      <button class="btn btn-outline" @click="router.back">Cancel</button>
      <button class="btn btn-primary btn-block" :disabled="saving" @click="handleSubmit">
        {{ saving ? 'Saving...' : 'Save' }}
      </button>
    </div>

    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<style scoped>
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.form-actions .btn-block {
  flex: 1;
}
</style>
