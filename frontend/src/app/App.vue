<template>
  <AuthGate>
    <div class="app-shell">
      <div
        v-if="sidebarOpen"
        class="sidebar-backdrop"
        aria-hidden="true"
        @click="closeSidebar"
      ></div>
      <AppSidebar />
      <main class="workspace">
        <header class="workspace-header">
          <button
            class="menu-button"
            type="button"
            aria-label="Open navigation"
            @click="toggleSidebar"
          >
            <IconMenu class="menu-icon" />
          </button>
          <RouterLink class="brand mobile-brand" to="/tasks">Tasker</RouterLink>
          <span class="connection" :class="{ offline: !online }">{{
            online ? 'Online' : 'Offline'
          }}</span>
        </header>
        <RouterView />
      </main>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import AppSidebar from '../components/layout/AppSidebar.vue'
import AuthGate from '../features/auth/AuthGate.vue'
import { IconMenu } from '../components/ui/icons'

const online = ref(navigator.onLine)
const sidebarOpen = ref(false)
const route = useRoute()

function setOnline() {
  online.value = navigator.onLine
}

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
  window.dispatchEvent(new CustomEvent('tasker:sidebar', { detail: sidebarOpen.value }))
}

function closeSidebar() {
  sidebarOpen.value = false
  window.dispatchEvent(new CustomEvent('tasker:sidebar', { detail: false }))
}

function onSidebarEvent(event: Event) {
  sidebarOpen.value = Boolean((event as CustomEvent<boolean>).detail)
}

watch(
  () => route.path,
  () => {
    if (sidebarOpen.value) {
      closeSidebar()
    }
  },
)

onMounted(() => {
  window.addEventListener('online', setOnline)
  window.addEventListener('offline', setOnline)
  window.addEventListener('tasker:sidebar', onSidebarEvent)
})

onBeforeUnmount(() => {
  window.removeEventListener('online', setOnline)
  window.removeEventListener('offline', setOnline)
  window.removeEventListener('tasker:sidebar', onSidebarEvent)
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
}
.workspace {
  min-width: 0;
  background: var(--color-bg-page);
}
.workspace-header {
  display: none;
  align-items: center;
  height: var(--header-height);
  padding: 0 var(--space-4);
  border-bottom: var(--border-width) solid var(--color-border);
  background: var(--color-bg-surface);
}
.menu-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  padding: 0;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}
.menu-button:hover {
  background: var(--color-bg-surface-muted);
  color: var(--color-text);
}
.menu-button:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}
.menu-icon {
  font-size: 1.5rem;
}
.mobile-brand {
  margin-left: var(--space-2);
  font-family: var(--font-heading);
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  letter-spacing: var(--tracking-tight);
  color: var(--color-primary);
  text-decoration: none;
}
.connection {
  margin-left: auto;
  color: var(--color-success);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}
.connection.offline {
  color: var(--color-danger);
}
.sidebar-backdrop {
  display: none;
}
@media (max-width: 720px) {
  .app-shell {
    display: block;
  }
  .workspace-header {
    display: flex;
  }
  .sidebar-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 90;
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(2px);
    animation: fadeIn var(--transition-fast) ease-out;
  }
}
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
