import { api, query } from '../../lib/api/client'
import type { Note } from '../notes/note.types'
import type { Task } from '../tasks/task.types'
export const search = (q: string, scope: 'all' | 'tasks' | 'notes') =>
  api<{ tasks?: Task[]; notes?: Note[] }>(`/v1/search${query({ q, scope, limit: 30 })}`)
