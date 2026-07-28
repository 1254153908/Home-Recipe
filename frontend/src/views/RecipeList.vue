<script setup>
import { ref, computed, onMounted } from 'vue'
import { getRecipes, getFavoriteRecipes } from '../api'
import RecipeCard from '../components/RecipeCard.vue'

const allRecipes = ref([])
const favorites = ref([])
const searchQuery = ref('')
const showFavorites = ref(false)
const loading = ref(true)

const recipes = computed(() => showFavorites.value ? favorites.value : allRecipes.value)

const filteredRecipes = computed(() => {
  if (!searchQuery.value) return recipes.value
  const q = searchQuery.value.toLowerCase()
  return recipes.value.filter(r => r.title.toLowerCase().includes(q))
})

onMounted(async () => {
  try {
    const [all, favs] = await Promise.all([getRecipes(), getFavoriteRecipes()])
    allRecipes.value = all
    favorites.value = favs
  } finally {
    loading.value = false
  }
})

function toggleFavorites() {
  showFavorites.value = !showFavorites.value
  searchQuery.value = ''
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>Recipes</h1>
      <p>{{ showFavorites ? 'Your saved favorites' : 'Discover and save home cooking' }}</p>
    </div>

    <div class="search-bar">
      <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8"/>
        <path d="m21 21-4.35-4.35"/>
      </svg>
      <input v-model="searchQuery" type="text" :placeholder="showFavorites ? 'Search favorites...' : 'Search recipes...'" />
    </div>

    <div class="filter-row">
      <button
        class="filter-btn"
        :class="{ active: !showFavorites }"
        @click="showFavorites = false"
      >All</button>
      <button
        class="filter-btn"
        :class="{ active: showFavorites }"
        @click="toggleFavorites"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1 1.1L12 21l7.8-7.5 1-1.1a5.5 5.5 0 0 0 0-7.8z"/>
        </svg>
        Favorites
      </button>
    </div>

    <div v-if="loading" class="empty-state">
      <p>Loading...</p>
    </div>

    <div v-else-if="filteredRecipes.length === 0" class="empty-state">
      <p v-if="searchQuery">No matching recipes</p>
      <p v-else-if="showFavorites">No favorites yet</p>
      <p v-else>No recipes yet, tap + to create one</p>
    </div>

    <div v-else class="card-grid">
      <RecipeCard v-for="recipe in filteredRecipes" :key="recipe.id" :recipe="recipe" />
    </div>

    <router-link to="/recipe/new" class="fab">+</router-link>
  </div>
</template>

<style scoped>
.filter-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: var(--radius-full);
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  border: 1px solid var(--border-light);
  transition: all 0.2s;
}

.filter-btn.active {
  background: var(--text-primary);
  color: var(--text-white);
  border-color: var(--text-primary);
}
</style>
