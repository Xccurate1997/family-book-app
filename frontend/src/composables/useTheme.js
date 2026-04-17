import { ref } from 'vue'

const STORAGE_KEY = 'bookapp_theme'

const defaultSettings = () => ({
  mode: 'solid',
  solidColor: '#f0f2f5',
  gradientColor1: '#667eea',
  gradientColor2: '#764ba2',
  gradientDirection: 'to bottom right',
  imageData: null,
  overlayOpacity: 0.7
})

const themeSettings = ref(defaultSettings())

function applyToDOM(settings) {
  const root = document.documentElement
  let bg
  switch (settings.mode) {
    case 'gradient':
      bg = settings.gradientDirection === 'circle'
        ? `radial-gradient(circle, ${settings.gradientColor1}, ${settings.gradientColor2})`
        : `linear-gradient(${settings.gradientDirection}, ${settings.gradientColor1}, ${settings.gradientColor2})`
      break
    case 'image':
      bg = settings.imageData
        ? `url(${settings.imageData}) center/cover no-repeat fixed`
        : settings.solidColor
      break
    default:
      bg = settings.solidColor
  }
  root.style.setProperty('--app-bg', bg)
  root.style.setProperty('--app-overlay-opacity', String(settings.overlayOpacity))
}

export function useTheme() {
  const loadTheme = () => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        themeSettings.value = { ...defaultSettings(), ...JSON.parse(saved) }
      }
    } catch { /* ignore */ }
    applyToDOM(themeSettings.value)
  }

  const applyTheme = (settings) => {
    themeSettings.value = { ...settings }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
    applyToDOM(settings)
  }

  const resetTheme = () => {
    const defaults = defaultSettings()
    themeSettings.value = defaults
    localStorage.removeItem(STORAGE_KEY)
    applyToDOM(defaults)
  }

  return { themeSettings, loadTheme, applyTheme, resetTheme }
}
