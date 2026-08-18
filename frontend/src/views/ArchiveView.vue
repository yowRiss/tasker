<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">Archive</h1>
        <p class="muted">Completed tasks sent to archive.</p>
      </div>
      <div class="header-actions">
        <RouterLink to="/tasks" class="button">← Back to Tasks</RouterLink>
        <button
          v-if="archivedTasks.length"
          type="button"
          class="button danger"
          @click="confirmClearArchive"
        >
          Clear Archive
        </button>
      </div>
    </header>

    <div v-if="toastMessage" class="toast-banner">
      <span>{{ toastMessage }}</span>
      <button type="button" class="toast-close" @click="toastMessage = ''">&times;</button>
    </div>

    <section class="filters card">
      <input
        v-model="filters.query"
        type="search"
        placeholder="Search archived tasks"
        aria-label="Search archived tasks"
      />
      <select v-model="filters.projectId">
        <option value="">All projects</option>
        <option v-for="project in projects" :key="project.id" :value="project.id">
          {{ project.name }}
        </option>
      </select>
      <select v-model="filters.tagId">
        <option value="">All tags</option>
        <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
      </select>
      <select v-model="filters.priority">
        <option value="">All priorities</option>
        <option value="1">Low</option>
        <option value="2">Medium</option>
        <option value="3">High</option>
      </select>
    </section>

    <p v-if="error" class="notice">
      {{ error }} <button type="button" @click="reload">Retry</button>
    </p>
    <p v-else-if="loading" class="empty">Loading archived tasks…</p>
    <template v-else-if="archivedTasks.length">
      <div class="archive-summary">
        <span class="count-badge"
          >{{ archivedTasks.length }} {{ archivedTasks.length === 1 ? 'task' : 'tasks' }} in
          archive</span
        >
      </div>
      <TaskList
        :tasks="archivedTasks"
        :is-archive-view="true"
        @toggle="handleRestore"
        @toggle-subtask="handleToggleSubtask"
        @restore="handleRestore"
        @delete="handleDelete"
      />
    </template>
    <div v-else class="card empty empty-archive">
      <div class="empty-icon">📁</div>
      <h3>Archive is empty</h3>
      <p class="muted">
        When you complete tasks in your task list, they will be sent to archive and listed here.
      </p>
      <RouterLink to="/tasks" class="button primary margin-top">Go to Tasks</RouterLink>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import TaskList from '../features/tasks/components/TaskList.vue'
import { useTasks } from '../features/tasks/composables/useTasks'
import type { TaskFilters } from '../features/tasks/composables/useTasks'
import type { Subtask, Task } from '../features/tasks/task.types'

const store = useTasks()
const { projects, tags, loading, error, toggleSubtask } = store
const toastMessage = ref('')
let toastTimer: number | undefined

const filters = reactive<TaskFilters>({
  status: 'archived',
  projectId: '',
  tagId: '',
  priority: '',
  due: 'all',
  query: '',
})

let timer: number | undefined
const archivedTasks = computed(() => store.visibleTasks(filters).value)

async function reload() {
  await store.load({
    status: 'completed',
    project_id: filters.projectId,
    q: filters.query,
  })
}

watch(
  () => [filters.projectId, filters.query],
  () => {
    window.clearTimeout(timer)
    timer = window.setTimeout(() => void reload(), 250)
  },
)

onMounted(() => void reload())

function showToast(msg: string) {
  toastMessage.value = msg
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 4000)
}

function handleToggleSubtask(task: Task, subtask: Subtask) {
  void toggleSubtask(task, subtask)
}

async function handleRestore(task: Task) {
  try {
    await store.toggle(task)
    showToast(`Task "${task.title}" unarchived and restored to Open tasks.`)
    await reload()
  } catch {
    // handled in store
  }
}

async function handleDelete(task: Task) {
  if (confirm(`Permanently delete "${task.title}" from archive?`)) {
    try {
      await store.removeTask(task.id)
      showToast(`Task "${task.title}" deleted.`)
      await reload()
    } catch {
      // handled in store
    }
  }
}

async function confirmClearArchive() {
  if (
    confirm(
      'Are you sure you want to permanently delete all archived tasks? This action cannot be undone.',
    )
  ) {
    try {
      await store.clearArchive()
      showToast('All archived tasks have been permanently deleted.')
      await reload()
    } catch {
      // handled in store
    }
  }
}
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}
.toast-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  border-radius: var(--radius);
  background: var(--accent-soft);
  border: 1px solid var(--accent);
  color: var(--accent-strong);
  font-weight: 550;
}
.toast-close {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  line-height: 1;
  color: var(--accent-strong);
  cursor: pointer;
  padding: 0 0.25rem;
}
.filters {
  display: grid;
  grid-template-columns: 2fr repeat(3, minmax(0, 1fr));
  gap: 0.5rem;
  padding: 0.65rem;
  margin: 1rem 0;
}
.filters input,
.filters select {
  min-width: 0;
  padding: 0.5rem;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: white;
}
.archive-summary {
  display: flex;
  align-items: center;
  margin-bottom: 0.75rem;
}
.count-badge {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-muted);
}
.empty-archive {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.5rem;
}
.empty-icon {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}
.empty-archive h3 {
  margin: 0.25rem 0;
  font-size: 1.2rem;
}
.margin-top {
  margin-top: 1rem;
}
@media (max-width: 800px) {
  .filters {
    grid-template-columns: repeat(2, 1fr);
  }
  .filters input {
    grid-column: span 2;
  }
}
@media (max-width: 520px) {
  .filters {
    grid-template-columns: 1fr;
  }
  .filters input {
    grid-column: span 1;
  }
}
</style>
