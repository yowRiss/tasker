/**
 * Theme management composable
 * Handles light/dark mode switching with system preference detection and persistence
 */

import { computed, onMounted, ref, watch } from 'vue'

type Theme = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'tasker-theme'

// Global state shared across all instances
const userPreference = ref<Theme>((localStorage.getItem(STORAGE_KEY) as Theme) || 'system')

// Get system preference
function getSystemPreference(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

// Compute the active theme
const activeTheme = computed<'light' | 'dark'>(() => {
  if (userPreference.value === 'system') {
    return getSystemPreference()
  }
  return userPreference.value
})

// Apply theme to document
function applyTheme(theme: 'light' | 'dark') {
  document.documentElement.setAttribute('data-theme', theme)
}

// Set the theme
function setTheme(theme: Theme) {
  userPreference.value = theme
  localStorage.setItem(STORAGE_KEY, theme)
}

// Toggle between light and dark (ignores system)
function toggleTheme() {
  const next = activeTheme.value === 'light' ? 'dark' : 'light'
  setTheme(next)
}

// Cycle through: light -> dark -> system -> light
function cycleTheme() {
  const order: Theme[] = ['light', 'dark', 'system']
  const currentIndex = order.indexOf(userPreference.value)
  const safeIndex = currentIndex >= 0 ? currentIndex : 0
  const nextIndex = (safeIndex + 1) % order.length
  const nextTheme = order[nextIndex]
  if (nextTheme) {
    setTheme(nextTheme)
  }
}

export function useTheme() {
  // Set up system preference listener
  onMounted(() => {
    // Apply initial theme
    applyTheme(activeTheme.value)

    // Listen for system preference changes
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const handleChange = () => {
      if (userPreference.value === 'system') {
        applyTheme(getSystemPreference())
      }
    }

    mediaQuery.addEventListener('change', handleChange)

    // Watch for user preference changes
    watch(activeTheme, (newTheme) => {
      applyTheme(newTheme)
    })
  })

  return {
    /** Current user preference ('light', 'dark', or 'system') */
    preference: userPreference,
    /** Computed active theme ('light' or 'dark') */
    theme: activeTheme,
    /** Set the theme preference */
    setTheme,
    /** Toggle between light and dark */
    toggleTheme,
    /** Cycle through light -> dark -> system */
    cycleTheme,
    /** Check if current theme is dark */
    isDark: computed(() => activeTheme.value === 'dark'),
  }
}

export type { Theme }
