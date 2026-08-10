<template>
  <div class="card account-editor">
    <h2 class="title">{{ isEditing ? 'Edit Account' : 'New Account' }}</h2>
    <form @submit.prevent="handleSubmit">
      <div class="field">
        <label for="acc-name">Account Name</label>
        <input
          id="acc-name"
          v-model="form.name"
          type="text"
          placeholder="e.g. Primary Checking, Cash Wallet"
          required
          maxlength="80"
        />
      </div>

      <div class="field">
        <label for="acc-type">Account Type</label>
        <select id="acc-type" v-model="form.account_type" required>
          <option value="bank">Bank Account</option>
          <option value="cash">Cash</option>
          <option value="e_wallet">E-Wallet</option>
          <option value="credit_card">Credit Card</option>
        </select>
      </div>

      <p v-if="localError" class="notice">{{ localError }}</p>

      <div class="actions">
        <button type="submit" class="button primary" :disabled="saving">
          {{ saving ? 'Saving…' : isEditing ? 'Update Account' : 'Create Account' }}
        </button>
        <button type="button" class="button subtle" :disabled="saving" @click="$emit('cancel')">
          Cancel
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import type { Account, AccountInput } from '../money.types'

const props = defineProps<{
  account?: Account | null
}>()

const emit = defineEmits<{
  save: [input: AccountInput]
  cancel: []
}>()

const isEditing = computed(() => Boolean(props.account))

const form = reactive<AccountInput>({
  name: props.account?.name ?? '',
  account_type: props.account?.account_type ?? 'bank',
})

const saving = ref(false)
const localError = ref<string | null>(null)

async function handleSubmit() {
  if (!form.name.trim()) {
    localError.value = 'Account name is required.'
    return
  }
  saving.value = true
  localError.value = null
  try {
    emit('save', {
      name: form.name.trim(),
      account_type: form.account_type,
    })
  } catch (e: unknown) {
    localError.value = e instanceof Error ? e.message : 'Failed to save account.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.account-editor {
  padding: var(--space-5);
  margin-bottom: var(--space-5);
}
.title {
  margin-top: 0;
  margin-bottom: var(--space-4);
  font-size: 1.2rem;
}
form {
  display: grid;
  gap: var(--space-4);
}
.actions {
  display: flex;
  gap: var(--space-3);
}
</style>
