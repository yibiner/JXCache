export type ThemeMode = 'dark' | 'light'

const STORAGE_KEY = 'jce_theme_mode'

export function getThemeMode(): ThemeMode {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    if (v === 'dark' || v === 'light') return v
  } catch {
    // ignore
  }
  // Default to dark for developer-oriented UI
  return 'dark'
}

export function setThemeMode(mode: ThemeMode) {
  try {
    localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    // ignore
  }
  applyThemeMode(mode)
}

export function applyThemeMode(mode: ThemeMode) {
  const root = document.documentElement
  root.dataset.theme = mode
  // Element Plus dark vars are enabled via the "dark" class on html
  root.classList.toggle('dark', mode === 'dark')
}


