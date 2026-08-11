import { computed, ref } from 'vue'
import {
  createProject as createProjectApi,
  deleteTask as deleteTaskApi,
  listProjects,
  listTags,
  listTasks,
  setCompletion,
  updateSubtask as updateSubtaskApi,
} from '../task.api'
import type { Project, Subtask, Tag, Task } from '../task.types'

export interface TaskFilters {
  status: 'open' | 'completed' | 'archived' | 'all'
  projectId: string
  tagId: string
  priority: string
  due: 'all' | 'overdue' | 'today' | 'upcoming' | 'none'
  query: string
}
const tasks = ref<Task[]>([])
const projects = ref<Project[]>([])
const tags = ref<Tag[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
function dateKind(due: string | null) {
  if (!due) return 'none'
  const today = new Date().toLocaleDateString('en-CA')
  return due < today ? 'overdue' : due === today ? 'today' : 'upcoming'
}
export function useTasks() {
  const visibleTasks = (filters: TaskFilters) =>
    computed(() =>
      tasks.value.filter(
        (task) =>
          (filters.status === 'all' ||
            (filters.status === 'archived'
              ? task.status === 'completed'
              : task.status === filters.status)) &&
          (!filters.projectId || task.project_id === filters.projectId) &&
          (!filters.tagId || task.tags.some((tag) => tag.id === filters.tagId)) &&
          (!filters.priority || task.priority === Number(filters.priority)) &&
          (filters.due === 'all' || dateKind(task.due_date) === filters.due),
      ),
    )
  async function load(params: { status?: string; project_id?: string; q?: string } = {}) {
    loading.value = true
    error.value = null
    try {
      const fetchParams = { ...params, limit: 100 }
      if (fetchParams.status === 'archived') {
        fetchParams.status = 'completed'
      }
      const [taskPage, projectPage, tagPage] = await Promise.all([
        listTasks(fetchParams),
        listProjects(),
        listTags(),
      ])
      tasks.value = taskPage.items
      projects.value = projectPage.items.filter((project) => !project.is_archived)
      tags.value = tagPage.items
    } catch (cause: unknown) {
      error.value = cause instanceof Error ? cause.message : 'Unable to load tasks.'
    } finally {
      loading.value = false
    }
  }
  async function addProject(name: string, color?: string) {
    const newProject = await createProjectApi(name, color)
    if (!projects.value.some((p) => p.id === newProject.id)) {
      projects.value.push(newProject)
    }
    return newProject
  }
  async function toggle(task: Task) {
    const previous = task.status
    task.status = previous === 'completed' ? 'open' : 'completed'
    task.completed_at = task.status === 'completed' ? new Date().toISOString() : null
    try {
      const saved = await setCompletion(task.id, task.status === 'completed')
      Object.assign(task, saved)
      return saved
    } catch (cause: unknown) {
      task.status = previous
      task.completed_at = null
      error.value = cause instanceof Error ? cause.message : 'Unable to update task.'
      throw cause
    }
  }
  async function removeTask(taskId: string) {
    try {
      await deleteTaskApi(taskId)
      tasks.value = tasks.value.filter((t) => t.id !== taskId)
    } catch (cause: unknown) {
      error.value = cause instanceof Error ? cause.message : 'Unable to delete task.'
      throw cause
    }
  }
  async function clearArchive() {
    const archivedTasks = tasks.value.filter((t) => t.status === 'completed')
    loading.value = true
    try {
      await Promise.all(archivedTasks.map((t) => deleteTaskApi(t.id)))
      tasks.value = tasks.value.filter((t) => t.status !== 'completed')
    } catch (cause: unknown) {
      error.value = cause instanceof Error ? cause.message : 'Unable to clear archive.'
      throw cause
    } finally {
      loading.value = false
    }
  }
  async function toggleSubtask(task: Task, subtask: Subtask) {
    const previous = subtask.completed
    subtask.completed = !previous
    try {
      const updated = await updateSubtaskApi(task.id, subtask.id, { completed: subtask.completed })
      Object.assign(subtask, updated)
    } catch (cause: unknown) {
      subtask.completed = previous
      error.value = cause instanceof Error ? cause.message : 'Unable to update subtask.'
    }
  }
  return {
    tasks,
    projects,
    tags,
    loading,
    error,
    load,
    toggle,
    toggleSubtask,
    removeTask,
    clearArchive,
    visibleTasks,
    addProject,
  }
}
