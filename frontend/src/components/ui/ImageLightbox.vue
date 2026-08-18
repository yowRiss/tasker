<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      class="lightbox-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="Image zoom view"
      @click.self="close"
    >
      <div class="lightbox-toolbar">
        <span v-if="title" class="lightbox-title">{{ title }}</span>
        <div class="toolbar-actions">
          <button type="button" class="tool-btn" title="Zoom Out" @click="zoomOut">-</button>
          <span class="zoom-level">{{ Math.round(zoomScale * 100) }}%</span>
          <button type="button" class="tool-btn" title="Zoom In" @click="zoomIn">+</button>
          <button type="button" class="tool-btn" title="Reset Zoom" @click="resetZoom">100%</button>
          <button type="button" class="tool-btn close-btn" title="Close (Esc)" @click="close">
            ✕
          </button>
        </div>
      </div>

      <div class="lightbox-content" @wheel.prevent="handleWheel" @click.self="close">
        <img
          :src="src"
          :alt="alt || 'Zoomed view'"
          class="lightbox-img"
          :style="imgStyle"
          @click="toggleZoom"
        />
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'

const props = defineProps<{
  src: string
  alt?: string
  title?: string
  isOpen: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const zoomScale = ref(1)

watch(
  () => props.isOpen,
  (val) => {
    if (val) {
      zoomScale.value = 1
      window.addEventListener('keydown', handleKeyDown)
    } else {
      window.removeEventListener('keydown', handleKeyDown)
    }
  },
)

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
}

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

function close() {
  emit('close')
}

function zoomIn() {
  zoomScale.value = Math.min(zoomScale.value + 0.25, 4)
}

function zoomOut() {
  zoomScale.value = Math.max(zoomScale.value - 0.25, 0.5)
}

function resetZoom() {
  zoomScale.value = 1
}

function toggleZoom() {
  zoomScale.value = zoomScale.value === 1 ? 2 : 1
}

function handleWheel(e: WheelEvent) {
  if (e.deltaY < 0) zoomIn()
  else if (e.deltaY > 0) zoomOut()
}

const imgStyle = computed(() => ({
  transform: `scale(${zoomScale.value})`,
  cursor: zoomScale.value === 1 ? 'zoom-in' : 'zoom-out',
}))
</script>

<style scoped>
.lightbox-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.88);
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  user-select: none;
}
.lightbox-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.25rem;
  background: rgba(0, 0, 0, 0.4);
  color: white;
}
.lightbox-title {
  font-size: 0.9rem;
  font-weight: 600;
  opacity: 0.9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}
.tool-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: white;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: background 0.15s;
}
.tool-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}
.close-btn {
  background: rgba(231, 76, 60, 0.6);
  border-color: rgba(231, 76, 60, 0.8);
}
.close-btn:hover {
  background: rgba(231, 76, 60, 0.9);
}
.zoom-level {
  font-size: 0.85rem;
  min-width: 3rem;
  text-align: center;
  font-weight: 600;
}
.lightbox-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  padding: 1.5rem;
}
.lightbox-img {
  max-width: 90vw;
  max-height: 80vh;
  object-fit: contain;
  transition: transform 0.2s ease-out;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  border-radius: 6px;
}
@media (max-width: 500px) {
  .lightbox-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 0.5rem;
  }
  .toolbar-actions {
    justify-content: space-between;
    width: 100%;
  }
  .tool-btn {
    padding: 0.3rem 0.5rem;
  }
}
</style>
