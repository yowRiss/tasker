<template>
  <ul class="task-list" aria-label="Tasks">
    <li v-for="task in tasks" :key="task.id" class="task card">
      <button
        class="complete"
        type="button"
        :title="
          task.status === 'completed' ? 'Restore task to Open' : 'Mark done & send to Archive'
        "
        :aria-label="
          task.status === 'completed'
            ? `Reopen ${task.title}`
            : `Complete and send ${task.title} to archive`
        "
        :class="{ done: task.status === 'completed' }"
        @click="$emit('toggle', task)"
      >
        ✓
      </button>
      <RouterLink :to="`/tasks/${task.id}`" class="task-main">
        <span :class="{ completed: task.status === 'completed' }">{{ task.title }}</span>
        <small v-if="task.description">{{ task.description }}</small>

        <div v-if="task.subtasks && task.subtasks.length" class="card-subtasks" @click.stop.prevent>
          <div
            v-for="st in task.subtasks"
            :key="st.id"
            class="card-subtask-item"
            @click.stop="$emit('toggle-subtask', task, st)"
          >
            <input
              :checked="st.completed"
              type="checkbox"
              class="subtask-checkbox"
              @click.stop="$emit('toggle-subtask', task, st)"
            />
            <span :class="{ 'subtask-done': st.completed }">{{ st.title }}</span>
          </div>
        </div>

        <span class="meta">
          <span v-if="task.completed_at && task.status === 'completed'" class="archive-date">
            Archived {{ formatDate(task.completed_at) }}
          </span>
          <span v-if="task.subtasks && task.subtasks.length" class="subtask-badge">
            ✓ {{ completedCount(task.subtasks) }}/{{ task.subtasks.length }} subtasks
          </span>
          <span v-if="task.due_date && task.status !== 'completed'">{{
            dueLabel(task.due_date)
          }}</span>
          <span v-if="task.priority" :class="`priority p${task.priority}`">{{
            priority(task.priority)
          }}</span>
          <span v-for="tag in task.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
        </span>
      </RouterLink>
      <div v-if="isArchiveView" class="archive-actions">
        <button
          type="button"
          class="button subtle action-btn"
          title="Restore task to active list"
          @click="$emit('restore', task)"
        >
          Unarchive
        </button>
        <button
          type="button"
          class="button subtle danger action-btn"
          title="Delete task permanently"
          @click="$emit('delete', task)"
        >
          Delete
        </button>
      </div>
    </li>
  </ul>
</template>
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { Subtask, Task } from '../task.types'
defineProps<{ tasks: Task[]; isArchiveView?: boolean }>()
defineEmits<{
  toggle: [task: Task]
  'toggle-subtask': [task: Task, subtask: Subtask]
  restore: [task: Task]
  delete: [task: Task]
}>()
const priority = (p: number) => ['', 'Low', 'Medium', 'High'][p]
function dueLabel(date: string) {
  const today = new Date().toLocaleDateString('en-CA')
  return date < today ? 'Overdue · ' + date : date === today ? 'Due today' : 'Due ' + date
}
function completedCount(subtasks: Subtask[]) {
  return subtasks.filter((s) => s.completed).length
}
function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString(undefined, {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  } catch {
    return iso
  }
}
</script>
<style scoped>
.task-list {
  display: grid;
  gap: 0.55rem;
  padding: 0;
  margin: 0;
  list-style: none;
}
.task {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.8rem;
}
.complete {
  flex: 0 0 1.4rem;
  height: 1.4rem;
  margin-top: 0.1rem;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: white;
  color: transparent;
  transition: all 0.15s ease;
}
.complete:hover {
  border-color: var(--accent);
}
.complete.done {
  background: var(--accent);
  border-color: var(--accent);
  color: white;
}
.task-main {
  display: grid;
  flex: 1;
  min-width: 0;
  gap: 0.35rem;
  text-decoration: none;
}
.task-main > span:first-child {
  font-weight: 650;
}
.completed {
  text-decoration: line-through;
  color: var(--text-muted);
}
small {
  overflow: hidden;
  color: var(--text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-subtasks {
  display: grid;
  gap: 0.25rem;
  margin: 0.2rem 0;
  padding: 0.35rem 0.5rem;
  background: var(--surface-muted);
  border-radius: 6px;
}
.card-subtask-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8125rem;
  color: var(--text);
  cursor: pointer;
}
.subtask-checkbox {
  width: 0.95rem;
  height: 0.95rem;
  cursor: pointer;
}
.subtask-done {
  text-decoration: line-through;
  color: var(--text-muted);
}
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  font-size: 0.75rem;
  color: var(--text-muted);
}
.archive-date {
  color: #786134;
  background: #fbf5e8;
  padding: 0.05rem 0.4rem;
  border-radius: 4px;
  font-weight: 500;
}
.subtask-badge {
  background: #e8f3ee;
  color: var(--accent-strong);
  padding: 0.05rem 0.4rem;
  border-radius: 4px;
  font-weight: 600;
}
.priority {
  font-weight: 650;
}
.p3 {
  color: var(--danger);
}
.p2 {
  color: #9a6700;
}
.p1 {
  color: #316d9a;
}
.archive-actions {
  display: flex;
  gap: 0.35rem;
  align-self: center;
}
.action-btn {
  font-size: 0.8rem;
  padding: 0.25rem 0.5rem;
  min-height: auto;
}
@media (max-width: 550px) {
  .task {
    flex-wrap: wrap;
  }
  .archive-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 0.25rem;
    padding-top: 0.35rem;
    border-top: 1px dashed var(--border);
  }
}
</style>
