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
.markdown :deep(.math-list-item) {
  margin-left: 1.25rem;
  list-style-type: disc;
  padding-left: 0.25rem;
}
.markdown :deep(.math-block) {
  display: block;
  text-align: center;
  margin: 0.75rem 0;
  padding: 0.75rem 1rem;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: 8px;
  font-family: 'Cambria Math', 'STIX Two Math', 'Computer Modern', 'Times New Roman', serif;
  font-size: 1.15rem;
  overflow-x: auto;
}
.markdown :deep(.math-inline) {
  font-family: 'Cambria Math', 'STIX Two Math', 'Computer Modern', 'Times New Roman', serif;
  padding: 0 0.2rem;
  font-size: 1.05rem;
  background: var(--surface-muted);
  border-radius: 4px;
}
.markdown :deep(.math-frac) {
  display: inline-flex;
  flex-direction: column;
  vertical-align: middle;
  text-align: center;
  padding: 0 0.2em;
}
.markdown :deep(.math-num) {
  border-bottom: 1px solid currentColor;
  padding: 0 0.1em;
  font-size: 0.9em;
}
.markdown :deep(.math-den) {
  padding: 0 0.1em;
  font-size: 0.9em;
}
.markdown :deep(.math-sqrt-symbol) {
  font-size: 1.1em;
  margin-right: 1px;
}
.markdown :deep(.math-sqrt-body) {
  border-top: 1px solid currentColor;
  padding-top: 1px;
  display: inline-block;
}
</style>
