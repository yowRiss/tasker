import { ref } from 'vue'
import { createCategory, deleteCategory, listCategories, updateCategory } from '../money.api'
import type { Category, CategoryInput, CategoryPatchInput } from '../money.types'
import { ApiError } from '../../../lib/api/client'

export function useCategories() {
  const categories = ref<Category[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load(type?: 'income' | 'expense' | '', includeArchived = false) {
    loading.value = true
    error.value = null
    try {
      const res = await listCategories({ type, include_archived: includeArchived })
      categories.value = res.items
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load categories'
    } finally {
      loading.value = false
    }
  }

  async function addCategory(input: CategoryInput) {
    error.value = null
    try {
      const cat = await createCategory(input)
      categories.value.push(cat)
      return cat
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to create category'
      throw e
    }
  }

  async function editCategory(id: string, input: CategoryPatchInput) {
    error.value = null
    try {
      const updated = await updateCategory(id, input)
      const index = categories.value.findIndex((c) => c.id === id)
      if (index !== -1) {
        categories.value[index] = updated
      }
      return updated
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to update category'
      throw e
    }
  }

  async function removeCategory(id: string) {
    error.value = null
    try {
      await deleteCategory(id)
      categories.value = categories.value.filter((c) => c.id !== id)
    } catch (e: unknown) {
      if (e instanceof ApiError && e.status === 409) {
        throw new Error('Cannot delete category with dependent records. Archive it instead.')
      }
      error.value = e instanceof Error ? e.message : 'Failed to delete category'
      throw e
    }
  }

  return {
    categories,
    loading,
    error,
    load,
    addCategory,
    editCategory,
    removeCategory,
  }
}
