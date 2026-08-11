<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">Tasks</h1>
        <p class="muted">Plan what matters next.</p>
      </div>
      <div class="header-actions">
        <RouterLink to="/archive" class="button">View Archive</RouterLink>
        <button class="button primary" @click="creating = !creating">
          {{ creating ? 'Close' : 'New task' }}
        </button>
      </div>
    </header>

    <div v-if="toastMessage" class="toast-banner">
      <span>{{ toastMessage }}</span>
      <div class="toast-actions">
        <button v-if="lastCompletedTask" type="button" class="button subtle toast-btn" @click="undoArchive">
          Undo
        </button>
        <RouterLink to="/archive" class="button toast-btn toast-link">View Archive</RouterLink>
        <button type="button" class="toast-close" @click="toastMessage = ''">&times;</button>
      </div>
    </div>

    <TaskEditor
      v-if="creating"
      :projects="projects"
      :tags="tags"
      @save="create"
      @project-created="onProjectCreated"
      @cancel="creating = false"
    />
    <section class="filters card">
      <input
        v-model="filters.query"
        type="search"
        placeholder="Search tasks"
        aria-label="Search tasks"
      /><select v-model="filters.status">
        <option value="open">Open</option>
        <option value="archived">Archived</option>
        <option value="completed">Completed</option>
        <option value="all">All tasks</option></select
      ><select v-model="filters.projectId">
        <option value="">All projects</option>
        <option v-for="project in projects" :key="project.id" :value="project.id">
          {{ project.name }}
        </option></select
      ><select v-model="filters.tagId">
        <option value="">All tags</option>
        <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option></select
      ><select v-model="filters.priority">
        <option value="">All priorities</option>
        <option value="1">Low</option>
        <option value="2">Medium</option>
        <option value="3">High</option></select
      ><select v-model="filters.due">
        <option value="all">Any due date</option>
        <option value="overdue">Overdue</option>
        <option value="today">Today</option>
        <option value="upcoming">Upcoming</option>
        <option value="none">No due date</option>
      </select>
    </section>
    <p v-if="error" class="notice">
      {{ error }} <button type="button" @click="reload">Retry</button>
    </p>
    <p v-else-if="loading" class="empty">Loading tasks…</p>
    <TaskList
      v-else-if="visible.length"
      :tasks="visible"
      @toggle="handleToggle"
      @toggle-subtask="handleToggleSubtask"
    />
    <p v-else class="card empty">No tasks match these filters.</p>
  </section>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import TaskEditor from '../features/tasks/components/TaskEditor.vue'
import TaskList from '../features/tasks/components/TaskList.vue'
import { createTask } from '../features/tasks/task.api'
import { useTasks } from '../features/tasks/composables/useTasks'
import type { TaskFilters } from '../features/tasks/composables/useTasks'
import type { Project, Subtask, Task, TaskInput } from '../features/tasks/task.types'

const store = useTasks()
const creating = ref(false)
const toastMessage = ref('')
const lastCompletedTask = ref<Task | null>(null)
let toastTimer: number | undefined

const { projects, tags, loading, error, toggleSubtask } = store
const filters = reactive<TaskFilters>({
  status: 'open',
  projectId: '',
  tagId: '',
  priority: '',
  due: 'all',
  query: '',
})
let timer: number | undefined
const visible = store.visibleTasks(filters)

async function reload() {
  await store.load({
    status: filters.status === 'all' ? '' : filters.status === 'archived' ? 'completed' : filters.status,
    project_id: filters.projectId,
    q: filters.query,
  })
}

watch(
  () => [filters.status, filters.projectId, filters.query],
  () => {
    window.clearTimeout(timer)
    timer = window.setTimeout(() => void reload(), 250)
  },
)

onMounted(() => void reload())

async function handleToggle(task: Task) {
  const isCompleting = task.status === 'open'
  const taskTitle = task.title
  try {
    await store.toggle(task)
    if (isCompleting) {
      lastCompletedTask.value = task
      toastMessage.value = `Task "${taskTitle}" marked done and sent to Archive.`
      window.clearTimeout(toastTimer)
      toastTimer = window.setTimeout(() => {
        toastMessage.value = ''
      }, 5000)
    } else {
      toastMessage.value = `Task "${taskTitle}" reopened.`
      window.clearTimeout(toastTimer)
      toastTimer = window.setTimeout(() => {
        toastMessage.value = ''
      }, 3000)
    }
  } catch {
    // handled in store
  }
}

function handleToggleSubtask(task: Task, subtask: Subtask) {
  void toggleSubtask(task, subtask)
}

async function undoArchive() {
  if (lastCompletedTask.value) {
    await store.toggle(lastCompletedTask.value)
    toastMessage.value = `Restored "${lastCompletedTask.value.title}" to Open tasks.`
    lastCompletedTask.value = null
    window.clearTimeout(toastTimer)
    toastTimer = window.setTimeout(() => {
      toastMessage.value = ''
    }, 3000)
  }
}

async function create(input: TaskInput) {
  try {
    await createTask(input)
    creating.value = false
    await reload()
  } catch (cause: unknown) {
    error.value = cause instanceof Error ? cause.message : 'Unable to create task.'
  }
}

function onProjectCreated(project: Project) {
  if (!projects.value.some((p) => p.id === project.id)) {
    projects.value.push(project)
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
  animation: fadeIn 0.2s ease-in-out;
}
.toast-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.toast-btn {
  font-size: 0.8125rem;
  padding: 0.25rem 0.6rem;
  min-height: auto;
}
.toast-link {
  background: var(--accent);
  color: white;
  text-decoration: none;
  border-radius: 6px;
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
  grid-template-columns: 2fr repeat(5, minmax(0, 1fr));
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
@media (max-width: 800px) {
  .filters {
    grid-template-columns: repeat(2, 1fr);
  }
  .filters input {
    grid-column: span 2;
  }
  .toast-banner {
    flex-direction: column;
    align-items: flex-start;
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
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
