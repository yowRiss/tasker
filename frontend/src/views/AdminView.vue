<template>
  <div class="page">
    <header class="page-header">
      <h1 class="page-title">Admin Settings</h1>
    </header>

    <div class="admin-container">
      <div class="card admin-card">
        <h2>Change Password</h2>
        <p class="muted">Update your account password below.</p>

        <p v-if="successMessage" class="success-notice">{{ successMessage }}</p>
        <p v-if="errorMessage" class="notice">{{ errorMessage }}</p>

        <form class="password-form" @submit.prevent="changePassword">
          <div class="field">
            <label for="currentPassword">Current Password</label>
            <input
              id="currentPassword"
              v-model="currentPassword"
              type="password"
              autocomplete="current-password"
              required
            />
          </div>

          <div class="field">
            <label for="newPassword">New Password</label>
            <input
              id="newPassword"
              v-model="newPassword"
              type="password"
              autocomplete="new-password"
              minlength="6"
              required
            />
          </div>

          <div class="field">
            <label for="confirmPassword">Confirm New Password</label>
            <input
              id="confirmPassword"
              v-model="confirmPassword"
              type="password"
              autocomplete="new-password"
              minlength="6"
              required
            />
          </div>

          <div class="actions">
            <button class="button primary" :disabled="submitting">
              {{ submitting ? 'Updating…' : 'Update Password' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../lib/api/client'

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const submitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

async function changePassword() {
  errorMessage.value = ''
  successMessage.value = ''

  if (newPassword.value.length < 6) {
    errorMessage.value = 'New password must be at least 6 characters.'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'New passwords do not match.'
    return
  }

  submitting.value = true

  try {
    await api<void>('/v1/auth/password', {
      method: 'PATCH',
      body: JSON.stringify({
        current_password: currentPassword.value,
        new_password: newPassword.value,
      }),
    })
    successMessage.value = 'Password updated successfully!'
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (err: unknown) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update password.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.admin-container {
  max-width: 32rem;
}
.admin-card {
  padding: var(--space-5);
  display: grid;
  gap: 1.25rem;
}
.admin-card h2 {
  margin: 0;
  font-size: 1.25rem;
  letter-spacing: -0.02em;
}
.password-form {
  display: grid;
  gap: 1.25rem;
}
.actions {
  display: flex;
  justify-content: flex-start;
  margin-top: 0.5rem;
}
.success-notice {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  background: var(--accent-soft);
  color: var(--success);
  font-weight: 600;
}
</style>
