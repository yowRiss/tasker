<template>
  <div v-if="auth.loading.value" class="auth-state">Loading your workspace…</div>
  <section v-else-if="!auth.session.value" class="signin">
    <form class="signin-card card" @submit.prevent="signIn">
      <p class="eyebrow">PERSONAL WORKSPACE</p>
      <h1>Welcome back.</h1>
      <p class="muted">Sign in to access your private tasks and notes.</p>
      <p v-if="auth.error.value" class="notice">{{ auth.error.value }}</p>
      <div class="field">
        <label for="username">Username</label>
        <input
          id="username"
          v-model="username"
          type="text"
          autocomplete="username"
          required
        />
      </div>
      <div class="field">
        <label for="password">Password</label>
        <input
          id="password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          required
        />
      </div>
      <div class="field-remember">
        <label for="remember-me" class="remember-label">
          <input
            id="remember-me"
            v-model="rememberMe"
            type="checkbox"
            class="remember-checkbox"
          />
          <span>Remember me (7 days)</span>
        </label>
      </div>
      <button class="button primary" :disabled="sending">
        {{ sending ? 'Signing in…' : 'Sign in' }}
      </button>
    </form>
  </section>
  <slot v-else />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuth } from './useAuth'

const auth = useAuth()
const username = ref('')
const password = ref('')
const rememberMe = ref(false)
const sending = ref(false)

onMounted(() => {
  void auth.ready()
})

async function signIn() {
  sending.value = true
  auth.error.value = null
  try {
    await auth.signIn(username.value, password.value, rememberMe.value)
  } catch (error: unknown) {
    auth.error.value = error instanceof Error ? error.message : 'Invalid username or password.'
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.auth-state,
.signin {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 1.5rem;
}
.signin {
  background: linear-gradient(135deg, #edf4ec, #f7f7f5 60%);
}
.signin-card {
  width: min(100%, 26rem);
  padding: 2rem;
  display: grid;
  gap: 1rem;
}
.signin-card h1 {
  margin: 0;
  font-size: 2rem;
  letter-spacing: -0.05em;
}
.eyebrow {
  margin: 0;
  color: var(--accent);
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.12em;
}
.field-remember {
  display: flex;
  align-items: center;
  margin-top: -0.25rem;
}
.remember-label {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  font-size: var(--font-small);
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  user-select: none;
  transition: color 0.15s ease;
}
.remember-label:hover {
  color: var(--text);
}
.remember-checkbox {
  width: 1.05rem;
  height: 1.05rem;
  accent-color: var(--accent);
  border-radius: 4px;
  cursor: pointer;
}
</style>
