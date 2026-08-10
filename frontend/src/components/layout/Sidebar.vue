<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <aside class="sidebar" :class="{ open }">
    <RouterLink class="brand" to="/tasks">Tasker</RouterLink>
    <nav aria-label="Main navigation">
      <RouterLink to="/tasks">Tasks</RouterLink>
      <RouterLink to="/notes">Notes</RouterLink>
      <RouterLink to="/money">Money</RouterLink>
      <RouterLink to="/search">Search</RouterLink>
      <RouterLink to="/admin">Settings</RouterLink>
    </nav>
    <div class="account">
      <span class="user-badge">{{ auth.session.value?.user.username }}</span>
      <button type="button" @click="auth.signOut">Sign out</button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuth } from '../../features/auth/useAuth'

const auth = useAuth()
const open = ref(false)

function setOpen(event: Event) {
  open.value = Boolean((event as CustomEvent<boolean>).detail)
}

onMounted(() => window.addEventListener('tasker:sidebar', setOpen))
onBeforeUnmount(() => window.removeEventListener('tasker:sidebar', setOpen))
</script>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 1.5rem 1rem;
  border-right: 1px solid var(--border);
  background: var(--surface);
}
.brand {
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: -0.05em;
  text-decoration: none;
  color: var(--accent);
}
nav {
  display: grid;
  gap: 0.3rem;
  margin-top: 2.5rem;
}
nav a {
  padding: 0.65rem 0.75rem;
  border-radius: 7px;
  text-decoration: none;
  color: var(--text-muted);
  font-weight: 650;
}
nav a.router-link-active {
  background: var(--accent-soft);
  color: var(--accent);
}
.account {
  display: grid;
  gap: 0.5rem;
  margin-top: auto;
  padding: 0.75rem;
  font-size: 0.8125rem;
  color: var(--text-muted);
  overflow-wrap: anywhere;
}
.user-badge {
  font-weight: 650;
  color: var(--text);
}
.account button {
  width: max-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-muted);
  text-decoration: underline;
}
@media (max-width: 720px) {
  .sidebar {
    position: fixed;
    z-index: 10;
    left: 0;
    width: 15.5rem;
    transform: translateX(-101%);
    transition: transform 0.2s;
  }
  .sidebar.open {
    transform: translateX(0);
  }
}
</style>
