<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMealPlans, getMealPlanRecipe, updateMealPlan, deleteMealPlan, getCookingLogs, updateCookingLog } from '../api'

const router = useRouter()
const plans = ref([])
const logs = ref([])
const loading = ref(true)
const tab = ref('plans')
const toast = ref('')
const expandedLog = ref(null)

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

async function loadLogs() {
  try {
    logs.value = await getCookingLogs()
  } catch { logs.value = [] }
}

onMounted(async () => {
  try {
    const list = await getMealPlans()
    const enriched = await Promise.all(
      list.map(async (plan) => {
        try {
          const detail = await getMealPlanRecipe(plan.id)
          return { ...plan, _recipeTitle: detail.title, _recipeId: detail.recipeId }
        } catch {
          return { ...plan, _recipeTitle: 'Unknown Recipe', _recipeId: plan.recipeId }
        }
      })
    )
    plans.value = enriched
  } finally {
    loading.value = false
  }
  await loadLogs()
})

async function handleStatusChange(plan, newStatus) {
  const oldStatus = plan.status
  plan.status = newStatus
  try {
    await updateMealPlan(plan.id, {
      recipeId: plan._recipeId || plan.recipeId,
      planDate: plan.planDate,
      status: newStatus,
      remark: plan.remark || '',
      review: plan.review || '',
      imageUrl: plan.imageUrl || ''
    })
    if (newStatus === 'done' && oldStatus !== 'done') {
      await loadLogs()
      showToast('Logged to history')
    }
  } catch {
    plan.status = oldStatus
    showToast('Failed to update status')
  }
}

async function handleDelete(id) {
  if (!confirm('Delete this meal plan?')) return
  await deleteMealPlan(id)
  plans.value = plans.value.filter(p => p.id !== id)
  showToast('Deleted')
}

function toggleLog(logEntry) {
  expandedLog.value = expandedLog.value === logEntry.id ? null : logEntry.id
}

async function saveLogEntry(logEntry) {
  try {
    await updateCookingLog(logEntry.id, {
      imageUrl: logEntry.imageUrl || '',
      review: logEntry.review || ''
    })
    showToast('Saved')
  } catch {
    showToast('Failed to save')
  }
}

const activePlans = computed(() => plans.value.filter(p => p.status !== 'done'))
const donePlans = computed(() => plans.value.filter(p => p.status === 'done'))
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Meal Plans</h1>
      <p>Plan your weekly menu</p>
    </div>

    <div class="tab-row">
      <button class="tab-btn" :class="{ active: tab === 'plans' }" @click="tab = 'plans'">Plans</button>
      <button class="tab-btn" :class="{ active: tab === 'log' }" @click="tab = 'log'">Log</button>
    </div>

    <!-- Plans Tab -->
    <template v-if="tab === 'plans'">
      <div v-if="loading" class="empty-state"><p>Loading...</p></div>

      <div v-else-if="plans.length === 0" class="empty-state">
        <p>No meal plans yet</p>
        <p style="margin-top:4px">Tap + to plan your first meal</p>
      </div>

      <div v-else class="plan-list">
        <div class="plan-card" v-for="plan in plans" :key="plan.id">
          <div class="plan-body">
            <router-link :to="`/recipe/${plan._recipeId || plan.recipeId}`" class="plan-recipe-link">
              {{ plan._recipeTitle }}
            </router-link>
            <div class="plan-meta">
              <span class="plan-date">{{ plan.planDate }}</span>
            </div>
            <p class="plan-remark" v-if="plan.remark">{{ plan.remark }}</p>
          </div>
          <div class="plan-actions">
            <select
              class="status-select"
              :class="plan.status"
              :value="plan.status || 'not_started'"
              @change="handleStatusChange(plan, $event.target.value)"
            >
              <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
            </select>
            <button class="btn btn-outline btn-sm" @click="router.push(`/meal-plan/${plan.id}/edit`)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="handleDelete(plan.id)">Delete</button>
          </div>
        </div>
      </div>

      <router-link to="/meal-plan/new" class="fab">+</router-link>
    </template>

    <!-- Log Tab -->
    <template v-if="tab === 'log'">
      <div v-if="logs.length === 0" class="empty-state">
        <p>No logs yet</p>
        <p style="margin-top:4px">Complete a meal plan to see it here</p>
      </div>

      <div v-else class="log-list">
        <div class="log-card" v-for="log in logs" :key="log.id">
          <div class="log-header" @click="toggleLog(log)">
            <div class="log-info">
              <h3 class="log-recipe">{{ log.recipeTitle }}</h3>
              <span class="log-date">{{ new Date(log.completedAt).toLocaleString() }}</span>
            </div>
            <svg
              class="log-chevron"
              :class="{ open: expandedLog === log.id }"
              width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            >
              <path d="m6 9 6 6 6-6"/>
            </svg>
          </div>

          <div v-if="expandedLog === log.id" class="log-detail">
            <div class="form-group">
              <label class="form-label">Result image URL</label>
              <input v-model="log.imageUrl" class="form-input" placeholder="https://..." />
              <img v-if="log.imageUrl" :src="log.imageUrl" class="log-preview" />
            </div>
            <div class="form-group">
              <label class="form-label">Review</label>
              <textarea v-model="log.review" class="form-input" placeholder="Write your review..." rows="2" />
            </div>
            <button class="btn btn-primary btn-sm" @click="saveLogEntry(log)">Save</button>
          </div>
        </div>
      </div>
    </template>

    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<style scoped>
.tab-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.tab-btn {
  padding: 8px 18px;
  border-radius: var(--radius-full);
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  border: 1px solid var(--border-light);
  transition: all 0.2s;
}

.tab-btn.active {
  background: var(--text-primary);
  color: var(--text-white);
  border-color: var(--text-primary);
}

/* Plans */
.plan-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 14px 16px;
}

.plan-recipe-link {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  transition: color 0.2s;
}

.plan-recipe-link:hover {
  color: var(--accent);
}

.plan-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 4px;
}

.plan-date {
  font-size: 13px;
  color: var(--text-secondary);
}

.plan-remark {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.plan-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 10px;
}

.status-select {
  height: 32px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-light);
  font-size: 12px;
  font-weight: 500;
  background: var(--input-bg);
  color: var(--text-secondary);
  cursor: pointer;
  outline: none;
  appearance: none;
  -webkit-appearance: none;
  padding-right: 24px;
  background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='m1 1 4 4 4-4' stroke='%238E8E93' stroke-width='1.5' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
}

.status-select.not_started { background: #F2F2F7; color: #8E8E93; border-color: #E5E5EA; }
.status-select.prepping { background: #FFF3E0; color: #E65100; border-color: #FFCC80; }
.status-select.cooking { background: #FFF8E1; color: #F57F17; border-color: #FFE082; }
.status-select.done { background: #E8F5E9; color: #2E7D32; border-color: #A5D6A7; }

/* Log */
.log-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.log-header:hover {
  background: var(--input-bg);
}

.log-recipe {
  font-size: 16px;
  font-weight: 600;
}

.log-date {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
  display: block;
}

.log-chevron {
  color: var(--text-secondary);
  flex-shrink: 0;
  transition: transform 0.2s;
}

.log-chevron.open {
  transform: rotate(180deg);
}

.log-detail {
  padding: 0 16px 16px;
}

.log-preview {
  margin-top: 8px;
  border-radius: var(--radius-sm);
  max-height: 160px;
  width: 100%;
  object-fit: cover;
}
</style>
