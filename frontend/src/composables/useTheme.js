import { ref } from 'vue'
import { getAvailableThemes as fetchAvailableThemes } from '../api/index.js'

const THEME_ID_STORAGE_KEY = 'bookapp_theme_id'
const SPLASH_DATE_STORAGE_KEY = 'bookapp_splash_date'

const currentTheme = ref(null)
const availableThemes = ref([])
const currentThemeId = ref(null)

function initThemeId() {
  const stored = localStorage.getItem(THEME_ID_STORAGE_KEY)
  if (stored) {
    currentThemeId.value = Number(stored)
  }
}
initThemeId()

export function useTheme() {

  async function loadAvailableThemes() {
    try {
      const response = await fetchAvailableThemes()
      availableThemes.value = response.data || []

      // 如果当前没有选中主题或选中的主题不在可用列表中，使用第一个主题（默认主题）
      const found = availableThemes.value.find(t => t.id === currentThemeId.value)
      if (!currentThemeId.value || !found) {
        if (availableThemes.value.length > 0) {
          currentThemeId.value = availableThemes.value[0].id
          localStorage.setItem(THEME_ID_STORAGE_KEY, String(currentThemeId.value))
        }
      }

      // 从已加载的列表中找到当前主题并应用
      if (currentThemeId.value) {
        const theme = availableThemes.value.find(t => t.id === currentThemeId.value)
        if (theme) {
          currentTheme.value = theme
          applyTheme(theme)
        }
      }
    } catch (error) {
      console.error('加载主题列表失败:', error)
    }
  }

  async function selectTheme(themeId) {
    // 优先从已加载的列表中查找，避免调用需要 ADMIN 权限的详情接口
    let theme = availableThemes.value.find(t => t.id === themeId)
    if (!theme) {
      // 列表中没有则重新加载
      const response = await fetchAvailableThemes()
      availableThemes.value = response.data || []
      theme = availableThemes.value.find(t => t.id === themeId)
    }
    if (theme) {
      currentTheme.value = theme
      currentThemeId.value = themeId
      localStorage.setItem(THEME_ID_STORAGE_KEY, String(themeId))
      applyTheme(theme)
    }
  }

  function applyTheme(theme) {
    if (!theme) return

    const root = document.documentElement
    if (theme.primaryColor) {
      root.style.setProperty('--theme-primary', theme.primaryColor)
    } else {
      root.style.removeProperty('--theme-primary')
    }
    if (theme.headerBg) {
      root.style.setProperty('--theme-header-bg', theme.headerBg)
    } else {
      root.style.removeProperty('--theme-header-bg')
    }
  }

  function getDecorationForTab(tabKey) {
    if (!currentTheme.value || !currentTheme.value.decorations) {
      return null
    }
    // 优先查找当前 tab 的专属装饰图片，没有则 fallback 到 transactions
    const decoration = currentTheme.value.decorations.find(d => d.tabKey === tabKey)
    if (decoration && (decoration.leftImageFilename || decoration.rightImageFilename)) {
      return decoration
    }
    return currentTheme.value.decorations.find(d => d.tabKey === 'transactions') || null
  }

  function shouldShowSplash() {
    if (!currentTheme.value || currentTheme.value.splashType === 'NONE') {
      return false
    }
    const today = new Date().toISOString().split('T')[0]
    const lastSplashDate = localStorage.getItem(SPLASH_DATE_STORAGE_KEY)
    return lastSplashDate !== today
  }

  function markSplashShown() {
    const today = new Date().toISOString().split('T')[0]
    localStorage.setItem(SPLASH_DATE_STORAGE_KEY, today)
  }

  function playSound() {
    if (!currentTheme.value || !currentTheme.value.soundFilename) {
      return
    }
    try {
      const audio = new Audio('/api/theme-assets/' + currentTheme.value.soundFilename)
      audio.play().catch(err => {
        console.warn('播放音效失败:', err)
      })
    } catch (error) {
      console.warn('创建音频对象失败:', error)
    }
  }

  return {
    currentTheme,
    availableThemes,
    currentThemeId,
    loadAvailableThemes,
    selectTheme,
    applyTheme,
    getDecorationForTab,
    shouldShowSplash,
    markSplashShown,
    playSound
  }
}
