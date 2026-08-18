<template>
  <div v-if="auth.loading.value" class="auth-state">Loading your workspace…</div>
  <section v-else-if="!auth.session.value" class="signin">
    <form class="signin-card card" @submit.prevent="handleSubmit">
      <p class="eyebrow">PERSONAL WORKSPACE</p>

      <div class="mode-tabs">
        <button
          type="button"
          class="tab-btn"
          :class="{ active: mode === 'signin' }"
          @click="switchMode('signin')"
        >
          Sign in
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >
          Create account
        </button>
      </div>

      <h1>{{ mode === 'signin' ? 'Welcome back.' : 'Create account.' }}</h1>
      <p class="muted">
        {{
          mode === 'signin'
            ? 'Sign in to access your private tasks and notes.'
            : 'Register a new admin account for your workspace.'
        }}
      </p>

      <p v-if="auth.error.value" class="notice">{{ auth.error.value }}</p>

      <div class="field">
        <label for="username">Username</label>
        <input id="username" v-model="username" type="text" autocomplete="username" required />
      </div>

      <div class="field">
        <label for="password">Password</label>
        <input
          id="password"
          v-model="password"
          type="password"
          :autocomplete="mode === 'signin' ? 'current-password' : 'new-password'"
          required
        />
      </div>

      <div v-if="mode === 'register'" class="field">
        <label for="confirm-password">Confirm Password</label>
        <input
          id="confirm-password"
          v-model="confirmPassword"
          type="password"
          autocomplete="new-password"
          required
        />
      </div>

      <div class="field-remember">
        <label for="remember-me" class="remember-label">
          <input id="remember-me" v-model="rememberMe" type="checkbox" class="remember-checkbox" />
          <span>Remember me (7 days)</span>
        </label>
      </div>

      <button class="button primary" :disabled="sending">
        {{
          sending
            ? mode === 'signin'
              ? 'Signing in…'
              : 'Registering…'
            : mode === 'signin'
              ? 'Sign in'
              : 'Create account'
        }}
      </button>
    </form>
  </section>
  <slot v-else />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuth } from './useAuth'

const auth = useAuth()
const mode = ref<'signin' | 'register'>('signin')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const rememberMe = ref(false)
const sending = ref(false)

onMounted(() => {
  void auth.ready()
})

function switchMode(newMode: 'signin' | 'register') {
  mode.value = newMode
  auth.error.value = null
  password.value = ''
  confirmPassword.value = ''
}

async function handleSubmit() {
  if (mode.value === 'register') {
    if (password.value.length < 6) {
      auth.error.value = 'Password must be at least 6 characters.'
      return
    }
    if (password.value !== confirmPassword.value) {
      auth.error.value = 'Passwords do not match.'
      return
    }
  }

  sending.value = true
  auth.error.value = null
  try {
    if (mode.value === 'signin') {
      await auth.signIn(username.value, password.value, rememberMe.value)
    } else {
      await auth.signUp(username.value, password.value, rememberMe.value)
    }
  } catch (error: unknown) {
    auth.error.value = error instanceof Error ? error.message : 'Authentication failed.'
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
.mode-tabs {
  display: flex;
  background: var(--surface-muted, #f1f3f0);
  padding: 3px;
  border-radius: 8px;
  gap: 4px;
}
.tab-btn {
  flex: 1;
  padding: 0.45rem;
  border: none;
  background: transparent;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-muted);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.tab-btn.active {
  background: white;
  color: var(--text);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
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
