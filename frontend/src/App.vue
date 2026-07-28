<script setup>
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'
import { computed } from 'vue'

const route = useRoute()
const showNav = computed(() => route.meta.tab !== undefined)
</script>

<template>
  <router-view />
  <NavBar v-if="showNav" />
</template>
