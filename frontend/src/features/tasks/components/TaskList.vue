<template>
  <ul class="task-list" aria-label="Tasks">
    <li v-for="task in tasks" :key="task.id" class="task card">
      <button
        class="complete"
        type="button"
        :aria-label="
          task.status === 'completed' ? `Reopen ${task.title}` : `Complete ${task.title}`
        "
        :class="{ done: task.status === 'completed' }"
        @click="$emit('toggle', task)"
      >
        ✓</button
      ><RouterLink :to="`/tasks/${task.id}`" class="task-main"
        ><span :class="{ completed: task.status === 'completed' }">{{ task.title }}</span
        ><small v-if="task.description">{{ task.description }}</small
        ><span class="meta"
          ><span v-if="task.due_date">{{ dueLabel(task.due_date) }}</span
          ><span v-if="task.priority" :class="`priority p${task.priority}`">{{
            priority(task.priority)
          }}</span
          ><span v-for="tag in task.tags" :key="tag.id" class="tag">{{ tag.name }}</span></span
        ></RouterLink
      >
    </li>
  </ul>
</template>
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { Task } from '../task.types'
defineProps<{ tasks: Task[] }>()
defineEmits<{ toggle: [task: Task] }>()
const priority = (p: number) => ['', 'Low', 'Medium', 'High'][p]
function dueLabel(date: string) {
  const today = new Date().toLocaleDateString('en-CA')
  return date < today ? 'Overdue · ' + date : date === today ? 'Due today' : 'Due ' + date
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
}
.complete.done {
  background: var(--accent);
  border-color: var(--accent);
  color: white;
}
.task-main {
  display: grid;
  min-width: 0;
  gap: 0.25rem;
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
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  font-size: 0.75rem;
  color: var(--text-muted);
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
</style>
