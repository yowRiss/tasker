<template>
  <form class="editor card" @submit.prevent="save">
    <div class="field">
      <label :for="`title-${id}`">Task title</label
      ><input :id="`title-${id}`" v-model="form.title" maxlength="280" required autofocus />
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
      ><textarea :id="`description-${id}`" v-model="form.description" rows="4" />
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
import { nextTick, reactive, ref } from 'vue'
import { createProject } from '../task.api'
import type { Project, Tag, Task, TaskInput } from '../task.types'
const props = defineProps<{ task?: Task; projects: Project[]; tags: Tag[] }>()
const emit = defineEmits<{
  save: [input: TaskInput]
  cancel: []
  delete: []
  'project-created': [project: Project]
}>()
const id = crypto.randomUUID()
const saving = ref(false),
  error = ref<string | null>(null)
const creatingProjectState = ref(false)
const savingProject = ref(false)
const newProjectName = ref('')
const projectError = ref<string | null>(null)
const newProjectInput = ref<HTMLInputElement | null>(null)

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
    if (!props.projects.some((p) => p.id === newProj.id)) {
      props.projects.push(newProj)
    }
    form.project_id = newProj.id
    creatingProjectState.value = false
    newProjectName.value = ''
  } catch (cause: unknown) {
    projectError.value = cause instanceof Error ? cause.message : 'Failed to create project.'
  } finally {
    savingProject.value = false
  }
}

const form = reactive<TaskInput>({
  title: props.task?.title ?? '',
  description: props.task?.description ?? '',
  due_date: props.task?.due_date ?? '',
  priority: props.task?.priority ?? 0,
  project_id: props.task?.project_id ?? '',
  tag_ids: props.task?.tags.map((t) => t.id) ?? [],
})
async function save() {
  const title = form.title.trim()
  if (!title) {
    error.value = 'A task title is required.'
    return
  }
  saving.value = true
  error.value = null
  try {
    emit('save', {
      ...form,
      title,
      description: form.description || null,
      due_date: form.due_date || null,
      project_id: form.project_id || null,
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
