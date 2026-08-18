<template>
  <form class="note-editor" @submit.prevent="save">
    <div class="field">
      <label for="note-title">Title</label>
      <input id="note-title" v-model="title" maxlength="280" required />
    </div>

    <!-- Reminder & Notification Panel -->
    <div class="field reminder-card">
      <div class="reminder-header">
        <label for="note-reminder" class="reminder-label">
          🔔 Reminder & Notification
        </label>
        <button
          v-if="reminderAtLocal"
          type="button"
          class="button subtle danger font-small"
          @click="clearReminder"
        >
          Remove Reminder
        </button>
      </div>
      <div class="reminder-body">
        <input
          id="note-reminder"
          v-model="reminderAtLocal"
          type="datetime-local"
          class="datetime-input"
        />
        <div v-if="reminderAtLocal" class="offset-section">
          <span class="muted font-small font-bold">Alert timing:</span>
          <div class="chip-group">
            <button
              v-for="opt in REMINDER_OPTIONS"
              :key="opt.offsetMinutes"
              type="button"
              class="chip-button"
              :class="{ active: selectedOffsets.includes(opt.offsetMinutes) }"
              @click="toggleOffset(opt.offsetMinutes)"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="toolbar">
      <button class="button" type="button" @click="preview = !preview">
        {{ preview ? 'Edit Markdown' : 'Preview Note' }}
      </button>
      <button
        v-if="!preview"
        class="button subtle"
        type="button"
        title="Insert Math Equation"
        @click="insertMathTemplate"
      >
        ∑ Math
      </button>
      <ImageUploader
        v-if="!preview"
        :note-id="currentNoteId"
        :prepare-note="prepareNoteForImage"
        @uploaded="handleImageUploaded"
      />
    </div>

    <div v-if="preview" class="preview card">
      <MarkdownPreview :content="content" />
    </div>
    <div v-else class="field">
      <label for="note-content">Markdown Content</label>
      <textarea
        id="note-content"
        ref="contentTextarea"
        v-model="content"
        rows="16"
        placeholder="Write your note in Markdown… (Type '-' for auto list)"
        @keydown.enter="handleKeydown"
      />
    </div>

    <!-- Dedicated Attached Images Gallery (Separate from text input) -->
    <div v-if="attachedImages.length" class="attached-section card">
      <div class="attached-header">
        <h4 class="attached-title">🖼️ Uploaded Images ({{ attachedImages.length }})</h4>
        <span class="muted font-small">Images are attached to this note</span>
      </div>

      <div class="image-grid">
        <div v-for="img in attachedImages" :key="img.id" class="image-card">
          <div
            class="thumb-wrapper clickable-thumb"
            title="Click to view full image and zoom"
            @click="img.url && !img.hasError && openZoom(img.url, img.filename)"
          >
            <img
              v-if="img.url && !img.hasError"
              :src="img.url"
              :alt="img.filename"
              class="thumb"
              @error="handleImgError(img.id)"
            />
            <span v-else-if="img.hasError" class="loading-thumb error-thumb">Failed to load preview</span>
            <span v-else class="loading-thumb">Loading image…</span>
          </div>

          <div class="image-details">
            <span class="filename" :title="img.filename">{{ img.filename }}</span>
            <button
              type="button"
              class="button subtle danger font-small remove-btn"
              @click="removeAttachedImage(img.id)"
            >
              Remove
            </button>
          </div>
        </div>
      </div>
    </div>

    <p v-if="error" class="notice">{{ error }}</p>

    <div class="actions">
      <button class="button primary" :disabled="saving">
        {{ saving ? 'Saving…' : 'Save note' }}
      </button>
      <button class="button subtle" type="button" @click="$emit('cancel')">Cancel</button>
    </div>

    <ImageLightbox
      v-if="selectedImage"
      :is-open="!!selectedImage"
      :src="selectedImage.src"
      :title="selectedImage.title"
      @close="selectedImage = null"
    />
  </form>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import ImageUploader from './ImageUploader.vue'
