<template>
  <section class="page">
    <RouterLink class="muted" to="/notes">← Notes</RouterLink>
    <p v-if="loading" class="empty">Loading note…</p>
    <p v-else-if="error" class="notice">{{ error }}</p>
    <template v-else-if="note"
      ><header class="page-header">
        <div>
          <h1 class="page-title">Edit note</h1>
          <p v-if="note.tasks?.length" class="muted">
            Linked: {{ note.tasks.map((task) => task.title).join(', ') }}
          </p>
        </div>
        <button class="button danger" @click="remove">Delete</button>
      </header>
      <NoteEditor
        :note-id="note.id"
        :initial="{ title: note.title, content_md: note.content_md, reminder_at: note.reminder_at, reminder_offsets: note.reminder_offsets }"
        @save="save"
        @cancel="router.push('/notes')"
    /></template>
  </section>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import NoteEditor from '../features/notes/components/NoteEditor.vue'
import { deleteNote, getNote, updateNote } from '../features/notes/note.api'
import type { Note, NoteInput } from '../features/notes/note.types'
const route = useRoute(),
  router = useRouter(),
  note = ref<Note | null>(null),
  loading = ref(true),
  error = ref<string | null>(null)
onMounted(async () => {
  try {
    note.value = await getNote(String(route.params.noteId))
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unable to load note.'
  } finally {
    loading.value = false
  }
})
async function save(input: NoteInput) {
  if (!note.value) return
  try {
    note.value = await updateNote(note.value.id, input)
    await router.push('/notes')
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unable to save note.'
  }
}
async function remove() {
  if (note.value && confirm('Permanently delete this note?')) {
    await deleteNote(note.value.id)
    await router.push('/notes')
  }
}
</script>
