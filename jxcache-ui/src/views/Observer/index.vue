<template>
  <div class="observer-page">
    <div class="config-card">
      <div class="config-header">
        <span class="config-title">{{ $t('observer.serverConfig') }}</span>
        <el-tag v-if="isConnected" type="success" size="small">
          {{ $t('observer.connected') }}
        </el-tag>
        <el-tag v-else type="danger" size="small">
          {{ $t('observer.disconnected') }}
        </el-tag>
      </div>
      <div class="config-body">
        <el-input
          v-model="serverUrl"
          :placeholder="$t('observer.serverUrlPlaceholder')"
          class="server-input"
          @keyup.enter="handleConnect"
        >
          <template #prepend>
            <el-icon><Link /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleConnect" :loading="connecting">
          {{ $t('observer.connect') }}
        </el-button>
      </div>
    </div>

    <div class="query-card">
      <div class="card-header">
        <span class="card-title">{{ $t('observer.title') }}</span>
      </div>
      <div class="card-body">
        <div class="query-form">
          <div class="form-row">
            <div class="form-item">
              <label class="form-label">{{ $t('observer.area') }}</label>
              <el-input
                v-model="queryForm.area"
                :placeholder="$t('observer.areaPlaceholder')"
                clearable
              />
            </div>
            <div class="form-item">
              <label class="form-label required">{{ $t('observer.cacheName') }}</label>
              <el-input
                v-model="queryForm.cacheName"
                :placeholder="$t('observer.cacheNamePlaceholder')"
                clearable
              />
            </div>
            <div class="form-item">
              <label class="form-label">{{ $t('observer.keyPrefix') }}</label>
              <el-input
                v-model="queryForm.keyPrefix"
                :placeholder="$t('observer.keyPrefixPlaceholder')"
                clearable
              />
            </div>
            <div class="form-item">
              <label class="form-label">{{ $t('observer.level') }}</label>
              <el-select
                v-model="queryForm.level"
                :placeholder="$t('observer.levelPlaceholder')"
                class="level-select"
              >
                <el-option label="L0" value="L0">
                  <span>{{ $t('observer.levelL0') }}</span>
                </el-option>
                <el-option label="L1" value="L1">
                  <span>{{ $t('observer.levelL1') }}</span>
                </el-option>
                <el-option label="AUTO" value="AUTO">
                  <span>{{ $t('observer.levelAuto') }}</span>
                </el-option>
              </el-select>
            </div>
          </div>
          <div class="form-actions">
            <el-button type="primary" @click="handleQuery" :loading="loading" :disabled="!isConnected">
              <el-icon><Search /></el-icon>
              {{ $t('observer.query') }}
            </el-button>
            <span v-if="queryForm.level !== 'L0'" class="level-hint">
              {{ $t('observer.listOnlySupportsL0') }}
            </span>
            <el-button @click="handleReset">
              {{ $t('common.reset') }}
            </el-button>
          </div>
        </div>

        <div v-if="snapshot" class="results-section">
          <div class="results-header">
            <span class="results-count">
              {{ $t('observer.total') }}: <strong>{{ snapshot.total }}</strong>
            </span>
          </div>
          
          <el-table
            :data="snapshot.entries"
            v-loading="loading"
            stripe
            class="results-table"
          >
            <el-table-column prop="key" :label="$t('observer.key')" min-width="200">
              <template #default="{ row }">
                <span class="mono">{{ row.key }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="valueType" :label="$t('observer.valueType')" width="150">
              <template #default="{ row }">
                <el-tag size="small">{{ row.valueType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="valuePreview" :label="$t('observer.valuePreview')" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="mono text-secondary">{{ row.valuePreview }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('observer.createTime')" width="170">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.operation')" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="handleViewDetail(row)">
                  {{ $t('observer.viewDetail') }}
                </el-button>
                <el-button type="danger" size="small" link @click="handleInvalidate(row)">
                  {{ $t('observer.invalidate') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="results-footer">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="snapshot.total"
              layout="total, sizes, prev, pager, next"
              @size-change="handlePageSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </div>

        <div v-else class="empty-state">
          <el-icon class="empty-icon"><Search /></el-icon>
          <p class="empty-text">{{ $t('observer.emptyTip') }}</p>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      :title="$t('observer.viewDetail')"
      width="700px"
      class="detail-dialog"
    >
      <div v-if="detail" class="detail-content">
        <div class="detail-grid">
          <div class="detail-item">
            <label>{{ $t('observer.key') }}</label>
            <span class="mono">{{ detail.key }}</span>
          </div>
          <div class="detail-item">
            <label>{{ $t('observer.valueType') }}</label>
            <el-tag size="small">{{ detail.valueType }}</el-tag>
          </div>
          <div class="detail-item">
            <label>{{ $t('observer.area') }}</label>
            <span>{{ detail.area }}</span>
          </div>
          <div class="detail-item">
            <label>{{ $t('observer.cacheName') }}</label>
            <span>{{ detail.cacheName }}</span>
          </div>
          <div class="detail-item" v-if="detail.requestedLevel">
            <label>{{ $t('observer.requestedLevel') }}</label>
            <el-tag size="small" type="info">{{ detail.requestedLevel }}</el-tag>
          </div>
          <div class="detail-item" v-if="detail.hitLevel">
            <label>{{ $t('observer.hitLevel') }}</label>
            <el-tag size="small" type="success">{{ detail.hitLevel }}</el-tag>
          </div>
        </div>
        <div class="detail-value">
          <label>{{ $t('observer.valuePreview') }}</label>
          <pre class="value-code mono">{{ detail.value }}</pre>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="invalidateDialogVisible"
      :title="$t('observer.invalidate')"
      width="480px"
    >
      <div class="invalidate-content">
        <p class="invalidate-tip">{{ $t('observer.invalidateTip') }}</p>
        <div class="invalidate-key mono">{{ invalidateForm.key }}</div>
        <el-checkbox v-model="invalidateForm.invalidateRemote" class="mt-16">
          {{ $t('observer.invalidateRemote') }}
        </el-checkbox>
      </div>
      <template #footer>
        <el-button @click="invalidateDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="danger" @click="handleConfirmInvalidate" :loading="invalidating">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Search, Link } from '@element-plus/icons-vue'
import { createObserverApi } from '@/api/observer'
import { getObserverUrl, setObserverUrl } from '@/utils/api-config'
import type { QueryRequest, LocalCacheSnapshot, LocalCacheEntryDetail } from '@/api/types'
import { formatDate } from '@/utils/date'

const { t } = useI18n()

// 页面状态
const serverUrl = ref('')
const isConnected = ref(false)
const connecting = ref(false)
const loading = ref(false)
const snapshot = ref<LocalCacheSnapshot | null>(null)
const detail = ref<LocalCacheEntryDetail | null>(null)
const detailDialogVisible = ref(false)
const invalidateDialogVisible = ref(false)
const invalidating = ref(false)
const initialized = ref(false)

// API 实例
let api = createObserverApi(getObserverUrl())

const queryForm = reactive({
  area: 'default',
  cacheName: '',
  keyPrefix: '',
  level: 'L0'
})

const pagination = reactive({
  page: 1,
  pageSize: 20
})

const invalidateForm = reactive({
  area: '',
  cacheName: '',
  key: '',
  invalidateRemote: false
})

onMounted(() => {
  // 仅在首次挂载时初始化并自动连接
  if (initialized.value) return
  initialized.value = true
  
  serverUrl.value = getObserverUrl()
  // 首次挂载时自动连接
  handleConnect(true)
})

const handleConnect = async (silent = false) => {
  if (!serverUrl.value) {
    if (!silent) {
      ElMessage.warning(t('observer.serverUrlRequired'))
    }
    return
  }

  connecting.value = true
  try {
    setObserverUrl(serverUrl.value)
    api = createObserverApi(serverUrl.value)
    
    // 通过 areas 接口测试连通性
    await api.getAreas()
    isConnected.value = true
    if (!silent) {
      ElMessage.success(t('observer.connectSuccess'))
    }
  } catch (error) {
    isConnected.value = false
    if (!silent) {
      ElMessage.error(t('observer.connectFailed'))
    }
  } finally {
    connecting.value = false
  }
}

const handleQuery = async () => {
  if (!queryForm.cacheName) {
    ElMessage.warning(t('observer.cacheNameRequired'))
    return
  }

  loading.value = true
  try {
    const request: QueryRequest = {
      area: queryForm.area || 'default',
      cacheName: queryForm.cacheName,
      level: queryForm.level || 'L0',
      keyPrefix: queryForm.keyPrefix || undefined,
      pageRequest: {
        pageNo: pagination.page,
        pageSize: pagination.pageSize
      }
    }
    snapshot.value = await api.query(request)
    if (snapshot.value?.message) {
      ElMessage.warning(snapshot.value.message)
    }
  } catch (error) {
    ElMessage.error(t('common.error'))
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryForm.area = 'default'
  queryForm.cacheName = ''
  queryForm.keyPrefix = ''
  queryForm.level = 'L0'
  snapshot.value = null
  pagination.page = 1
}

const handleViewDetail = async (row: any) => {
  try {
    // 详情查询使用当前选中的 level；默认 L0（本地）
    detail.value = await api.getEntry(queryForm.area || 'default', queryForm.cacheName, row.key, queryForm.level || 'L0')
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(t('common.error'))
  }
}

const handleInvalidate = (row: any) => {
  invalidateForm.area = queryForm.area || 'default'
  invalidateForm.cacheName = queryForm.cacheName
  invalidateForm.key = row.key
  invalidateForm.invalidateRemote = false
  invalidateDialogVisible.value = true
}

const handleConfirmInvalidate = async () => {
  invalidating.value = true
  try {
    await api.invalidate({
      area: invalidateForm.area,
      cacheName: invalidateForm.cacheName,
      key: invalidateForm.key,
      invalidateRemote: invalidateForm.invalidateRemote
    })
    ElMessage.success(t('common.success'))
    invalidateDialogVisible.value = false
    handleQuery()
  } catch (error) {
    ElMessage.error(t('common.error'))
  } finally {
    invalidating.value = false
  }
}

const handlePageChange = (page: number) => {
  pagination.page = page
  handleQuery()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.page = 1
  handleQuery()
}
</script>

<style scoped lang="scss">
.observer-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.config-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.config-header {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 12px;
}

.config-title {
  font-weight: 600;
  color: var(--text-primary);
}

.config-body {
  padding: 16px 20px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.server-input {
  flex: 1;
  max-width: 500px;
}

.query-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-body {
  padding: 20px;
}

.query-form {
  margin-bottom: 24px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  
  &.required::after {
    content: '*';
    color: var(--danger-color);
    margin-left: 4px;
  }
}

.form-actions {
  display: flex;
  gap: 12px;
}

.level-hint {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1;
}

.results-section {
  margin-top: 24px;
}

.results-header {
  margin-bottom: 16px;
}

.results-count {
  color: var(--text-secondary);
  font-size: 14px;
  
  strong {
    color: var(--primary-color);
  }
}

.results-table {
  margin-bottom: 16px;
}

.results-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.empty-text {
  color: var(--text-muted);
  font-size: 14px;
}

.detail-content {
  .detail-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    margin-bottom: 20px;
  }
  
  .detail-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
    
    label {
      font-size: 12px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    
    span {
      color: var(--text-primary);
    }
  }
  
  .detail-value {
    label {
      display: block;
      font-size: 12px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: 8px;
    }
  }
  
  .value-code {
    background: var(--bg-elevated);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 16px;
    max-height: 300px;
    overflow: auto;
    font-size: 13px;
    line-height: 1.6;
    color: var(--text-secondary);
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.invalidate-content {
  .invalidate-tip {
    color: var(--text-secondary);
    margin-bottom: 12px;
  }
  
  .invalidate-key {
    background: var(--bg-elevated);
    border: 1px solid var(--border-color);
    border-radius: 6px;
    padding: 12px 16px;
    color: var(--danger-color);
  }
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