import MarkdownPreview from './MarkdownPreview.vue'
import ImageLightbox from '../../../components/ui/ImageLightbox.vue'
import { deleteImage, imageAccess } from '../note.api'
import { imageIds, insertAtCursor } from '../../../lib/markdown/noteImage'
import { REMINDER_OPTIONS } from '../composables/useNoteNotifications'
import type { Note, NoteImage, NoteInput } from '../note.types'

interface AttachedItem {
  id: string
  url?: string
  filename: string
  hasError?: boolean
}

function toDatetimeLocal(isoString?: string | null): string {
  if (!isoString) return ''
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

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

const title = ref(props.initial?.title ?? '')
const content = ref(props.initial?.content_md ?? '')
const reminderAtLocal = ref(toDatetimeLocal(props.initial?.reminder_at))
const selectedOffsets = ref<number[]>(
  props.initial?.reminder_offsets?.length ? [...props.initial.reminder_offsets] : [0],
)

function clearReminder() {
  reminderAtLocal.value = ''
}

function toggleOffset(minutes: number) {
  if (selectedOffsets.value.includes(minutes)) {
    if (selectedOffsets.value.length > 1) {
      selectedOffsets.value = selectedOffsets.value.filter((m) => m !== minutes)
    }
  } else {
    selectedOffsets.value.push(minutes)
  }
}

const selectedImage = ref<{ src: string; title: string } | null>(null)
const contentTextarea = ref<HTMLTextAreaElement | null>(null)

function insertMathTemplate() {
  const mathSnippet = '\n$$ f(x) = x^2 $$\n'
  if (contentTextarea.value) {
    insertAtCursor(contentTextarea.value, mathSnippet)
  } else {
    content.value += (content.value ? '\n' : '') + mathSnippet
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
    const textarea = contentTextarea.value || (e.target as HTMLTextAreaElement)
    if (!textarea) return

    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    if (start !== end) return

    const val = textarea.value
    const lineStart = val.lastIndexOf('\n', start - 1) + 1
    const currentLine = val.substring(lineStart, start)

    const listMatch = currentLine.match(/^(\s*-\s+)(.*)/)
    if (listMatch && listMatch[1] && listMatch[2] !== undefined) {
      e.preventDefault()
      const indentAndDash = listMatch[1]
      const rest = listMatch[2].trim()

      if (!rest) {
        const newText = val.substring(0, lineStart) + val.substring(start)
        content.value = newText
        setTimeout(() => {
          if (contentTextarea.value) {
            contentTextarea.value.selectionStart = contentTextarea.value.selectionEnd = lineStart
          }
        }, 0)
      } else {
        const insertion = '\n' + indentAndDash
        const newText = val.substring(0, start) + insertion + val.substring(start)
        content.value = newText
        setTimeout(() => {
          if (contentTextarea.value) {
            contentTextarea.value.selectionStart = contentTextarea.value.selectionEnd = start + insertion.length
          }
        }, 0)
      }
    }
  }
}

function openZoom(src: string, title: string) {
  selectedImage.value = { src, title }
}

function handleImgError(id: string) {
  const item = attachedImages.value.find((img) => img.id === id)
  if (item) {
    item.hasError = true
  }
}

const preview = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const currentNoteId = ref(props.noteId)
const attachedImages = ref<AttachedItem[]>([])

watch(
  () => props.noteId,
  (noteId) => {
    currentNoteId.value = noteId
  },
)

async function loadExistingImages() {
  const rawContent = props.initial?.content_md ?? ''
  const ids = imageIds(rawContent)

  let cleanText = rawContent
  for (const id of ids) {
    cleanText = cleanText.replace(new RegExp(`\\s*note-image:${id}\\s*`, 'g'), ' ')
  }
  content.value = cleanText.trim()

  for (const id of ids) {
    if (!attachedImages.value.some((img) => img.id === id)) {
      try {
        const access = await imageAccess(id)
        attachedImages.value.push({
          id,
          url: access.url,
          filename: `Attached Image`,
        })
      } catch {
        // Image unavailable
      }
    }
  }
}

onMounted(() => void loadExistingImages())

async function handleImageUploaded(res: { image: NoteImage; token: string }) {
  error.value = null
  try {
    const access = await imageAccess(res.image.id)
    attachedImages.value.push({
      id: res.image.id,
      url: access.url,
      filename: res.image.original_filename || 'Uploaded Image',
    })
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Failed to load image preview.'
  }
}

async function removeAttachedImage(id: string) {
  if (!confirm('Are you sure you want to delete this image attachment?')) return
  try {
    await deleteImage(id)
    attachedImages.value = attachedImages.value.filter((img) => img.id !== id)
    content.value = content.value.replace(new RegExp(`note-image:${id}`, 'g'), '').trim()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Failed to delete image.'
  }
}

async function prepareNoteForImage() {
  if (currentNoteId.value) return currentNoteId.value
  const trimmed = title.value.trim()
  if (!trimmed) throw new Error('Add a title before uploading an image.')
  if (!props.createForImage) throw new Error('Save the note before uploading an image.')

  error.value = null
  saving.value = true
  try {
    const note = await props.createForImage({
      title: trimmed,
      content_md: content.value,
      reminder_at: reminderAtLocal.value ? new Date(reminderAtLocal.value).toISOString() : null,
      reminder_offsets: selectedOffsets.value,
    })
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

  let fullContent = content.value
  const existingIds = new Set(imageIds(fullContent))
  const missingImages = attachedImages.value.filter((img) => !existingIds.has(img.id))
  if (missingImages.length) {
    fullContent =
      fullContent +
      (fullContent ? '\n\n' : '') +
      missingImages.map((img) => `note-image:${img.id}`).join('\n')
  }

  const reminder_at = reminderAtLocal.value ? new Date(reminderAtLocal.value).toISOString() : null
  const reminder_offsets = selectedOffsets.value

  emit('save', {
    title: trimmed,
    content_md: fullContent,
    reminder_at,
    reminder_offsets,
  })
  saving.value = false
}
</script>

<style scoped>
.note-editor {
  display: grid;
  gap: 1rem;
}
.reminder-card {
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-muted, #f8f9fa);
  display: grid;
  gap: 0.75rem;
}
.reminder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.reminder-label {
  font-weight: 600;
  font-size: 0.95rem;
}
.reminder-body {
  display: grid;
  gap: 0.75rem;
}
.datetime-input {
  padding: 0.5rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  max-width: 20rem;
  font-family: inherit;
}
.offset-section {
  display: grid;
  gap: 0.4rem;
}
.font-bold {
  font-weight: 600;
}
.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.chip-button {
  padding: 0.3rem 0.65rem;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: white;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.chip-button.active {
  background: var(--accent, #2563eb);
  color: white;
  border-color: var(--accent, #2563eb);
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
.attached-section {
  padding: 1rem;
  display: grid;
  gap: 0.75rem;
  background: var(--surface-muted);
}
.attached-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.attached-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}
.font-small {
  font-size: 0.8rem;
}
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 11rem), 1fr));
  gap: 0.75rem;
}
.image-card {
  background: white;
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.thumb-wrapper {
  width: 100%;
  height: 10rem;
  background: #f4f5f1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.clickable-thumb {
  cursor: zoom-in;
}
.thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.loading-thumb {
  font-size: 0.8rem;
  color: var(--text-muted);
}
.image-details {
  padding: 0.6rem;
  display: grid;
  gap: 0.4rem;
}
.filename {
  font-size: 0.8rem;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.remove-btn {
  width: 100%;
}
</style>

