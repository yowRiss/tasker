<template>
  <form class="editor card" @submit.prevent="save">
    <div class="field">
      <label :for="`title-${id}`">Task title</label
      ><input :id="`title-${id}`" ref="titleInput" v-model="form.title" maxlength="280" required />
    </div>
    <div class="editor-grid">
      <div class="field">
        <label :for="`due-${id}`">Due date</label
        ><input :id="`due-${id}`" v-model="form.due_date" type="date" />
      </div>
      <div class="field">
        <label :for="`priority-${id}`">Priority</label
        ><select :id="`priority-${id}`" v-model.number="form.priority">
          <option :value="0">None</option>
          <option :value="1">Low</option>
          <option :value="2">Medium</option>
          <option :value="3">High</option>
        </select>
      </div>
      <div class="field">
        <div class="field-label-row">
          <label :for="`project-${id}`">Project</label>
          <button type="button" class="button-text" @click="toggleProjectForm">
            {{ creatingProjectState ? 'Cancel' : '+ New project' }}
          </button>
        </div>
        <div v-if="creatingProjectState" class="inline-project-form">
          <div class="inline-project-inputs">
            <input
              ref="newProjectInput"
              v-model="newProjectName"
              placeholder="Project name"
              maxlength="80"
              @keyup.enter.prevent="handleCreateProject"
            />
            <button
              type="button"
              class="button primary small-btn"
              :disabled="savingProject || !newProjectName.trim()"
              @click="handleCreateProject"
            >
              {{ savingProject ? 'Adding…' : 'Add' }}
            </button>
          </div>
          <p v-if="projectError" class="notice project-notice">{{ projectError }}</p>
        </div>
        <select
          v-else
          :id="`project-${id}`"
          v-model="form.project_id"
          @change="onProjectSelectChange"
        >
          <option value="">No project</option>
          <option v-for="project in projects" :key="project.id" :value="project.id">
            {{ project.name }}
          </option>
          <option value="__new__">+ Create new project…</option>
        </select>
      </div>
    </div>
    <div class="field">
      <span class="label">Tags</span>
      <div class="tags">
        <label v-for="tag in tags" :key="tag.id" class="tag-choice"
          ><input v-model="form.tag_ids" type="checkbox" :value="tag.id" /> {{ tag.name }}</label
        >
      </div>
    </div>
    <div class="field">
      <label :for="`description-${id}`">Description</label
      ><textarea :id="`description-${id}`" v-model="form.description" rows="3" />
    </div>

    <div v-if="task" class="field">
      <span class="label">Subtasks</span>
      <div v-if="form.subtasks && form.subtasks.length" class="subtask-list">
        <div v-for="(subtask, index) in form.subtasks" :key="subtask.id || index" class="subtask-item">
          <input
            v-model="subtask.completed"
            type="checkbox"
            class="subtask-checkbox"
            title="Toggle subtask completion"
          />
          <input
            v-model="subtask.title"
            type="text"
            placeholder="Subtask title"
            class="subtask-title-input"
            :class="{ 'subtask-done': subtask.completed }"
            @keyup.enter.prevent="addSubtask"
          />
          <button
            type="button"
            class="remove-subtask-btn"
            title="Remove subtask"
            @click="removeSubtask(index)"
          >
            &times;
          </button>
        </div>
      </div>
      <div class="add-subtask-row">
        <input
          v-model="newSubtaskTitle"
          placeholder="+ Add a subtask (press Enter)"
          class="subtask-add-input"
          @keyup.enter.prevent="addSubtask"
        />
        <button
          type="button"
          class="button subtle small-btn"
          :disabled="!newSubtaskTitle.trim()"
          @click="addSubtask"
        >
          Add
        </button>
      </div>
    </div>

    <p v-if="error" class="notice">{{ error }}</p>
    <div class="actions">
      <button class="button primary" :disabled="saving">
        {{ saving ? 'Saving…' : 'Save task' }}</button
      ><button v-if="task" class="button danger" type="button" @click="$emit('delete')">
        Delete</button
      ><button class="button subtle" type="button" @click="$emit('cancel')">Cancel</button>
    </div>
  </form>
</template>
<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { createProject } from '../task.api'
import type { Priority, Project, SubtaskInput, Tag, Task, TaskInput } from '../task.types'
const props = defineProps<{ task?: Task; projects: Project[]; tags: Tag[] }>()
const emit = defineEmits<{
  save: [input: TaskInput]
  cancel: []
  delete: []
  'project-created': [project: Project]
}>()
const id = crypto.randomUUID()
const titleInput = ref<HTMLInputElement | null>(null)
const saving = ref(false),
  error = ref<string | null>(null)
