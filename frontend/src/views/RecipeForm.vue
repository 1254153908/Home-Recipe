<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createRecipe, updateRecipe, getRecipeDetail, aiRecognize, uploadFile } from '../api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => route.name === 'RecipeEdit')
const recipeId = computed(() => route.params.id)

const title = ref('')
const imageUrl = ref('')
const sourceType = ref('')
const sourceUrl = ref('')
const steps = ref([])
const ingredients = ref([])
const seasonings = ref([])
const saving = ref(false)
const toast = ref('')

// AI recognize
const aiContent = ref('')
const aiRecognizing = ref(false)
const showAiPanel = ref(false)
const aiMode = ref('link')        // 'link' | 'image'
const aiImageFiles = ref([])      // 批量选择的图片 { file, previewUrl }
const aiImagePreviews = ref([])   // 预览 URL 列表

// Image upload
const uploading = ref({})

function showToast(msg) {
  toast.value = msg
  setTimeout(() => { toast.value = '' }, 2000)
}

function triggerUpload(target) {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    uploading.value[target] = true
    try {
      const url = await uploadFile(file)
      if (target === 'cover') {
        imageUrl.value = url
      } else if (target.startsWith('step-')) {
        const idx = parseInt(target.split('-')[1])
        steps.value[idx].imageUrl = url
      }
      showToast('Image uploaded')
    } catch {
      showToast('Upload failed')
    } finally {
      uploading.value[target] = false
    }
  }
  input.click()
}

function addStep() {
  steps.value.push({ content: '', imageUrl: '' })
}

function removeStep(index) {
  steps.value.splice(index, 1)
}

function addIngredient() {
  ingredients.value.push({ name: '', quantity: '', unit: '' })
}

function removeIngredient(index) {
  ingredients.value.splice(index, 1)
}

function addSeasoning() {
  seasonings.value.push({ name: '', quantity: '', unit: '' })
}

function removeSeasoning(index) {
  seasonings.value.splice(index, 1)
}

function applyDraft(result, linkUrl) {
  title.value = result.title || ''
  imageUrl.value = result.imageUrl || ''
  sourceUrl.value = linkUrl || ''
  if (result.steps?.length) {
    steps.value = result.steps.map(s => ({ content: s.content || '', imageUrl: s.imageUrl || '' }))
  }
  if (result.ingredients?.length) {
    ingredients.value = result.ingredients.map(i => ({ name: i.name || '', quantity: i.quantity || '', unit: i.unit || '' }))
  }
  if (result.seasonings?.length) {
    seasonings.value = result.seasonings.map(s => ({ name: s.name || '', quantity: s.quantity || '', unit: s.unit || '' }))
  }
}

async function handleAiRecognize() {
  aiRecognizing.value = true
  try {
    if (aiMode.value === 'link') {
      // 链接模式
      if (!aiContent.value.trim()) { showToast('Enter a URL'); return }
      const result = await aiRecognize({
        sourceType: 'link',
        content: aiContent.value.trim()
      })
      applyDraft(result, aiContent.value.trim())
    } else {
      // 图片模式：批量选择 → base64 → Vision 识别
      if (aiImagePreviews.value.length === 0) { showToast('Select at least one image'); return }
      // 取第一张识别（可扩展为逐张合并结果）
      const file = aiImageFiles.value[0]
      if (!file) { showToast('No image selected'); return }
      const base64 = await new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result)
        reader.onerror = reject
        reader.readAsDataURL(file)
      })
      const result = await aiRecognize({
        sourceType: 'image',
        content: base64
      })
      applyDraft(result, '')
    }
    showToast('Recognition complete')
    showAiPanel.value = false
  } catch {
    showToast('Recognition failed, check AI service')
  } finally {
    aiRecognizing.value = false
  }
}

function handleImageSelect(e) {
  const files = Array.from(e.target.files || [])
  if (files.length === 0) return
  aiImageFiles.value = files
  // 生成预览 URL（blob, 不存数据库）
  aiImagePreviews.value = files.map(f => URL.createObjectURL(f))
}

function clearImages() {
  aiImagePreviews.value.forEach(url => URL.revokeObjectURL(url))
  aiImagePreviews.value = []
  aiImageFiles.value = []
}

onMounted(async () => {
  if (isEdit.value) {
    const detail = await getRecipeDetail(recipeId.value)
    title.value = detail.recipe.title
    imageUrl.value = detail.recipe.imageUrl || ''
    sourceType.value = detail.recipe.sourceType || ''
    sourceUrl.value = detail.recipe.sourceUrl || ''
    steps.value = detail.steps?.length ? detail.steps.map(s => ({ content: s.content, imageUrl: s.imageUrl || '' })) : []
    ingredients.value = detail.ingredients?.length ? detail.ingredients.map(i => ({ name: i.name, quantity: i.quantity, unit: i.unit })) : []
    seasonings.value = detail.seasonings?.length ? detail.seasonings.map(s => ({ name: s.name, quantity: s.quantity, unit: s.unit })) : []
  }
})

