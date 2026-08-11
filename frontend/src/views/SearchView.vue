<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">Search</h1>
        <p class="muted">Find tasks and notes.</p>
      </div>
    </header>
    <div class="searchbar">
      <input ref="input" v-model="q" type="search" placeholder="Search your workspace" /><select
        v-model="scope"
      >
        <option value="all">Everything</option>
        <option value="tasks">Tasks</option>
        <option value="notes">Notes</option>
      </select>
    </div>
    <p v-if="error" class="notice">{{ error }}</p>
    <p v-else-if="loading" class="empty">Searching…</p>
    <template v-else-if="q"
      ><section v-if="results.tasks?.length">
        <h2>Tasks</h2>
        <RouterLink
          v-for="task in results.tasks"
          :key="task.id"
          class="result card"
          :to="`/tasks/${task.id}`"
          >{{ task.title }}</RouterLink
        >
      </section>
      <section v-if="results.notes?.length">
        <h2>Notes</h2>
        <RouterLink
          v-for="note in results.notes"
          :key="note.id"
          class="result card"
          :to="`/notes/${note.id}`"
          ><strong>{{ note.title }}</strong
          ><small>{{ excerpt(note.content_md) }}</small></RouterLink
        >
      </section>
      <p v-if="!results.tasks?.length && !results.notes?.length" class="empty">
        No results.
      </p></template
    >
    <p v-else class="empty">Start typing to search.</p>
  </section>
</template>
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { search } from '../features/search/search.api'
import type { Note } from '../features/notes/note.types'
import type { Task } from '../features/tasks/task.types'
const input = ref<HTMLInputElement | null>(null),
  q = ref(''),
  scope = ref<'all' | 'tasks' | 'notes'>('all'),
  loading = ref(false),
  error = ref<string | null>(null),
  results = ref<{ tasks?: Task[]; notes?: Note[] }>({})
let timer: number | undefined
onMounted(() => input.value?.focus())
watch([q, scope], () => {
  clearTimeout(timer)
  if (!q.value.trim()) {
    results.value = {}
    return
  }
  timer = window.setTimeout(async () => {
    loading.value = true
    try {
      results.value = await search(q.value, scope.value)
      error.value = null
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Search failed.'
    } finally {
      loading.value = false
    }
  }, 250)
})
function excerpt(value: string) {
  return value
    .replace(/[#*_`>-]/g, ' ')
    .replace(/\s+/g, ' ')
    .slice(0, 130)
}
</script>
<style scoped>
.searchbar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
}
.searchbar input,
.searchbar select {
  padding: 0.7rem;
  border: 1px solid var(--border);
  border-radius: 8px;
}
.searchbar input {
  flex: 1;
}
@media (max-width: 500px) {
  .searchbar {
    flex-direction: column;
  }
}
section section {
  margin-top: 1.5rem;
}
.result {
  display: grid;
  gap: 0.3rem;
  padding: 0.85rem;
  margin: 0.5rem 0;
  text-decoration: none;
}
.result small {
  color: var(--text-muted);
}
</style>
