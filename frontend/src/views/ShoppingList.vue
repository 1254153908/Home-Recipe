<script setup>
import { ref, computed, onMounted } from 'vue'
import { generateShoppingList, getShoppingLists, getShoppingListDetail, toggleShoppingListItem, deleteShoppingList } from '../api'

const startDate = ref('')
const endDate = ref('')
const currentList = ref(null)
const history = ref([])
const loading = ref(false)
const errorMsg = ref('')

onMounted(() => {
  const now = new Date()
  const day = now.getDay()
  const mon = new Date(now.getFullYear(), now.getMonth(), now.getDate() - (day === 0 ? 6 : day - 1))
  const sun = new Date(mon.getFullYear(), mon.getMonth(), mon.getDate() + 6)
  startDate.value = fmt(mon)
  endDate.value = fmt(sun)
  loadHistory()
})

function fmt(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

function prevWeek() {
  shift(-7)
}

function nextWeek() {
  shift(7)
}

function shift(delta) {
  const s = parseDate(startDate.value)
  const e = parseDate(endDate.value)
  s.setDate(s.getDate() + delta)
  e.setDate(e.getDate() + delta)
  startDate.value = fmt(s)
  endDate.value = fmt(e)
}

function parseDate(str) {
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

async function handleGenerate() {
  loading.value = true
  errorMsg.value = ''
  try {
    currentList.value = await generateShoppingList(startDate.value, endDate.value)
    loadHistory()
  } catch (e) {
    errorMsg.value = e?.response?.data?.message || 'Failed to generate list'
  } finally {
    loading.value = false
  }
}

async function handleToggle(item) {
  const newVal = !item.isPurchased
  try {
    await toggleShoppingListItem(currentList.value.id, item.id, newVal)
    item.isPurchased = newVal
  } catch {}
}

async function handleDeleteList(listId) {
  if (!confirm('Delete this list?')) return
  try {
    await deleteShoppingList(listId)
    if (currentList.value?.id === listId) {
      currentList.value = null
    }
    loadHistory()
  } catch {}
}

async function handleViewHistory(listId) {
  errorMsg.value = ''
  try {
    currentList.value = await getShoppingListDetail(listId)
  } catch {}
}

async function loadHistory() {
  try {
    history.value = await getShoppingLists()
  } catch {}
}

const purchasedCount = computed(() => {
  if (!currentList.value?.items) return 0
  return currentList.value.items.filter(i => i.isPurchased).length
})

const totalCount = computed(() => currentList.value?.items?.length || 0)

const progressPercent = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((purchasedCount.value / totalCount.value) * 100)
})

const allDone = computed(() => totalCount.value > 0 && purchasedCount.value === totalCount.value)

function historyPurchased(h) {
  if (!h.items) return [0, 0]
  const done = h.items.filter(i => i.isPurchased).length
  return [done, h.items.length]
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Shopping List</h1>
    </div>

    <div class="week-picker">
      <button class="week-btn" @click="prevWeek">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      </button>
      <div class="week-range">
        <input type="date" class="date-input" v-model="startDate" />
        <span class="range-sep">-</span>
        <input type="date" class="date-input" v-model="endDate" />
      </div>
      <button class="week-btn" @click="nextWeek">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      </button>
    </div>

    <button class="btn btn-primary btn-block" @click="handleGenerate" :disabled="loading">
      {{ loading ? 'Generating...' : 'Generate List' }}
    </button>

    <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

    <!-- Current List -->
    <div v-if="currentList" class="list-section">
      <div class="list-header">
        <span class="list-title">{{ currentList.startDate }} ~ {{ currentList.endDate }}</span>
        <button class="btn-delete" @click="handleDeleteList(currentList.id)">Delete</button>
      </div>

      <div class="progress-wrap">
        <div class="progress-track">
          <div class="progress-fill" :class="{ done: allDone }" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <span class="progress-text">{{ purchasedCount }}/{{ totalCount }} {{ allDone ? '✓' : '' }}</span>
      </div>

      <div class="list-card">
        <div
          v-for="item in currentList.items"
          :key="item.id"
          class="list-item"
          :class="{ purchased: item.isPurchased }"
          @click="handleToggle(item)"
        >
          <div class="item-check" :class="{ checked: item.isPurchased }">
            <svg v-if="item.isPurchased" width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M5 13l4 4L19 7" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <span class="item-name">{{ item.name }}</span>
          <span class="item-qty">{{ item.quantity }}{{ item.unit }}</span>
        </div>
        <div v-if="currentList.items.length === 0" class="empty-list">
          No ingredients needed for this period.
        </div>
      </div>

      <p class="list-hint">Tap an item to mark as purchased</p>
    </div>

    <!-- History -->
    <div class="history-section" v-if="history.length > 0">
      <h3 class="section-title">History</h3>
      <div class="history-list">
        <div
          v-for="h in history"
          :key="h.id"
          class="history-item"
          @click="handleViewHistory(h.id)"
        >
          <div class="hi-left">
            <span class="hi-dates">{{ h.startDate }} ~ {{ h.endDate }}</span>
            <span class="hi-meta">{{ h.items?.length || 0 }} items · {{ historyPurchased(h)[0] }}/{{ historyPurchased(h)[1] }} done</span>
          </div>
          <div class="hi-right">
            <button class="btn-del-item" @click.stop="handleDeleteList(h.id)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            </button>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="hi-chev">
              <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.week-picker {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.week-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--input-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  flex-shrink: 0;
}

.week-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

.date-input {
  width: 130px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-primary);
  background: var(--card-bg);
}

.range-sep {
  color: var(--text-secondary);
  font-size: 14px;
}

.error-msg {
  color: #FF3B30;
  font-size: 13px;
  text-align: center;
  margin-top: 8px;
}

.list-section {
  margin-top: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.list-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.btn-delete {
  font-size: 13px;
  color: #FF3B30;
  background: none;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
}

.btn-delete:active {
  background: rgba(255, 59, 48, 0.08);
}

.progress-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.progress-track {
  flex: 1;
  height: 6px;
  background: var(--input-bg);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-fill.done {
  background: #34C759;
}

.progress-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  min-width: 48px;
  text-align: right;
}

.list-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.list-item:not(:last-child) {
  border-bottom: 1px solid var(--border-divider);
}

.list-item:active {
  background: var(--input-bg);
}

.list-item.purchased {
  background: #F9F9FB;
}

.list-item.purchased .item-name {
  text-decoration: line-through;
  color: var(--text-placeholder);
}

.list-item.purchased .item-qty {
  color: var(--text-placeholder);
}

.item-check {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.item-check.checked {
  background: #34C759;
  border-color: #34C759;
}

.item-name {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
}

.item-qty {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 400;
}

.empty-list {
  padding: 24px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.list-hint {
  text-align: center;
  color: var(--text-placeholder);
  font-size: 12px;
  margin-top: 10px;
}

.history-section {
  margin-top: 32px;
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  padding-left: 4px;
}

.history-list {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  color: var(--text-primary);
}

.history-item:not(:last-child) {
  border-bottom: 1px solid var(--border-divider);
}

.history-item:active {
  background: var(--input-bg);
}

.hi-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hi-dates {
  font-size: 14px;
  font-weight: 500;
}

.hi-meta {
  font-size: 12px;
  color: var(--text-secondary);
}

.hi-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-del-item {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--input-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.btn-del-item:active {
  background: rgba(255, 59, 48, 0.15);
  color: #FF3B30;
}

.hi-chev {
  color: var(--text-placeholder);
}
</style>
