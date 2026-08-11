<template>
  <div class="upload">
    <input
      ref="picker"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/gif"
      class="sr-only"
      @change="choose"
    />
    <button class="button" type="button" :disabled="uploading" @click="picker?.click()">
      📷 {{ uploading ? 'Uploading…' : 'Upload Image' }}
    </button>
    <span v-if="error" class="error">{{ error }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { uploadImage } from '../note.api'
import type { NoteImage } from '../note.types'

const props = defineProps<{
  noteId?: string
  prepareNote?: () => Promise<string>
}>()

const emit = defineEmits<{
  uploaded: [res: { image: NoteImage; token: string }]
}>()

const picker = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const error = ref<string | null>(null)

async function choose(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  error.value = null
  if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)) {
    error.value = 'Use JPEG, PNG, WebP, or GIF.'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    error.value = 'Images must be 10 MiB or smaller.'
    return
  }
  uploading.value = true
  try {
    const noteId = props.noteId ?? (await props.prepareNote?.())
    if (!noteId) throw new Error('Save the note before uploading an image.')
    const result = await uploadImage(noteId, file)
    emit('uploaded', result)
  } catch (cause: unknown) {
    error.value = cause instanceof Error ? cause.message : 'Image upload failed. Please retry.'
  } finally {
    uploading.value = false
    ;(event.target as HTMLInputElement).value = ''
  }
}
</script>

<style scoped>
.upload {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.error {
  font-size: 0.8rem;
  color: var(--danger);
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  clip: rect(0, 0, 0, 0);
  overflow: hidden;
}
</style>
