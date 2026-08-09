import { computed, ref } from 'vue'
import { createProject as createProjectApi, listProjects, listTags, listTasks, setCompletion } from '../task.api'
import type { Project, Tag, Task } from '../task.types'

export interface TaskFilters {
  status: 'open' | 'completed' | 'all'
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
          (filters.status === 'all' || task.status === filters.status) &&
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
      const [taskPage, projectPage, tagPage] = await Promise.all([
        listTasks({ ...params, limit: 100 }),
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
    } catch (cause: unknown) {
      task.status = previous
      task.completed_at = null
      error.value = cause instanceof Error ? cause.message : 'Unable to update task.'
    }
  }
  return { tasks, projects, tags, loading, error, load, toggle, visibleTasks, addProject }
}
