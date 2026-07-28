<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRecipeDetail, deleteRecipe, favoriteRecipe, unfavoriteRecipe } from '../api'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const loading = ref(true)
const toast = ref('')

function showToast(msg) {
  toast.value = msg
  setTimeout(() => { toast.value = '' }, 2000)
}

onMounted(async () => {
  try {
    detail.value = await getRecipeDetail(route.params.id)
  } finally {
    loading.value = false
  }
})

async function handleDelete() {
  if (!confirm('Delete this recipe?')) return
  await deleteRecipe(route.params.id)
  showToast('Deleted')
  setTimeout(() => router.replace('/'), 500)
}

async function handleFavorite() {
  await favoriteRecipe(route.params.id)
  showToast('Favorited')
}

async function handleUnfavorite() {
  await unfavoriteRecipe(route.params.id)
  showToast('Unfavorited')
}
</script>

<template>
  <div class="page" v-if="!loading && detail">
    <div class="detail-hero" v-if="detail.recipe.imageUrl">
      <img :src="detail.recipe.imageUrl" :alt="detail.recipe.title" />
    </div>

    <div class="detail-header">
      <button class="btn-back" @click="router.back">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="m15 18-6-6 6-6"/>
        </svg>
      </button>
      <h1 class="detail-title">{{ detail.recipe.title }}</h1>
      <div class="detail-meta" v-if="detail.recipe.sourceUrl">
        <a class="source-link" :href="detail.recipe.sourceUrl" target="_blank">
          {{ detail.recipe.sourceType || 'Source' }}
        </a>
      </div>
    </div>

    <section class="detail-section">
      <h2 class="section-title">Ingredients</h2>
      <div class="item-list" v-if="detail.ingredients?.length">
        <span class="item-tag" v-for="(ing, i) in detail.ingredients" :key="i">
          {{ ing.name }} <em>{{ ing.quantity }}{{ ing.unit }}</em>
        </span>
      </div>
      <p class="text-muted" v-else>No ingredients</p>
    </section>

    <section class="detail-section">
      <h2 class="section-title">Seasonings</h2>
      <div class="item-list" v-if="detail.seasonings?.length">
        <span class="item-tag" v-for="(sea, i) in detail.seasonings" :key="i">
          {{ sea.name }} <em>{{ sea.quantity }}{{ sea.unit }}</em>
        </span>
      </div>
      <p class="text-muted" v-else>No seasonings</p>
    </section>

    <section class="detail-section">
      <h2 class="section-title">Steps</h2>
      <div class="steps" v-if="detail.steps?.length">
        <div class="step-item" v-for="step in detail.steps" :key="step.id || step.stepNo">
          <span class="step-no">{{ step.stepNo + 1 }}</span>
          <div class="step-content">
            <p>{{ step.content }}</p>
            <img v-if="step.imageUrl" :src="step.imageUrl" class="step-img" />
          </div>
        </div>
      </div>
      <p class="text-muted" v-else>No steps</p>
    </section>

    <div class="detail-actions">
      <button class="btn btn-outline btn-sm" @click="handleFavorite">Favorite</button>
      <button class="btn btn-outline btn-sm" @click="handleUnfavorite">Unfavorite</button>
      <router-link :to="`/recipe/${route.params.id}/edit`" class="btn btn-primary btn-sm">Edit</router-link>
      <button class="btn btn-danger btn-sm" @click="handleDelete">Delete</button>
    </div>

    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<style scoped>
.detail-hero {
  margin: -16px -16px 0;
}

.detail-hero img {
  width: 100%;
  aspect-ratio: 16/9;
  object-fit: cover;
}

.detail-header {
  padding: 20px 0 0;
  position: relative;
}

.btn-back {
  position: absolute;
  top: -40px;
  left: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--card-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-card);
}

.detail-title {
  font-size: 22px;
  font-weight: 700;
}

.detail-meta {
  margin-top: 8px;
}

.source-link {
  font-size: 14px;
  color: var(--accent);
}

.detail-section {
  margin-top: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.item-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.item-tag {
  background: var(--card-bg);
  padding: 8px 14px;
  border-radius: var(--radius-full);
  font-size: 14px;
  border: 1px solid var(--border-light);
}

.item-tag em {
  font-style: normal;
  color: var(--text-secondary);
  margin-left: 4px;
}

.text-muted {
  color: var(--text-secondary);
  font-size: 14px;
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-item {
  display: flex;
  gap: 14px;
}

.step-no {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--text-primary);
  color: var(--text-white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-content p {
  font-size: 15px;
  line-height: 1.6;
}

.step-img {
  margin-top: 8px;
  border-radius: var(--radius-sm);
  max-height: 200px;
}

.detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  flex-wrap: wrap;
}
</style>
