<!-- eslint-disable vue/no-v-html -->
<template><article class="markdown" v-html="html"></article></template>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { imageAccess } from '../note.api'
import { imageIds, renderMarkdown } from '../../../lib/markdown/noteImage'
const props = defineProps<{ content: string }>()
const urls = ref(new Map<string, string>())
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
</script>
<style scoped>
.markdown {
  line-height: 1.65;
  overflow-wrap: anywhere;
}
.markdown :deep(img) {
  max-width: 100%;
  border-radius: 8px;
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
