<script setup>
defineProps({
  recipe: { type: Object, required: true }
})
</script>

<template>
  <router-link :to="`/recipe/${recipe.id}`" class="recipe-card">
    <div class="card-image">
      <img
        v-if="recipe.imageUrl"
        :src="recipe.imageUrl"
        :alt="recipe.title"
      />
      <div v-else class="card-placeholder">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#C7C7CC" stroke-width="2">
          <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
        </svg>
      </div>
      <span v-if="recipe.sourceType" class="card-badge">{{ recipe.sourceType }}</span>
    </div>
    <div class="card-body">
      <h3 class="card-title">{{ recipe.title }}</h3>
    </div>
  </router-link>
</template>

<style scoped>
.recipe-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  display: block;
  transition: transform 0.2s;
}

.recipe-card:active {
  transform: scale(0.98);
}

.card-image {
  position: relative;
  aspect-ratio: 16 / 9;
  background: var(--category-bg);
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--input-bg);
}

.card-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: var(--text-white);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 500;
  text-transform: uppercase;
}

.card-body {
  padding: 10px 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
