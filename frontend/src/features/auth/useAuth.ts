import { computed, ref } from 'vue'
import { api, getToken, setToken } from '../../lib/api/client'

export interface AuthUser {
  id: string
  username: string
}

export interface AuthSession {
  token: string
  user: AuthUser
}

const session = ref<AuthSession | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
let initialize: Promise<void> | null = null

async function ready() {
  if (!initialize) {
    initialize = (async () => {
      const jwt = getToken()
      if (!jwt) {
        session.value = null
        loading.value = false
        return
      }
      try {
        const user = await api<AuthUser>('/v1/me')
        session.value = { token: jwt, user }
      } catch {
        setToken(null)
        session.value = null
      } finally {
        loading.value = false
      }
    })()
  }
  await initialize
}

export function useAuth() {
  return {
    session,
    loading,
    error,
    ready,
    isSignedIn: computed(() => !!session.value),
    async signIn(username: string, password: string) {
      error.value = null
      const res = await api<{ token: string; user: AuthUser }>('/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      })
      setToken(res.token)
      session.value = { token: res.token, user: res.user }
    },
    signOut() {
      setToken(null)
      session.value = null
    },
  }
}
