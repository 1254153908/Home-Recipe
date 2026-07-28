<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMealPlans, getMealPlanRecipe, updateMealPlan, deleteMealPlan, getCookingLogs, updateCookingLog } from '../api'

const router = useRouter()
const plans = ref([])
const logs = ref([])
const loading = ref(true)
const toast = ref('')
const expandedPlanId = ref(null)

const STATUSES = [
  { value: 'not_started', label: 'Not Started' },
  { value: 'prepping', label: 'Prepping' },
  { value: 'cooking', label: 'Cooking' },
  { value: 'done', label: 'Done' }
]

const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

// ---------- week navigation ----------
const weekOffset = ref(0)
const selectedDay = ref(0)

function mondayOfWeek(offset) {
  const now = new Date()
  const day = now.getDay()
  const diff = day === 0 ? -6 : 1 - day
  const monday = new Date(now)
  monday.setDate(now.getDate() + diff + offset * 7)
  monday.setHours(0, 0, 0, 0)
  return monday
}

const weekStart = computed(() => mondayOfWeek(weekOffset.value))

const weekDays = computed(() => {
  const days = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(weekStart.value)
    d.setDate(d.getDate() + i)
    days.push(d)
  }
  return days
})

const weekLabel = computed(() => {
  const s = weekStart.value
  const e = new Date(s)
  e.setDate(e.getDate() + 6)
  const fmt = d => {
    const m = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][d.getMonth()]
    return `${m} ${d.getDate()}`
  }
  return `${fmt(s)} - ${fmt(e)}, ${s.getFullYear()}`
})

const selectedDate = computed(() => weekDays.value[selectedDay.value])
const todayIndex = computed(() => {
  const now = new Date()
  return weekDays.value.findIndex(d => formatDate(d) === formatDate(now))
})

function prevWeek() {
  weekOffset.value--
  selectedDay.value = todayIndex.value >= 0 ? todayIndex.value : 0
}
function nextWeek() {
  weekOffset.value++
  selectedDay.value = todayIndex.value >= 0 ? todayIndex.value : 0
}

function selectDay(i) {
  selectedDay.value = i
}

// ---------- data loading ----------
function showToast(msg) {
  toast.value = msg
  setTimeout(() => { toast.value = '' }, 2000)
}

function formatDate(d) {
  return d.toISOString().slice(0, 10)
}

function getLogForPlan(planId) {
  return logs.value.find(l => l.planId === planId)
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
  // 默认选中今天
  if (todayIndex.value >= 0) selectedDay.value = todayIndex.value
})

function plansForDate(date) {
  const key = formatDate(date)
  return plans.value.filter(p => p.planDate === key)
}

const dayPlans = computed(() => plansForDate(selectedDate.value))

// ---------- actions ----------
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
      showToast('Logged')
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

function toggleExpand(planId) {
  expandedPlanId.value = expandedPlanId.value === planId ? null : planId
}

