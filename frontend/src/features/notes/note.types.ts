import type { Tag } from '../tasks/task.types'
export interface LinkedTask {
  id: string
  title: string
}
export interface Note {
  id: string
  title: string
  content_md: string
  tags: Tag[]
  tasks: LinkedTask[]
  created_at: string
  updated_at: string
}
export interface NoteInput {
  title: string
  content_md: string
}
export interface NoteImage {
  id: string
  note_id: string
  original_filename: string
  mime_type: string
  byte_size: number
  alt_text: string | null
  width: number | null
  height: number | null
}
export interface Page<T> {
  items: T[]
}
