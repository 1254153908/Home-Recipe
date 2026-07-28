<script setup>
import { ref } from 'vue'

const userId = 0
const userName = 'Default User'
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Profile</h1>
    </div>

    <div class="user-card">
      <div class="avatar">{{ userName.charAt(0) }}</div>
      <div class="user-info">
        <h2 class="user-name">{{ userName }}</h2>
        <p class="user-id">User ID: {{ userId }}</p>
      </div>
    </div>

    <div class="menu-section">
      <h3 class="menu-title">About</h3>
      <div class="menu-card">
        <div class="menu-item">
          <span>HomeRecipe</span>
          <span class="menu-value">v1.0.0</span>
        </div>
        <div class="menu-divider" />
        <div class="menu-item">
          <span>Server</span>
          <span class="menu-value">localhost:4993</span>
        </div>
        <div class="menu-divider" />
        <div class="menu-item">
          <span>Tech stack</span>
          <span class="menu-value">Vue 3 + Vite</span>
        </div>
      </div>
    </div>

    <div class="menu-section">
      <h3 class="menu-title">Features</h3>
      <div class="menu-card">
        <div class="menu-item">
          <span>Recipe management</span>
          <span class="menu-value">CRUD</span>
        </div>
        <div class="menu-divider" />
        <div class="menu-item">
          <span>Favorites</span>
          <span class="menu-value">Save / Unsave</span>
        </div>
        <div class="menu-divider" />
        <div class="menu-item">
          <span>Meal plans</span>
          <span class="menu-value">Plan your meals</span>
        </div>
        <div class="menu-divider" />
        <div class="menu-item">
          <span>AI Recognize</span>
          <span class="menu-value">Auto-fill recipes</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-card);
  margin-bottom: 20px;
}

.avatar {
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

.user-name {
  font-size: 18px;
  font-weight: 600;
}

.user-id {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.menu-section {
  margin-bottom: 20px;
}

.menu-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  padding-left: 4px;
}

.menu-card {
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  font-size: 15px;
}

.menu-value {
  font-size: 14px;
  color: var(--text-secondary);
}

.menu-divider {
  height: 1px;
  background: var(--border-divider);
  margin: 0 16px;
}
</style>
