import { computed, ref } from 'vue'
import {
  contributeTarget,
  createTarget,
  deleteTarget,
  listTargets,
  updateTarget,
} from '../money.api'
import type {
  Target,
  TargetContributeInput,
  TargetInput,
  TargetPatchInput,
} from '../money.types'

export function useTargets() {
  const targets = ref<Target[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const activeTargets = computed(() => targets.value.filter((t) => t.status === 'active'))
  const achievedTargets = computed(() =>
    targets.value.filter((t) => t.status === 'achieved' || t.is_achieved),
  )

  const totalTargetAmount = computed(() => {
    return targets.value.reduce((sum, t) => sum + (parseFloat(t.target_amount) || 0), 0)
  })

  const totalCurrentAmount = computed(() => {
    return targets.value.reduce((sum, t) => sum + (parseFloat(t.current_amount) || 0), 0)
  })

  const overallProgressPercent = computed(() => {
    if (totalTargetAmount.value <= 0) return 0
    return Math.min(100, Math.round((totalCurrentAmount.value / totalTargetAmount.value) * 100))
  })

  async function load(status: string = '') {
    loading.value = true
    error.value = null
    try {
      const res = await listTargets({ status })
      targets.value = res.items
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load targets'
    } finally {
      loading.value = false
    }
  }

  async function addTarget(input: TargetInput) {
    error.value = null
    try {
      const created = await createTarget(input)
      targets.value.unshift(created)
      return created
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to create target'
      throw e
    }
  }

  async function editTarget(id: string, input: TargetPatchInput) {
    error.value = null
    try {
      const updated = await updateTarget(id, input)
      const idx = targets.value.findIndex((t) => t.id === id)
      if (idx !== -1) {
        targets.value[idx] = updated
      }
      return updated
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to update target'
      throw e
    }
  }

  async function removeTarget(id: string) {
    error.value = null
    try {
      await deleteTarget(id)
      targets.value = targets.value.filter((t) => t.id !== id)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to delete target'
      throw e
    }
  }

  async function contribute(id: string, input: TargetContributeInput) {
    error.value = null
    try {
      const updated = await contributeTarget(id, input)
      const idx = targets.value.findIndex((t) => t.id === id)
      if (idx !== -1) {
        targets.value[idx] = updated
      }
      return updated
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to adjust target balance'
      throw e
    }
  }

  return {
    targets,
    activeTargets,
    achievedTargets,
    totalTargetAmount,
    totalCurrentAmount,
    overallProgressPercent,
    loading,
    error,
    load,
    addTarget,
    editTarget,
    removeTarget,
    contribute,
  }
}
