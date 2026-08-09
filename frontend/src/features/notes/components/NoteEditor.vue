<template>
  <form class="note-editor" @submit.prevent="save">
    <div class="field">
      <label for="note-title">Title</label
      ><input id="note-title" v-model="title" maxlength="280" required />
    </div>
    <div class="toolbar">
      <button class="button" type="button" @click="preview = !preview">
        {{ preview ? 'Edit Markdown' : 'Preview' }}</button
      ><ImageUploader v-if="noteId && !preview" :note-id="noteId" @uploaded="insert" />
    </div>
    <div v-if="preview" class="preview card"><MarkdownPreview :content="content" /></div>
    <div v-else class="field">
      <label for="note-content">Markdown</label
      ><textarea
        id="note-content"
        ref="area"
        v-model="content"
        rows="18"
        placeholder="Write in Markdown…"
      />
    </div>
    <p v-if="error" class="notice">{{ error }}</p>
    <div class="actions">
      <button class="button primary" :disabled="saving">
        {{ saving ? 'Saving…' : 'Save note' }}</button
      ><button class="button subtle" type="button" @click="$emit('cancel')">Cancel</button>
    </div>
  </form>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import ImageUploader from './ImageUploader.vue'
import MarkdownPreview from './MarkdownPreview.vue'
import { insertAtCursor } from '../../../lib/markdown/noteImage'
import type { NoteInput } from '../note.types'
const props = defineProps<{ initial?: NoteInput; noteId?: string }>()
const emit = defineEmits<{ save: [input: NoteInput]; cancel: [] }>()
const title = ref(props.initial?.title ?? ''),
  content = ref(props.initial?.content_md ?? ''),
  preview = ref(false),
  saving = ref(false),
  error = ref<string | null>(null),
  area = ref<HTMLTextAreaElement | null>(null)
function insert(token: string) {
  if (area.value) insertAtCursor(area.value, token)
  else content.value += `${content.value ? '\n\n' : ''}${token}`
}
function save() {
  const trimmed = title.value.trim()
  if (!trimmed) {
    error.value = 'A note title is required.'
    return
  }
  saving.value = true
  emit('save', { title: trimmed, content_md: content.value })
  saving.value = false
}
</script>
<style scoped>
.note-editor {
  display: grid;
  gap: 1rem;
}
.toolbar,
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.preview {
  min-height: 20rem;
  padding: 1rem;
}
</style>