async function handleSubmit() {
  if (!title.value.trim()) {
    showToast('Please enter a recipe name')
    return
  }
  saving.value = true
  try {
    const data = {
      title: title.value.trim(),
      imageUrl: imageUrl.value.trim(),
      sourceType: sourceType.value.trim(),
      sourceUrl: sourceUrl.value.trim(),
      steps: steps.value,
      ingredients: ingredients.value,
      seasonings: seasonings.value
    }
    if (isEdit.value) {
      await updateRecipe(recipeId.value, data)
      showToast('Updated')
      router.replace(`/recipe/${recipeId.value}`)
    } else {
      const result = await createRecipe(data)
      showToast('Created')
      router.replace(`/recipe/${result.recipe.id}`)
    }
  } catch {
    showToast('Save failed')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h1>{{ isEdit ? 'Edit Recipe' : 'New Recipe' }}</h1>
    </div>

    <!-- AI Recognize Panel (only in create mode) -->
    <div v-if="!isEdit" class="ai-section">
      <button class="btn btn-outline btn-block ai-trigger" @click="showAiPanel = !showAiPanel">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="9"/>
          <path d="M8 12h8M12 8v8"/>
        </svg>
        {{ showAiPanel ? 'Hide AI Recognize' : 'AI Recognize — auto-fill form' }}
      </button>

      <div v-if="showAiPanel" class="ai-panel">
        <!-- Mode tabs -->
        <div class="ai-tabs">
          <button class="ai-tab" :class="{ active: aiMode === 'link' }" @click="aiMode = 'link'">Link</button>
          <button class="ai-tab" :class="{ active: aiMode === 'image' }" @click="aiMode = 'image'">Image</button>
        </div>

        <!-- Link mode -->
        <div v-if="aiMode === 'link'">
          <div class="form-group">
            <label class="form-label">Paste recipe URL</label>
            <p class="ai-hint">Supports xiachufang.com (fast HTML parsing, no LLM cost) and other recipe sites</p>
            <div class="ai-input-row">
              <input v-model="aiContent" class="form-input" placeholder="https://www.xiachufang.com/recipe/..." />
              <button class="btn btn-accent btn-sm" :disabled="aiRecognizing || !aiContent.trim()" @click="handleAiRecognize">
                {{ aiRecognizing ? '...' : 'Recognize' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Image mode -->
        <div v-if="aiMode === 'image'">
          <div class="form-group">
            <label class="form-label">Select food photos</label>
            <p class="ai-hint">Take photos of recipe screenshots (Xiaohongshu, TikTok, etc.) — images are used for recognition only, not saved</p>
            <label class="img-select-area" :class="{ hasFiles: aiImagePreviews.length > 0 }">
              <input type="file" accept="image/*" multiple @change="handleImageSelect" style="display:none" />
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <path d="m21 15-5-5L5 21"/>
              </svg>
              <span>{{ aiImagePreviews.length > 0 ? `${aiImagePreviews.length} image(s) selected` : 'Tap to select from gallery' }}</span>
            </label>
            <div v-if="aiImagePreviews.length > 0" class="img-previews">
              <div class="preview-thumb" v-for="(url, i) in aiImagePreviews" :key="i">
                <img :src="url" />
              </div>
            </div>
          </div>
          <div class="ai-actions">
            <button v-if="aiImagePreviews.length > 0" class="btn btn-outline btn-sm" @click="clearImages">Clear</button>
            <button class="btn btn-accent btn-sm" :disabled="aiRecognizing || aiImagePreviews.length === 0" @click="handleAiRecognize">
              {{ aiRecognizing ? 'Recognizing...' : 'Recognize' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">Recipe name *</label>
      <input v-model="title" class="form-input" placeholder="e.g. Tomato Egg Stir-fry" />
    </div>

    <div class="form-group">
      <label class="form-label">Cover image</label>
      <div class="img-url-row">
        <input v-model="imageUrl" class="form-input" placeholder="https://... or upload" />
        <button class="btn btn-outline btn-sm upload-btn" :disabled="uploading['cover']" @click="triggerUpload('cover')">
          {{ uploading['cover'] ? '...' : 'Upload' }}
        </button>
      </div>
      <img v-if="imageUrl" :src="imageUrl" class="preview-img" />
    </div>

    <div class="form-row">
      <div class="form-group">
        <label class="form-label">Source type</label>
        <select v-model="sourceType" class="form-input">
          <option value="">--</option>
          <option value="link">Link</option>
          <option value="video">Video</option>
          <option value="image">Image</option>
        </select>
      </div>
      <div class="form-group flex-1">
        <label class="form-label">Source URL</label>
        <input v-model="sourceUrl" class="form-input" placeholder="https://..." />
      </div>
    </div>

    <!-- Steps -->
    <div class="section-editor">
      <div class="section-editor-header">
        <h2 class="section-title">Steps</h2>
        <button class="btn btn-outline btn-sm" @click="addStep">+ Add</button>
      </div>
      <div v-if="steps.length === 0" class="text-muted" style="margin-bottom:12px">No steps yet</div>
      <div class="step-editor-item" v-for="(step, i) in steps" :key="i">
        <span class="step-editor-no">{{ i + 1 }}</span>
        <div class="step-editor-body">
          <textarea v-model="step.content" class="form-input" placeholder="Step description" rows="2" />
          <div class="img-url-row" style="margin-top:8px">
            <input v-model="step.imageUrl" class="form-input" placeholder="Step image URL (optional)" />
            <button class="btn btn-outline btn-sm upload-btn" :disabled="uploading[`step-${i}`]" @click="triggerUpload(`step-${i}`)">
              {{ uploading[`step-${i}`] ? '...' : 'Up' }}
            </button>
          </div>
        </div>
        <button class="btn-remove" @click="removeStep(i)">&times;</button>
      </div>
    </div>

    <!-- Ingredients -->
    <div class="section-editor">
      <div class="section-editor-header">
        <h2 class="section-title">Ingredients</h2>
        <button class="btn btn-outline btn-sm" @click="addIngredient">+ Add</button>
      </div>
      <div v-for="(item, i) in ingredients" :key="i" class="item-editor-row">
        <input v-model="item.name" class="form-input" placeholder="Name" style="flex:2" />
        <input v-model="item.quantity" class="form-input" placeholder="Qty" style="flex:1" />
        <input v-model="item.unit" class="form-input" placeholder="Unit" style="flex:1" />
        <button class="btn-remove" @click="removeIngredient(i)">&times;</button>
      </div>
    </div>

    <!-- Seasonings -->
    <div class="section-editor">
      <div class="section-editor-header">
        <h2 class="section-title">Seasonings</h2>
        <button class="btn btn-outline btn-sm" @click="addSeasoning">+ Add</button>
      </div>
      <div v-for="(item, i) in seasonings" :key="i" class="item-editor-row">
        <input v-model="item.name" class="form-input" placeholder="Name" style="flex:2" />
        <input v-model="item.quantity" class="form-input" placeholder="Qty" style="flex:1" />
        <input v-model="item.unit" class="form-input" placeholder="Unit" style="flex:1" />
        <button class="btn-remove" @click="removeSeasoning(i)">&times;</button>
      </div>
    </div>

    <div class="form-actions">
      <button class="btn btn-outline" @click="router.back">Cancel</button>
      <button class="btn btn-primary btn-block" :disabled="saving" @click="handleSubmit">
        {{ saving ? 'Saving...' : 'Save' }}
      </button>
    </div>

    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<style scoped>
.ai-section {
  margin-bottom: 20px;
}

.ai-trigger {
  justify-content: center;
  gap: 8px;
  color: var(--accent);
  border-color: var(--accent);
  padding: 12px;
  font-size: 15px;
}

.ai-panel {
  margin-top: 12px;
  padding: 16px;
  background: var(--card-bg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}

.ai-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.ai-tab {
  padding: 6px 16px;
  border-radius: var(--radius-full);
  background: var(--input-bg);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  border: 1px solid transparent;
  transition: all 0.15s;
}

.ai-tab.active {
  background: var(--text-primary);
  color: var(--text-white);
}

.ai-hint {
  font-size: 11px;
  color: var(--text-placeholder);
  margin-bottom: 6px;
}

.img-select-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 28px;
  border: 2px dashed var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-placeholder);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.15s;
}

.img-select-area:hover,
.img-select-area.hasFiles {
  border-color: var(--accent);
  color: var(--accent);
}

.img-previews {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  overflow-x: auto;
}

.preview-thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
}

.preview-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ai-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.ai-input-row {
  display: flex;
  gap: 8px;
}

.ai-input-row .form-input {
  flex: 1;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-row .form-group:first-child {
  width: 120px;
}

.flex-1 {
  flex: 1;
}

.img-url-row {
  display: flex;
  gap: 8px;
}

.img-url-row .form-input {
  flex: 1;
}

.upload-btn {
  flex-shrink: 0;
  white-space: nowrap;
}

.preview-img {
  margin-top: 8px;
  border-radius: var(--radius-sm);
  max-height: 160px;
}

.section-editor {
  margin-top: 20px;
}

.section-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.text-muted {
  color: var(--text-secondary);
  font-size: 14px;
}

.step-editor-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  align-items: flex-start;
}

.step-editor-no {
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
  margin-top: 4px;
}

.step-editor-body {
  flex: 1;
}

.item-editor-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
}

.btn-remove {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: transparent;
  color: var(--text-secondary);
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.btn-remove:hover {
  background: var(--input-bg);
  color: var(--danger);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.form-actions .btn-block {
  flex: 1;
}
</style>
