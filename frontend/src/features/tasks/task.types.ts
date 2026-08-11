export type Priority = 0 | 1 | 2 | 3
export interface Tag {
  id: string
  name: string
  color: string | null
}
export interface Project {
  id: string
  name: string
  color: string | null
  is_archived: boolean
}
export interface Subtask {
  id: string
  task_id: string
  title: string
  completed: boolean
  position: number
  created_at: string
  updated_at: string
}
export interface SubtaskInput {
  id?: string
  title: string
  completed: boolean
  position?: number
}
export interface Task {
  id: string
  title: string
  description: string | null
  due_date: string | null
  project_id: string | null
  status: 'open' | 'completed'
  completed_at: string | null
  priority: Priority
  tags: Tag[]
  subtasks: Subtask[]
  created_at: string
  updated_at: string
}
export interface TaskInput {
  title: string
  description: string | null
  due_date: string | null
  priority: Priority
  project_id: string | null
  tag_ids: string[]
  subtasks?: SubtaskInput[]
}
export interface Page<T> {
  items: T[]
  next_cursor?: string
}
