<template>
  <button
    type="button"
    class="theme-toggle"
    :aria-label="`Switch to ${isDark ? 'light' : 'dark'} mode`"
    :title="`Current: ${theme} mode. Click to toggle.`"
    @click="toggleTheme"
  >
    <IconSun v-if="isDark" class="icon" />
    <IconMoon v-else class="icon" />
  </button>
</template>

<script setup lang="ts">
import { IconSun, IconMoon } from './icons'
import { useTheme } from '../../composables/useTheme'

const { theme, isDark, toggleTheme } = useTheme()
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  padding: 0;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}

.theme-toggle:hover {
  background: var(--color-bg-surface-muted);
  color: var(--color-text);
}

.theme-toggle:focus-visible {
  outline: none;
  box-shadow: var(--focus-ring);
}

.icon {
  font-size: 1.25rem;
}
</style>
