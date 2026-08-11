<!-- eslint-disable vue/no-v-html -->
<template>
  <article class="markdown" @click="handleClick" v-html="html"></article>
  <ImageLightbox
    v-if="zoomImg"
    :is-open="!!zoomImg"
    :src="zoomImg.src"
    :title="zoomImg.title"
    @close="zoomImg = null"
  />
</template>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import ImageLightbox from '../../../components/ui/ImageLightbox.vue'
import { imageAccess } from '../note.api'
import { imageIds, renderMarkdown } from '../../../lib/markdown/noteImage'
const props = defineProps<{ content: string }>()
const urls = ref(new Map<string, string>())
const zoomImg = ref<{ src: string; title: string } | null>(null)

async function resolve() {
  for (const id of imageIds(props.content)) {
    if (!urls.value.has(id))
      try {
        const access = await imageAccess(id)
        urls.value.set(id, access.url)
      } catch {
        /* unavailable images remain non-rendered */
      }
  }
}
onMounted(() => void resolve())
watch(
  () => props.content,
  () => void resolve(),
)
const html = computed(() => renderMarkdown(props.content, urls.value))

function handleClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target && target.tagName === 'IMG') {
    const img = target as HTMLImageElement
    zoomImg.value = { src: img.src, title: img.alt || 'Note Image' }
  }
}
</script>
<style scoped>
.markdown {
  line-height: 1.65;
  overflow-wrap: anywhere;
}
.markdown :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  cursor: zoom-in;
}
.markdown :deep(code) {
  padding: 0.12rem 0.3rem;
  border-radius: 4px;
  background: var(--surface-muted);
}
.markdown :deep(blockquote) {
  margin-left: 0;
  padding-left: 1rem;
  border-left: 3px solid var(--border);
  color: var(--text-muted);
}
</style>
