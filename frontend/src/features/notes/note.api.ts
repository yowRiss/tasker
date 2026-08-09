import { api, query } from '../../lib/api/client'
import type { Note, NoteImage, NoteInput, Page } from './note.types'
export const listNotes = (params: Record<string, string | number | undefined> = {}) =>
  api<Page<Note>>(`/v1/notes${query(params)}`)
export const getNote = (id: string) => api<Note>(`/v1/notes/${id}`)
export const createNote = (input: NoteInput) =>
  api<Note>('/v1/notes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
export const updateNote = (id: string, input: Partial<NoteInput>) =>
  api<Note>(`/v1/notes/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
export const deleteNote = (id: string) => api<void>(`/v1/notes/${id}`, { method: 'DELETE' })
export const uploadImage = (noteId: string, file: File) => {
  const form = new FormData()
  form.set('file', file)
  return api<{ image: NoteImage; token: string }>(`/v1/notes/${noteId}/images`, {
    method: 'POST',
    body: form,
  })
}
export const imageAccess = (id: string) =>
  api<{ url: string; expires_in: number }>(`/v1/note-images/${id}/access`)
export const updateImageAlt = (id: string, alt_text: string | null) =>
  api<NoteImage>(`/v1/note-images/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ alt_text }),
  })
export const deleteImage = (id: string) => api<void>(`/v1/note-images/${id}`, { method: 'DELETE' })
