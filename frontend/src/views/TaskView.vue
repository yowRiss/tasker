<template>
  <section class="page">
    <RouterLink class="muted" to="/tasks">← Tasks</RouterLink>
    <p v-if="loading" class="empty">Loading task…</p>
    <p v-else-if="error" class="notice">{{ error }}</p>
    <template v-else-if="task">
      <header class="page-header">
        <div>
          <h1 class="page-title">Edit task</h1>
          <p v-if="task.status === 'completed'" class="archived-badge">Archived</p>
        </div>
        <button class="button" @click="toggle">
          {{ task.status === 'completed' ? 'Unarchive Task' : 'Send to Archive' }}
        </button>
      </header>
      <TaskEditor
        :task="task"
        :projects="projects"
        :tags="tags"
        @save="save"
        @delete="remove"
        @project-created="onProjectCreated"
        @cancel="router.push(task.status === 'completed' ? '/archive' : '/tasks')"
      />
    </template>
  </section>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import TaskEditor from '../features/tasks/components/TaskEditor.vue'
import {
  deleteTask,
  getTask,
  listProjects,
  listTags,
  setCompletion,
  updateTask,
} from '../features/tasks/task.api'
import type { Project, Tag, Task, TaskInput } from '../features/tasks/task.types'
const route = useRoute(),
  router = useRouter(),
  task = ref<Task | null>(null),
  projects = ref<Project[]>([]),
  tags = ref<Tag[]>([]),
  loading = ref(true),
  error = ref<string | null>(null)
onMounted(async () => {
  try {
    const [t, p, g] = await Promise.all([
      getTask(String(route.params.taskId)),
      listProjects(),
      listTags(),
    ])
    task.value = t
    projects.value = p.items
    tags.value = g.items
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unable to load task.'
  } finally {
    loading.value = false
  }
})
async function save(input: TaskInput) {
  if (!task.value) return
  try {
    task.value = await updateTask(task.value.id, input)
    router.push('/tasks')
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unable to save task.'
  }
}
async function toggle() {
  if (task.value) task.value = await setCompletion(task.value.id, task.value.status !== 'completed')
}
async function remove() {
  if (task.value && confirm('Permanently delete this task?')) {
    await deleteTask(task.value.id)
    await router.push('/tasks')
  }
}
function onProjectCreated(project: Project) {
  if (!projects.value.some((p) => p.id === project.id)) {
    projects.value.push(project)
  }
}
</script>
<style scoped>
.archived-badge {
  display: inline-block;
  margin: 0.25rem 0 0 0;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #fbf5e8;
  color: #786134;
  border: 1px solid #e3d2ab;
}
</style>
