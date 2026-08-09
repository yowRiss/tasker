<template>
  <div class="upload">
    <input
      ref="picker"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/gif"
      class="sr-only"
      @change="choose"
    /><button class="button" type="button" :disabled="uploading" @click="picker?.click()">
      {{ uploading ? 'Uploading…' : 'Insert image' }}</button
    ><span v-if="error" class="error">{{ error }}</span>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { uploadImage } from '../note.api'
const props = defineProps<{ noteId: string }>()
const emit = defineEmits<{ uploaded: [token: string] }>()
const picker = ref<HTMLInputElement | null>(null),
  uploading = ref(false),
  error = ref<string | null>(null)
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
    emit('uploaded', (await uploadImage(props.noteId, file)).token)
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
