<template>
  <AuthGate>
    <div class="app-shell">
      <AppSidebar />
      <main class="workspace">
        <header class="workspace-header">
          <button
            class="menu-button"
            type="button"
            aria-label="Open navigation"
            @click="toggleSidebar"
          >
            <span></span><span></span><span></span>
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
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import AppSidebar from '../components/layout/AppSidebar.vue'
import AuthGate from '../features/auth/AuthGate.vue'

const online = ref(navigator.onLine)
const sidebarOpen = ref(false)
function setOnline() {
  online.value = navigator.onLine
}
function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
  window.dispatchEvent(new CustomEvent('tasker:sidebar', { detail: sidebarOpen.value }))
}
onMounted(() => {
  window.addEventListener('online', setOnline)
  window.addEventListener('offline', setOnline)
})
onBeforeUnmount(() => {
  window.removeEventListener('online', setOnline)
  window.removeEventListener('offline', setOnline)
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 15.5rem minmax(0, 1fr);
}
.workspace {
  min-width: 0;
}
.workspace-header {
  display: none;
  align-items: center;
  height: 3.5rem;
  padding: 0 var(--space-4);
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}
.menu-button {
  width: 2.5rem;
  height: 2.5rem;
  padding: 0.55rem;
  border: 0;
  background: transparent;
}
.menu-button span {
  display: block;
  height: 2px;
  margin: 4px;
  background: currentColor;
}
.mobile-brand {
  margin-left: var(--space-2);
}
.connection {
  margin-left: auto;
  color: var(--success);
  font-size: var(--font-small);
}
.connection.offline {
  color: var(--danger);
}
@media (max-width: 720px) {
  .app-shell {
    display: block;
  }
  .workspace-header {
    display: flex;
  }
}
</style>
