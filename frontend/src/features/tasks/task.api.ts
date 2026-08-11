import { api, query } from '../../lib/api/client'
import type { Page, Project, Subtask, Tag, Task, TaskInput } from './task.types'
export const listTasks = (params: Record<string, string | number | undefined> = {}) =>
  api<Page<Task>>(`/v1/tasks${query(params)}`)
export const getTask = (id: string) => api<Task>(`/v1/tasks/${id}`)
export const createTask = (input: TaskInput) =>
  api<Task>('/v1/tasks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
export const updateTask = (id: string, input: Partial<TaskInput>) =>
  api<Task>(`/v1/tasks/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
export const setCompletion = (id: string, completed: boolean) =>
  api<Task>(`/v1/tasks/${id}/completion`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ completed }),
  })
export const deleteTask = (id: string) => api<void>(`/v1/tasks/${id}`, { method: 'DELETE' })
export const createSubtask = (
  taskId: string,
  title: string,
  completed = false,
  position = 0,
) =>
  api<Subtask>(`/v1/tasks/${taskId}/subtasks`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, completed, position }),
  })
export const updateSubtask = (
  taskId: string,
  subtaskId: string,
  input: { title?: string; completed?: boolean; position?: number },
) =>
  api<Subtask>(`/v1/tasks/${taskId}/subtasks/${subtaskId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
export const deleteSubtask = (taskId: string, subtaskId: string) =>
  api<void>(`/v1/tasks/${taskId}/subtasks/${subtaskId}`, { method: 'DELETE' })
export const listProjects = () => api<Page<Project>>('/v1/projects')
export const createProject = (name: string, color?: string) =>
  api<Project>('/v1/projects', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, color: color || null }),
  })
export const listTags = () => api<Page<Tag>>('/v1/tags')
export const createTag = (name: string) =>
  api<Tag>('/v1/tags', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
