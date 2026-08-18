<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">Notes</h1>
        <p class="muted">Ideas, references, and useful context.</p>
      </div>
      <button class="button primary" @click="toggleCreate">
        {{ creating ? 'Close' : 'New note' }}
      </button>
    </header>
    <NoteEditor
      v-if="creating"
      :create-for-image="createDraftForImage"
      @save="saveCreate"
      @cancel="cancelCreate"
      @draft-created="draftNoteId = $event.id"
    />
    <div class="notes-toolbar">
      <input v-model="q" type="search" placeholder="Search notes" aria-label="Search notes" />
    </div>
    <p v-if="error" class="notice">
      {{ error }} <button type="button" @click="load">Retry</button>
    </p>
    <p v-else-if="loading" class="empty">Loading notes…</p>
    <ul v-else-if="notes.length" class="note-list">
      <li v-for="note in notes" :key="note.id" class="card note-card">
        <div class="note-card-main">
          <RouterLink :to="`/notes/${note.id}`" class="note-link">
            <div class="note-title-row">
              <strong>{{ note.title }}</strong>
              <span v-if="note.reminder_at" class="reminder-pill" :title="`Reminder set for ${formatReminder(note.reminder_at)}`">
                🔔 {{ formatReminder(note.reminder_at) }}
              </span>
            </div>
            <small>{{ excerpt(note.content_md) }}</small>
            <time>Updated {{ format(note.updated_at) }}</time>
          </RouterLink>
          <button
            type="button"
            class="button subtle danger delete-btn"
            title="Delete note"
            @click="handleDeleteNote(note.id)"
          >
            Delete
          </button>
        </div>
      </li>
    </ul>
    <p v-else class="card empty">No notes yet. Capture something worth keeping.</p>
  </section>
</template>
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import NoteEditor from '../features/notes/components/NoteEditor.vue'
import { useNoteNotifications } from '../features/notes/composables/useNoteNotifications'
import { createNote, deleteNote, listNotes, updateNote } from '../features/notes/note.api'
import type { Note, NoteInput } from '../features/notes/note.types'

const { startScheduler } = useNoteNotifications()
const router = useRouter(),
  notes = ref<Note[]>([]),
  q = ref(''),
  loading = ref(false),
  error = ref<string | null>(null),
  creating = ref(false),
  draftNoteId = ref<string | null>(null)
let timer: number | undefined
async function load() {
  loading.value = true
  error.value = null
  try {
    notes.value = (await listNotes({ q: q.value, limit: 50 })).items
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unable to load notes.'
  } finally {
    loading.value = false
  }
}
onMounted(() => {
  void load()
  startScheduler(() => notes.value)
})
watch(q, () => {
  clearTimeout(timer)
  timer = window.setTimeout(() => void load(), 250)
})
async function createDraftForImage(input: NoteInput) {
  const note = await createNote(input)
  draftNoteId.value = note.id
  return note
}
async function saveCreate(input: NoteInput) {
  try {
    const note = draftNoteId.value
      ? await updateNote(draftNoteId.value, input)
      : await createNote(input)
    draftNoteId.value = null
    creating.value = false
    await router.push(`/notes/${note.id}`)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unable to create note.'
  }
}
async function cancelCreate() {
  if (draftNoteId.value) {
    try {
      await deleteNote(draftNoteId.value)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Unable to discard the draft note.'
      return
    }
  }
  draftNoteId.value = null
  creating.value = false
}
function toggleCreate() {
  if (creating.value) void cancelCreate()
  else creating.value = true
}
async function handleDeleteNote(id: string) {
  if (!confirm('Permanently delete this note?')) return
  try {
    await deleteNote(id)
    notes.value = notes.value.filter((n) => n.id !== id)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unable to delete note.'
  }
}
function excerpt(markdown: string) {
  return (
    markdown
      .replace(/[#*_`>-]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
      .slice(0, 150) || 'Empty note'
  )
}
function format(value: string) {
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(
    new Date(value),
  )
}
function formatReminder(value: string) {
  const d = new Date(value)
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(d)
}
</script>
<style scoped>
.notes-toolbar {
  margin: 1rem 0;
}
.notes-toolbar input {
  width: min(30rem, 100%);
  padding: 0.65rem;
  border: 1px solid var(--border);
  border-radius: 7px;
}
.note-list {
  display: grid;
  gap: 0.6rem;
  padding: 0;
  list-style: none;
}
.note-card-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
}
.note-link {
  display: grid;
  gap: 0.4rem;
  flex: 1;
  text-decoration: none;
}
.note-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.reminder-pill {
  font-size: 0.75rem;
  padding: 0.15rem 0.5rem;
  background: var(--accent-light, #e0e7ff);
  color: var(--accent, #2563eb);
  border-radius: 12px;
  font-weight: 500;
}
.note-link small,
.note-link time {
  color: var(--text-muted);
}
.note-link time {
  font-size: 0.75rem;
}
.delete-btn {
  flex-shrink: 0;
}
@media (max-width: 600px) {
  .note-card-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  .delete-btn {
    align-self: flex-end;
  }
}
</style>
