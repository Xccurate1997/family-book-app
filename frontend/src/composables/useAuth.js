import { ref, computed } from 'vue'

const TOKEN_KEY = 'bookapp_token'
const USER_KEY = 'bookapp_user'

const token = ref(localStorage.getItem(TOKEN_KEY) || null)
const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const currentUser = computed(() => user.value)

  function setAuth(authData) {
    token.value = authData.token
    user.value = {
      userId: authData.userId,
      username: authData.username,
      nickname: authData.nickname,
      role: authData.role
    }
    localStorage.setItem(TOKEN_KEY, authData.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function getToken() {
    return token.value
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    currentUser,
    setAuth,
    clearAuth,
    getToken
  }
}
