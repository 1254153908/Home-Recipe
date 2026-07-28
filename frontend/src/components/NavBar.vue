<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { key: 'recipes', label: 'Recipes', icon: 'recipe', to: '/' },
  { key: 'plans', label: 'Plans', icon: 'plan', to: '/plans' },
  { key: 'user', label: 'Profile', icon: 'user', to: '/user' }
]
</script>

<template>
  <nav class="bottom-nav">
    <router-link
      v-for="tab in tabs"
      :key="tab.key"
      :to="tab.to"
      class="nav-item"
      :class="{ active: route.meta.tab === tab.key }"
    >
      <svg class="nav-icon" width="20" height="20" viewBox="0 0 24 24" fill="none">
        <template v-if="tab.icon === 'plan'">
          <rect x="3" y="4" width="18" height="18" rx="3" stroke="currentColor" stroke-width="2"/>
          <path d="M3 10h18" stroke="currentColor" stroke-width="2"/>
          <path d="M8 2v4M16 2v4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </template>
        <template v-if="tab.icon === 'recipe'">
          <path d="M4 6h16v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6z" stroke="currentColor" stroke-width="2"/>
          <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M9 12h6M9 16h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </template>
        <template v-if="tab.icon === 'user'">
          <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="2"/>
          <path d="M4 22c0-4.4 3.6-8 8-8s8 3.6 8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </template>
      </svg>
      <span class="nav-label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: var(--nav-height);
  background: var(--card-bg);
  border-top: 1px solid var(--border-divider);
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 12px;
  color: var(--text-placeholder);
  transition: color 0.2s;
}

.nav-item.active {
  color: var(--text-primary);
}

.nav-icon {
  width: 20px;
  height: 20px;
}

.nav-label {
  font-size: 10px;
  font-weight: 500;
}
</style>
