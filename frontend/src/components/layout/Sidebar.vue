<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <aside class="sidebar" :class="{ open }">
    <RouterLink class="brand" to="/tasks" @click="closeSidebarIfMobile">Tasker</RouterLink>
    <nav aria-label="Main navigation">
      <RouterLink to="/tasks" @click="closeSidebarIfMobile">Tasks</RouterLink>
      <RouterLink to="/notes" @click="closeSidebarIfMobile">Notes</RouterLink>
      <RouterLink to="/money" @click="closeSidebarIfMobile">Money</RouterLink>
      <RouterLink to="/archive" @click="closeSidebarIfMobile">Archive</RouterLink>
      <RouterLink to="/search" @click="closeSidebarIfMobile">Search</RouterLink>
      <RouterLink to="/admin" @click="closeSidebarIfMobile">Settings</RouterLink>
    </nav>
    <div class="sidebar-footer">
      <ThemeToggle />
      <div class="account">
        <span class="user-badge">{{ auth.session.value?.user.username }}</span>
        <button type="button" @click="auth.signOut">Sign out</button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuth } from '../../features/auth/useAuth'
import ThemeToggle from '../ui/ThemeToggle.vue'

const auth = useAuth()
const open = ref(false)

function setOpen(event: Event) {
  open.value = Boolean((event as CustomEvent<boolean>).detail)
}

function closeSidebarIfMobile() {
  if (window.innerWidth <= 720) {
    window.dispatchEvent(new CustomEvent('tasker:sidebar', { detail: false }))
  }
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
  padding: var(--space-5) var(--space-4);
  border-right: var(--border-width) solid var(--color-border);
  background: var(--color-bg-surface);
}
.brand {
  font-family: var(--font-heading);
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  letter-spacing: var(--tracking-tight);
  text-decoration: none;
  color: var(--color-primary);
  transition: color var(--transition-fast);
}
.brand:hover {
  color: var(--color-primary-hover);
}
nav {
  display: grid;
  gap: var(--space-1);
  margin-top: var(--space-5);
}
nav a {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: var(--color-text-muted);
  font-weight: var(--font-semibold);
  transition: background-color var(--transition-fast), color var(--transition-fast);
}
nav a:hover {
  background: var(--color-bg-surface-muted);
  color: var(--color-text);
}
nav a.router-link-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}
.sidebar-footer {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: auto;
  padding-top: var(--space-4);
  border-top: var(--border-width) solid var(--color-border);
}
.account {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}
.user-badge {
  font-weight: var(--font-semibold);
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.account button {
  width: max-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  text-decoration: underline;
  cursor: pointer;
  transition: color var(--transition-fast);
}
.account button:hover {
  color: var(--color-text);
}
@media (max-width: 720px) {
  .sidebar {
    position: fixed;
    z-index: 100;
    left: 0;
    width: var(--sidebar-width);
    max-width: 80vw;
    transform: translateX(-101%);
    transition: transform var(--transition-base);
    box-shadow: var(--shadow-lg);
  }
  .sidebar.open {
    transform: translateX(0);
  }
}
</style>
