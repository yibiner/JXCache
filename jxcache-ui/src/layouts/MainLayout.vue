<template>
  <div class="app-layout">
    <header class="app-header">
      <div class="header-brand">
        <div class="logo">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7l10 5 10-5-10-5z" fill="currentColor" opacity="0.8"/>
            <path d="M2 17l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M2 12l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <span class="brand-name">JXCache</span>
      </div>
      <div class="header-actions">
        <el-select
          v-model="currentTheme"
          @change="handleThemeChange"
          class="theme-select"
          size="small"
        >
          <el-option :label="$t('layout.themeDark')" value="dark" />
          <el-option :label="$t('layout.themeLight')" value="light" />
        </el-select>
        <el-select
          v-model="currentLocale"
          @change="handleLocaleChange"
          class="lang-select"
          size="small"
        >
          <el-option label="中文" value="zh-cn" />
          <el-option label="EN" value="en" />
        </el-select>
      </div>
    </header>

    <div class="app-body">
      <aside class="app-sidebar">
        <nav class="nav-menu">
          <router-link 
            v-for="item in menuItems" 
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <component :is="item.icon" class="nav-icon" />
            <span class="nav-label">{{ $t(item.label) }}</span>
          </router-link>
        </nav>
      </aside>

      <main class="app-content">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, markRaw } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search, Connection } from '@element-plus/icons-vue'
import { setLocale } from '@/i18n'
import { getThemeMode, setThemeMode, type ThemeMode } from '@/utils/theme'

const route = useRoute()
const { locale } = useI18n()

const currentLocale = ref(locale.value as string)
const currentTheme = ref<ThemeMode>(getThemeMode())

const menuItems = [
  { path: '/observer', label: 'nav.observer', icon: markRaw(Search) },
  { path: '/aggregator', label: 'nav.aggregator', icon: markRaw(Connection) }
]

const isActive = (path: string) => route.path === path

const handleLocaleChange = (value: string) => {
  setLocale(value as 'zh-cn' | 'en')
}

const handleThemeChange = (value: ThemeMode) => {
  setThemeMode(value)
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-dark);
  overflow: hidden;
}

.app-header {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .logo {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--primary-color), #00a080);
    border-radius: 10px;
    color: var(--bg-dark);
  }
  
  .brand-name {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: -0.02em;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.theme-select {
  width: 88px;
  
  :deep(.el-select__wrapper) {
    background: transparent;
    border-color: var(--border-color);
    min-height: 32px;
    
    &:hover {
      border-color: var(--primary-color);
    }
  }
}

.lang-select {
  width: 80px;
  
  :deep(.el-select__wrapper) {
    background: transparent;
    border-color: var(--border-color);
    min-height: 32px;
    
    &:hover {
      border-color: var(--primary-color);
    }
  }
}

.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.app-sidebar {
  width: 200px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  padding: 16px 12px;
  flex-shrink: 0;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s ease;
  font-weight: 500;
  
  &:hover {
    background: var(--bg-elevated);
    color: var(--text-primary);
  }
  
  &.active {
    background: var(--primary-light);
    color: var(--primary-color);
    
    .nav-icon {
      color: var(--primary-color);
    }
  }
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.nav-label {
  font-size: 14px;
}

.app-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: var(--bg-dark);
}
</style>
