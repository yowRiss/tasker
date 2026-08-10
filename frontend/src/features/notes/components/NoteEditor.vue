<template>
  <form class="note-editor" @submit.prevent="save">
    <div class="field">
      <label for="note-title">Title</label
      ><input id="note-title" v-model="title" maxlength="280" required />
    </div>
    <div class="toolbar">
      <button class="button" type="button" @click="preview = !preview">
        {{ preview ? 'Edit Markdown' : 'Preview' }}</button
      ><ImageUploader
        v-if="!preview"
        :note-id="currentNoteId"
        :prepare-note="prepareNoteForImage"
        @uploaded="insert"
      />
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
import { ref, watch } from 'vue'
import ImageUploader from './ImageUploader.vue'
import MarkdownPreview from './MarkdownPreview.vue'
import { insertAtCursor } from '../../../lib/markdown/noteImage'
import type { Note, NoteInput } from '../note.types'
const props = defineProps<{
  initial?: NoteInput
  noteId?: string
  createForImage?: (input: NoteInput) => Promise<Note>
}>()
const emit = defineEmits<{
  save: [input: NoteInput]
  cancel: []
  draftCreated: [note: Note]
}>()
const title = ref(props.initial?.title ?? ''),
  content = ref(props.initial?.content_md ?? ''),
  preview = ref(false),
  saving = ref(false),
  error = ref<string | null>(null),
  area = ref<HTMLTextAreaElement | null>(null),
  currentNoteId = ref(props.noteId)
watch(
  () => props.noteId,
  (noteId) => {
    currentNoteId.value = noteId
  },
)
function insert(token: string) {
  if (area.value) insertAtCursor(area.value, token)
  else content.value += `${content.value ? '\n\n' : ''}${token}`
}
async function prepareNoteForImage() {
  if (currentNoteId.value) return currentNoteId.value
  const trimmed = title.value.trim()
  if (!trimmed) throw new Error('Add a title before uploading an image.')
  if (!props.createForImage) throw new Error('Save the note before uploading an image.')

  error.value = null
  saving.value = true
  try {
    const note = await props.createForImage({ title: trimmed, content_md: content.value })
    currentNoteId.value = note.id
    emit('draftCreated', note)
    return note.id
  } catch (cause: unknown) {
    const message = cause instanceof Error ? cause.message : 'Unable to prepare the note for upload.'
    error.value = message
    throw new Error(message)
  } finally {
    saving.value = false
  }
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
