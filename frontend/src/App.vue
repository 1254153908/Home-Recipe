<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from './components/NavBar.vue'
import { computed } from 'vue'

const route = useRoute()
const router = useRouter()
const showNav = computed(() => route.meta.tab !== undefined)
const showPanel = ref(false)
const panelRef = ref(null)
const touchStartX = ref(0)

function getUserInfo() {
  try {
    return JSON.parse(localStorage.getItem('user')) || {}
  } catch {
    return {}
  }
}

function tokenExists() {
  return !!localStorage.getItem('token')
}

function handleAvatarClick() {
  if (tokenExists()) {
    openPanel()
  } else {
    router.push('/login')
  }
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  closePanel()
  router.push('/login')
}

function openPanel() {
  showPanel.value = true
}

function closePanel() {
  showPanel.value = false
}

function onTouchStart(e) {
  touchStartX.value = e.touches[0].clientX
}

function onTouchEnd(e) {
  const dx = e.changedTouches[0].clientX - touchStartX.value
  if (dx > 60) {
    closePanel()
  }
}

function onOverlayTouchStart(e) {
  touchStartX.value = e.touches[0].clientX
}

function onOverlayTouchEnd(e) {
  const dx = e.changedTouches[0].clientX - touchStartX.value
  if (dx < -30) {
    closePanel()
  }
}

// Block background scroll when panel is open
onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})

function onKeydown(e) {
  if (e.key === 'Escape' && showPanel.value) {
    closePanel()
  }
}
</script>

<template>
  <div class="app-root">
    <router-view />

    <!-- Top-right avatar -->
    <button class="avatar-btn" @click="handleAvatarClick" v-show="showNav">
      <span class="avatar-char">{{ tokenExists() ? (getUserInfo().nickname || 'U')[0].toUpperCase() : '→' }}</span>
    </button>

    <!-- Slide-out Profile Panel -->
    <Transition name="panel">
      <div v-if="showPanel" class="panel-overlay" @click="closePanel" @touchstart="onOverlayTouchStart" @touchend="onOverlayTouchEnd">
        <div
          ref="panelRef"
          class="profile-panel"
          @click.stop
          @touchstart="onTouchStart"
          @touchend="onTouchEnd"
        >
          <!-- User Card -->
          <div class="panel-user-card">
            <div class="panel-avatar">{{ (getUserInfo().nickname || 'U')[0].toUpperCase() }}</div>
            <div class="panel-user-info">
              <h2 class="panel-user-name">{{ getUserInfo().nickname || 'User' }}</h2>
              <p class="panel-user-id">@{{ getUserInfo().username || '' }}</p>
            </div>
          </div>

          <button class="logout-btn" @click="logout">退出登录</button>

          <!-- About -->
          <div class="panel-section">
            <h3 class="panel-section-title">About</h3>
            <div class="panel-menu-card">
              <div class="panel-menu-item">
                <span>HomeRecipe</span>
                <span class="panel-menu-value">v1.0.0</span>
              </div>
              <div class="panel-divider" />
              <div class="panel-menu-item">
                <span>Server</span>
                <span class="panel-menu-value">localhost:4993</span>
              </div>
              <div class="panel-divider" />
              <div class="panel-menu-item">
                <span>Tech stack</span>
                <span class="panel-menu-value">Vue 3 + Vite</span>
              </div>
            </div>
          </div>

          <!-- Features -->
          <div class="panel-section">
            <h3 class="panel-section-title">Features</h3>
            <div class="panel-menu-card">
              <div class="panel-menu-item">
                <span>Recipe management</span>
                <span class="panel-menu-value">CRUD</span>
              </div>
              <div class="panel-divider" />
              <div class="panel-menu-item">
                <span>Favorites</span>
                <span class="panel-menu-value">Save / Unsave</span>
              </div>
              <div class="panel-divider" />
              <div class="panel-menu-item">
                <span>Meal plans</span>
                <span class="panel-menu-value">Plan your meals</span>
              </div>
              <div class="panel-divider" />
              <div class="panel-menu-item">
                <span>AI Recognize</span>
                <span class="panel-menu-value">Auto-fill recipes</span>
              </div>
              <div class="panel-divider" />
              <div class="panel-menu-item">
                <span>Shopping List</span>
                <span class="panel-menu-value">Auto-aggregate</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <NavBar v-if="showNav" />
  </div>
</template>

<style scoped>
.app-root {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

.avatar-btn {
  position: fixed;
  top: 12px;
  right: calc(50% - (var(--container-width) / 2) + 16px);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--text-primary);
  color: var(--text-white);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 90;
  cursor: pointer;
}

@media (max-width: 480px) {
  .avatar-btn {
    right: 8px;
  }
}

.avatar-char {
  font-size: 15px;
  font-weight: 700;
}

/* Panel Overlay */
.panel-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.4);
}

/* Profile Panel */
.profile-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 85%;
  max-width: 400px;
  height: 100%;
  background: #F2F2F7;
  padding: 20px 18px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* Panel Transition */
.panel-enter-active,
.panel-leave-active {
  transition: all 0.3s ease;
}
.panel-enter-active .profile-panel,
.panel-leave-active .profile-panel {
  transition: transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.panel-enter-from,
.panel-leave-to {
  background: rgba(0, 0, 0, 0);
}
.panel-enter-from .profile-panel,
.panel-leave-to .profile-panel {
  transform: translateX(100%);
}

/* Panel Content */
.panel-user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-card);
  margin-bottom: 24px;
}

.panel-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--text-primary);
  color: var(--text-white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
}

.panel-user-name {
  font-size: 18px;
  font-weight: 600;
}

.panel-user-id {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.panel-section {
  margin-bottom: 20px;
}

.panel-section-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  padding-left: 4px;
}

.panel-menu-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.panel-menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  font-size: 15px;
}

.panel-menu-value {
  font-size: 14px;
  color: var(--text-secondary);
}

.panel-divider {
  height: 1px;
  background: var(--border-divider);
  margin: 0 16px;
}

.logout-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--input-bg);
  font-size: 15px;
  color: var(--danger);
  margin-top: 12px;
  cursor: pointer;
}
</style>
