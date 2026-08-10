<template>
  <div class="receipt-uploader">
    <div v-if="receipt" class="receipt-preview">
      <div class="receipt-info">
        <span class="file-icon">📎</span>
        <span class="file-name">{{ receipt.original_filename }}</span>
        <small class="file-size">({{ (receipt.byte_size / 1024).toFixed(1) }} KB)</small>
      </div>

      <div class="receipt-actions">
        <button v-if="signedUrl" type="button" class="button subtle" @click="showModal = true">
          View Receipt
        </button>
        <button
          type="button"
          class="button subtle danger"
          :disabled="deleting"
          @click="handleDelete"
        >
          {{ deleting ? 'Deleting…' : 'Remove' }}
        </button>
      </div>
    </div>

    <div v-else class="upload-control">
      <input
        ref="picker"
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        class="sr-only"
        @change="handleChoose"
      />
      <button
        type="button"
        class="button subtle"
        :disabled="uploading || !transactionId"
        @click="picker?.click()"
      >
        {{ uploading ? 'Uploading receipt…' : 'Attach Receipt Image' }}
      </button>
      <span v-if="!transactionId" class="help-text">Save transaction first to attach a receipt.</span>
    </div>

    <p v-if="error" class="notice">{{ error }}</p>

    <!-- Modal view for receipt image -->
    <div v-if="showModal && signedUrl" class="modal-overlay" @click.self="showModal = false">
      <div class="modal-content card">
        <header class="modal-header">
          <h3>Receipt: {{ receipt?.original_filename }}</h3>
          <button type="button" class="button subtle" @click="showModal = false">✕</button>
        </header>
        <div class="modal-body">
          <img :src="signedUrl" :alt="receipt?.original_filename || 'Receipt Image'" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { deleteReceipt, receiptAccess, uploadReceipt } from '../money.api'
import type { Receipt } from '../money.types'

const props = defineProps<{
  transactionId?: string
  receipt?: Receipt | null
}>()

const emit = defineEmits<{
  uploaded: [receipt: Receipt]
  deleted: []
}>()

const picker = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const deleting = ref(false)
const error = ref<string | null>(null)
const signedUrl = ref<string | null>(null)
const showModal = ref(false)

async function fetchSignedUrl() {
  if (!props.receipt?.id) {
    signedUrl.value = null
    return
  }
  try {
    const res = await receiptAccess(props.receipt.id)
    signedUrl.value = res.url
  } catch (e: unknown) {
    console.error('Failed to get receipt access URL', e)
  }
}

onMounted(() => void fetchSignedUrl())
watch(() => props.receipt?.id, () => void fetchSignedUrl())

async function handleChoose(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  error.value = null

  if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)) {
    error.value = 'Allowed image types: JPEG, PNG, WebP, GIF.'
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    error.value = 'Receipt image must be 10 MiB or smaller.'
    return
  }
  if (!props.transactionId) {
    error.value = 'Save transaction before uploading a receipt.'
    return
  }

  uploading.value = true
  try {
    const uploaded = await uploadReceipt(props.transactionId, file)
    emit('uploaded', uploaded)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Receipt upload failed.'
  } finally {
    uploading.value = false
    if (event.target) {
      ;(event.target as HTMLInputElement).value = ''
    }
  }
}

async function handleDelete() {
  if (!props.receipt?.id) return
  deleting.value = true
  error.value = null
  try {
    await deleteReceipt(props.receipt.id)
    signedUrl.value = null
    emit('deleted')
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Failed to delete receipt.'
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.receipt-uploader {
  display: grid;
  gap: var(--space-2);
}
.receipt-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-2) var(--space-3);
  border: 1px dashed var(--border);
  border-radius: 7px;
  background: var(--surface-muted);
}
.receipt-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-small);
}
.file-name {
  font-weight: 600;
}
.file-size {
  color: var(--text-muted);
}
.receipt-actions {
  display: flex;
  gap: var(--space-2);
}
.upload-control {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.help-text {
  font-size: 0.8rem;
  color: var(--text-muted);
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  clip: rect(0, 0, 0, 0);
  overflow: hidden;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  padding: var(--space-4);
}
.modal-content {
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  background: white;
  padding: var(--space-4);
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
}
.modal-header h3 {
  margin: 0;
  font-size: 1.1rem;
}
.modal-body {
  overflow: auto;
  text-align: center;
}
.modal-body img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 6px;
}
</style>