async function saveLog(logEntry) {
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

function goAdd() {
  router.push(`/meal-plan/new?date=${formatDate(selectedDate.value)}`)
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Meal Plans</h1>
      <p>Plan your weekly menu</p>
    </div>

    <!-- Week selector -->
    <div class="week-nav">
      <button class="week-arrow" @click="prevWeek">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>
      <span class="week-label">{{ weekLabel }}</span>
      <button class="week-arrow" @click="nextWeek">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="m9 18 6-6-6-6"/>
        </svg>
      </button>
    </div>

    <!-- Day headers -->
    <div class="day-headers">
      <button
        v-for="(day, i) in weekDays"
        :key="i"
        class="day-header"
        :class="{ active: selectedDay === i }"
        @click="selectDay(i)"
      >
        <span class="day-name">{{ DAY_NAMES[i] }}</span>
        <span class="day-num">{{ day.getMonth() + 1 }}/{{ day.getDate() }}</span>
        <span class="day-count">{{ plansForDate(day).length }} meal{{ plansForDate(day).length !== 1 ? 's' : '' }}</span>
      </button>
    </div>

    <!-- Content: selected day only -->
    <div v-if="loading" class="empty-state"><p>Loading...</p></div>

    <div v-else class="day-content">
      <div class="day-divider">
        <span>{{ DAY_NAMES[selectedDay] }}, {{ selectedDate.getMonth() + 1 }}/{{ selectedDate.getDate() }}</span>
        <button class="day-add-btn" @click="goAdd()">+</button>
      </div>

      <div v-if="dayPlans.length === 0" class="day-empty">
        No plans
      </div>

      <div v-else class="plan-list">
        <div class="plan-card" v-for="plan in dayPlans" :key="plan.id">
          <div class="plan-top">
            <router-link :to="`/recipe/${plan._recipeId || plan.recipeId}`" class="plan-recipe-link">
              {{ plan._recipeTitle }}
            </router-link>
            <select
              class="status-select"
              :class="plan.status || 'not_started'"
              :value="plan.status || 'not_started'"
              @change="handleStatusChange(plan, $event.target.value)"
            >
              <option v-for="s in STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
            </select>
          </div>

          <p class="plan-remark" v-if="plan.remark">{{ plan.remark }}</p>

          <div class="plan-actions">
            <button class="btn btn-outline btn-sm" @click="router.push(`/meal-plan/${plan.id}/edit`)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="handleDelete(plan.id)">Delete</button>
            <button
              class="btn btn-outline btn-sm log-toggle"
              :class="{ open: expandedPlanId === plan.id }"
              @click="toggleExpand(plan.id)"
            >
              {{ expandedPlanId === plan.id ? 'Hide Log' : 'Log' }}
              <svg
                class="log-chevron"
                :class="{ open: expandedPlanId === plan.id }"
                width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              >
                <path d="m6 9 6 6 6-6"/>
              </svg>
            </button>
          </div>

          <!-- Embedded Log -->
          <div v-if="expandedPlanId === plan.id" class="log-detail">
            <template v-if="getLogForPlan(plan.id)">
              <div class="form-group">
                <label class="form-label">Result image</label>
                <div class="img-url-row">
                  <input v-model="getLogForPlan(plan.id).imageUrl" class="form-input" placeholder="https://..." />
                </div>
                <img
                  v-if="getLogForPlan(plan.id).imageUrl"
                  :src="getLogForPlan(plan.id).imageUrl"
                  class="log-preview"
                />
              </div>
              <div class="form-group">
                <label class="form-label">Review</label>
                <textarea
                  v-model="getLogForPlan(plan.id).review"
                  class="form-input"
                  placeholder="Write your review..."
                  rows="2"
                />
              </div>
              <button class="btn btn-primary btn-sm" @click="saveLog(getLogForPlan(plan.id))">Save</button>
            </template>
            <p v-else class="text-muted">Set status to "Done" to create a log entry</p>
          </div>
        </div>
      </div>
    </div>

    <router-link to="/meal-plan/new" class="fab">+</router-link>
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<style scoped>
/* Week nav */
.week-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.week-arrow {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--input-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  transition: background 0.15s;
}

.week-arrow:active { background: var(--border-light); }

.week-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 180px;
  text-align: center;
}

/* Day headers */
.day-headers {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
}

.day-header {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 2px;
  border-radius: var(--radius-sm);
  background: var(--input-bg);
  border: 2px solid transparent;
  transition: all 0.15s;
  cursor: pointer;
}

.day-header:hover { background: var(--border-light); }

.day-header.active {
  background: var(--text-primary);
  border-color: var(--text-primary);
}

.day-header.active .day-name,
.day-header.active .day-num,
.day-header.active .day-count {
  color: var(--text-white);
}

.day-name {
  font-size: 10px;
  font-weight: 500;
  color: var(--text-secondary);
  text-transform: uppercase;
}

.day-num {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.day-count {
  font-size: 10px;
  color: var(--text-secondary);
}

/* Day content */
.day-content { }

.day-divider {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.day-add-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--input-bg);
  color: var(--text-secondary);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.day-add-btn:hover { background: var(--border-light); color: var(--text-primary); }

.day-empty {
  font-size: 13px;
  color: var(--text-placeholder);
  padding: 10px 0;
}

/* Plan cards */
.plan-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 12px 14px;
}

.plan-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.plan-recipe-link {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-remark {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.plan-actions {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: 8px;
}

/* Status select */
.status-select {
  height: 28px;
  padding: 0 8px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-light);
  font-size: 11px;
  font-weight: 500;
  background: var(--input-bg);
  color: var(--text-secondary);
  cursor: pointer;
  outline: none;
  appearance: none;
  -webkit-appearance: none;
  padding-right: 22px;
  background-image: url("data:image/svg+xml,%3Csvg width='10' height='6' viewBox='0 0 10 6' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='m1 1 4 4 4-4' stroke='%238E8E93' stroke-width='1.5' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 6px center;
  flex-shrink: 0;
}

.status-select.not_started { background: #F2F2F7; color: #8E8E93; border-color: #E5E5EA; }
.status-select.prepping { background: #FFF3E0; color: #E65100; border-color: #FFCC80; }
.status-select.cooking { background: #FFF8E1; color: #F57F17; border-color: #FFE082; }
.status-select.done { background: #E8F5E9; color: #2E7D32; border-color: #A5D6A7; }

/* Log toggle */
.log-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
}

.log-chevron {
  transition: transform 0.2s;
}

.log-chevron.open {
  transform: rotate(180deg);
}

/* Log detail */
.log-detail {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--border-divider);
}

.log-preview {
  margin-top: 6px;
  border-radius: var(--radius-sm);
  max-height: 120px;
  width: 100%;
  object-fit: cover;
}

.img-url-row {
  display: flex;
  gap: 6px;
}

.img-url-row .form-input {
  flex: 1;
}

.text-muted {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