const creatingProjectState = ref(false)
const savingProject = ref(false)
const newProjectName = ref('')
const newSubtaskTitle = ref('')
const projectError = ref<string | null>(null)
const newProjectInput = ref<HTMLInputElement | null>(null)

onMounted(() => {
  void nextTick(() => titleInput.value?.focus())
})

function toggleProjectForm() {
  creatingProjectState.value = !creatingProjectState.value
  projectError.value = null
  newProjectName.value = ''
  if (creatingProjectState.value) {
    void nextTick(() => newProjectInput.value?.focus())
  }
}

function onProjectSelectChange() {
  if (form.project_id === '__new__') {
    form.project_id = ''
    creatingProjectState.value = true
    projectError.value = null
    newProjectName.value = ''
    void nextTick(() => newProjectInput.value?.focus())
  }
}

async function handleCreateProject() {
  const name = newProjectName.value.trim()
  if (!name) return
  savingProject.value = true
  projectError.value = null
  try {
    const newProj = await createProject(name)
    emit('project-created', newProj)
    form.project_id = newProj.id
    creatingProjectState.value = false
    newProjectName.value = ''
  } catch (cause: unknown) {
    projectError.value = cause instanceof Error ? cause.message : 'Failed to create project.'
  } finally {
    savingProject.value = false
  }
}

const initialSubtasks: SubtaskInput[] = props.task?.subtasks?.map((st) => ({
  id: st.id,
  title: st.title,
  completed: st.completed,
  position: st.position,
})) ?? []

const form = reactive<TaskInput>({
  title: props.task?.title ?? '',
  description: props.task?.description ?? '',
  due_date: props.task?.due_date ?? '',
  priority: props.task?.priority ?? 0,
  project_id: props.task?.project_id ?? '',
  tag_ids: props.task?.tags.map((t) => t.id) ?? [],
  subtasks: initialSubtasks,
})

function addSubtask() {
  const title = newSubtaskTitle.value.trim()
  if (!title) return
  if (!form.subtasks) form.subtasks = []
  form.subtasks.push({
    title,
    completed: false,
    position: form.subtasks.length,
  })
  newSubtaskTitle.value = ''
}

function removeSubtask(index: number) {
  if (form.subtasks) {
    form.subtasks.splice(index, 1)
  }
}

async function save() {
  const title = form.title.trim()
  if (!title) {
    error.value = 'A task title is required.'
    return
  }
  saving.value = true
  error.value = null
  try {
    const validSubtasks = (form.subtasks || [])
      .map((st) => ({
        id: st.id || undefined,
        title: st.title.trim(),
        completed: Boolean(st.completed),
        position: st.position ?? 0,
      }))
      .filter((st) => st.title !== '')
    const desc = form.description?.trim() || null
    const due = form.due_date?.trim() || null
    const proj = form.project_id?.trim() || null
    emit('save', {
      title,
      description: desc,
      due_date: due,
      priority: Number(form.priority) as Priority,
      project_id: proj,
      tag_ids: Array.isArray(form.tag_ids) ? form.tag_ids : [],
      subtasks: validSubtasks,
    })
  } finally {
    saving.value = false
  }
}
</script>
<style scoped>
.editor {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}
.editor-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}
.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.button-text {
  background: none;
  border: none;
  color: var(--accent);
  font-size: var(--font-small);
  font-weight: 600;
  padding: 0;
}
.button-text:hover {
  text-decoration: underline;
}
.inline-project-form {
  display: grid;
  gap: 0.35rem;
}
.inline-project-inputs {
  display: flex;
  gap: 0.35rem;
}
.small-btn {
  min-height: auto;
  padding: 0.4rem 0.65rem;
  font-size: 0.85rem;
  white-space: nowrap;
}
.project-notice {
  margin: 0;
  padding: 0.4rem 0.6rem;
  font-size: 0.8rem;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}
.tag-choice {
  padding: 0.3rem 0.55rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 0.8rem;
}
.subtask-list {
  display: grid;
  gap: 0.4rem;
  margin-bottom: 0.5rem;
}
.subtask-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.subtask-checkbox {
  width: 1.1rem;
  height: 1.1rem;
  cursor: pointer;
}
.subtask-title-input {
  flex: 1;
  padding: 0.35rem 0.55rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 0.875rem;
}
.subtask-done {
  text-decoration: line-through;
  color: var(--text-muted);
}
.remove-subtask-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0 0.4rem;
  line-height: 1;
}
.remove-subtask-btn:hover {
  color: var(--danger);
}
.add-subtask-row {
  display: flex;
  gap: 0.4rem;
}
.subtask-add-input {
  flex: 1;
  padding: 0.4rem 0.6rem;
  border: 1px dashed var(--border);
  border-radius: 6px;
  font-size: 0.85rem;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.label {
  font-weight: 650;
  font-size: var(--font-small);
}
@media (max-width: 620px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
