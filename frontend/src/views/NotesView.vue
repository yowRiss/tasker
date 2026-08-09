<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">Notes</h1>
        <p class="muted">Ideas, references, and useful context.</p>
      </div>
      <button class="button primary" @click="creating = !creating">
        {{ creating ? 'Close' : 'New note' }}
      </button>
    </header>
    <NoteEditor v-if="creating" @save="create" @cancel="creating = false" />
    <div class="notes-toolbar">
      <input v-model="q" type="search" placeholder="Search notes" aria-label="Search notes" />
    </div>
    <p v-if="error" class="notice">
      {{ error }} <button type="button" @click="load">Retry</button>
    </p>
    <p v-else-if="loading" class="empty">Loading notes…</p>
    <ul v-else-if="notes.length" class="note-list">
      <li v-for="note in notes" :key="note.id" class="card">
        <RouterLink :to="`/notes/${note.id}`"
          ><strong>{{ note.title }}</strong
          ><small>{{ excerpt(note.content_md) }}</small
          ><time>Updated {{ format(note.updated_at) }}</time></RouterLink
        >
      </li>
    </ul>
    <p v-else class="card empty">No notes yet. Capture something worth keeping.</p>
  </section>
</template>
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import NoteEditor from '../features/notes/components/NoteEditor.vue'
import { createNote, listNotes } from '../features/notes/note.api'
import type { Note, NoteInput } from '../features/notes/note.types'
const router = useRouter(),
  notes = ref<Note[]>([]),
  q = ref(''),
  loading = ref(false),
  error = ref<string | null>(null),
  creating = ref(false)
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
onMounted(() => void load())
watch(q, () => {
  clearTimeout(timer)
  timer = window.setTimeout(() => void load(), 250)
})
async function create(input: NoteInput) {
  try {
    const note = await createNote(input)
    creating.value = false
    await router.push(`/notes/${note.id}`)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unable to create note.'
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
.note-list a {
  display: grid;
  gap: 0.4rem;
  padding: 1rem;
  text-decoration: none;
}
.note-list small,
.note-list time {
  color: var(--text-muted);
}
.note-list time {
  font-size: 0.75rem;
}
</style>
