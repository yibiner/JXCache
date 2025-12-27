<template>
  <div class="aggregator-page">
    <div class="config-card">
      <div class="config-header">
        <span class="config-title">{{ $t('aggregator.serverConfig') }}</span>
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
          :placeholder="$t('aggregator.serverUrlPlaceholder')"
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

    <div class="content-grid">
      <div class="query-panel">
        <div class="panel-header">
          <span class="panel-title">{{ $t('aggregator.queryParams') }}</span>
        </div>
        <div class="panel-body">
          <div class="form-group">
            <label class="form-label required">{{ $t('aggregator.serviceName') }}</label>
            <div class="input-with-action">
              <el-input
                v-model="queryForm.serviceName"
                :placeholder="$t('aggregator.serviceNamePlaceholder')"
                clearable
              />
              <el-button 
                type="primary" 
                @click="handleLoadNodes" 
                :loading="loadingNodes"
                :disabled="!queryForm.serviceName"
              >
                {{ $t('aggregator.loadNodes') }}
              </el-button>
            </div>
          </div>

          <div class="form-group" v-if="nodes.length > 0">
            <label class="form-label">{{ $t('aggregator.selectNodes') }}</label>
            <el-select
              v-model="queryForm.targets"
              :placeholder="$t('aggregator.allNodes')"
              multiple
              clearable
              filterable
              collapse-tags
              collapse-tags-tooltip
              class="full-width"
            >
              <el-option
                v-for="node in nodes"
                :key="node.nodeId"
                :label="`${node.nodeId} (${node.host}:${node.port})`"
                :value="node.nodeId"
              >
                <div class="node-option">
                  <span class="node-id">{{ node.nodeId }}</span>
                  <span class="node-address">{{ node.host }}:{{ node.port }}</span>
                  <el-tag v-if="node.healthy" type="success" size="small">OK</el-tag>
                  <el-tag v-else type="danger" size="small">Down</el-tag>
                </div>
              </el-option>
            </el-select>
            <div class="node-count">
              {{ $t('aggregator.nodesLoaded', { count: nodes.length }) }}
            </div>
          </div>

          <el-divider />

          <div class="form-group">
            <label class="form-label required">{{ $t('aggregator.area') }}</label>
            <el-input
              v-model="queryForm.area"
              placeholder="default"
              clearable
            />
          </div>

          <div class="form-group">
            <label class="form-label required">{{ $t('aggregator.cacheName') }}</label>
            <el-input
              v-model="queryForm.cacheName"
              :placeholder="$t('aggregator.cacheNamePlaceholder')"
              clearable
            />
          </div>

          <div class="form-group">
            <label class="form-label">{{ $t('aggregator.key') }}</label>
            <el-input
              v-model="queryForm.key"
              :placeholder="$t('aggregator.keyPlaceholder')"
              clearable
            />
          </div>

          <div class="action-buttons">
            <el-button 
              type="primary" 
              @click="handleQuery" 
              :loading="loading"
              :disabled="!isConnected || !canQuery"
            >
              <el-icon><Search /></el-icon>
              {{ $t('aggregator.query') }}
            </el-button>
            <el-button 
              type="success" 
              @click="handleConsistencyCheck" 
              :loading="loading"
              :disabled="!isConnected || !canConsistencyCheck"
            >
              <el-icon><Check /></el-icon>
              {{ $t('aggregator.consistencyCheck') }}
            </el-button>
            <el-button 
              type="danger" 
              @click="handleInvalidate" 
              :loading="loading"
              :disabled="!isConnected || !canInvalidate"
            >
              <el-icon><Delete /></el-icon>
              {{ $t('aggregator.invalidate') }}
            </el-button>
          </div>
        </div>
      </div>

      <div class="results-panel">
        <div class="panel-header">
          <span class="panel-title">{{ $t('aggregator.queryResults') }}</span>
        </div>
        <div class="panel-body">
          <div v-if="results.length > 0" class="query-results">
            <el-tabs v-model="activeTab" type="border-card">
              <el-tab-pane
                v-for="result in results"
                :key="result.nodeId"
                :label="result.nodeId"
                :name="result.nodeId"
              >
                <div class="tab-header">
                  <span class="entry-count">
                    {{ $t('observer.total') }}: <strong>{{ result.entries?.length || 0 }}</strong>
                  </span>
                </div>
                <el-table :data="result.entries" stripe class="results-table" max-height="400">
                  <el-table-column prop="key" :label="$t('aggregator.key')" min-width="200">
                    <template #default="{ row }">
                      <span class="mono">{{ row.key }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="valueType" :label="$t('observer.valueType')" width="120">
                    <template #default="{ row }">
                      <el-tag size="small">{{ row.valueType }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="valuePreview" :label="$t('observer.valuePreview')" min-width="250" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span class="mono text-secondary">{{ row.valuePreview }}</span>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>

          <div v-else-if="consistencyResult" class="consistency-results">
            <el-alert
              :type="consistencyResult.consistent ? 'success' : 'warning'"
              :title="consistencyResult.consistent ? $t('aggregator.allConsistent') : $t('aggregator.hasInconsistent')"
              :closable="false"
              show-icon
              class="mb-16"
            />
            
            <el-table :data="consistencyResult.nodes" stripe>
              <el-table-column prop="nodeId" :label="$t('aggregator.nodeId')" width="200">
                <template #default="{ row }">
                  <span class="mono">{{ row.nodeId }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('aggregator.status')" width="120">
                <template #default="{ row }">
                  <el-tag v-if="row.exists" type="success">{{ $t('aggregator.exists') }}</el-tag>
                  <el-tag v-else type="danger">{{ $t('aggregator.missing') }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="value" :label="$t('aggregator.value')" min-width="300" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="mono text-secondary">{{ row.value || '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-else class="empty-state">
            <el-icon class="empty-icon"><Connection /></el-icon>
            <p class="empty-text">{{ $t('aggregator.emptyTip') }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Link, Check, Delete, Connection } from '@element-plus/icons-vue'
import { createAggregatorApi } from '@/api/aggregator'
import { getAggregatorUrl, setAggregatorUrl } from '@/utils/api-config'
import type { ServiceInstance, ConsistencyResult, AggregateResult } from '@/api/types'

const { t } = useI18n()

// 页面状态
const serverUrl = ref('')
const isConnected = ref(false)
const connecting = ref(false)
const loading = ref(false)
const loadingNodes = ref(false)
const nodes = ref<ServiceInstance[]>([])
const results = ref<any[]>([])
const consistencyResult = ref<ConsistencyResult | null>(null)
const activeTab = ref('')
const initialized = ref(false)

// API 实例
let api = createAggregatorApi(getAggregatorUrl())

const queryForm = reactive({
  serviceName: '',
  area: 'default',
  cacheName: '',
  key: '',
  targets: [] as string[]
})

// 计算属性
const canQuery = computed(() => {
  return queryForm.serviceName && queryForm.area && queryForm.cacheName
})

const canConsistencyCheck = computed(() => {
  return queryForm.serviceName && queryForm.area && queryForm.cacheName && queryForm.key
})

const canInvalidate = computed(() => {
  return queryForm.serviceName && queryForm.area && queryForm.cacheName && queryForm.key
})

onMounted(() => {
  // 仅在首次挂载时初始化并自动连接
  if (initialized.value) return
  initialized.value = true
  
  serverUrl.value = getAggregatorUrl()
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
    setAggregatorUrl(serverUrl.value)
    api = createAggregatorApi(serverUrl.value)
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

const handleLoadNodes = async () => {
  if (!queryForm.serviceName) {
    ElMessage.warning(t('aggregator.serviceNameRequired'))
    return
  }

  loadingNodes.value = true
  try {
    nodes.value = await api.getNodes(queryForm.serviceName)
    ElMessage.success(t('aggregator.nodesLoadedMsg', { count: nodes.value.length }))
  } catch (error) {
    ElMessage.error(t('common.error'))
  } finally {
    loadingNodes.value = false
  }
}

const handleQuery = async () => {
  if (!canQuery.value) {
    ElMessage.warning(t('aggregator.fillRequired'))
    return
  }

  loading.value = true
  consistencyResult.value = null
  try {
    const result: AggregateResult = await api.query(
      queryForm.serviceName,
      {
        area: queryForm.area,
        cacheName: queryForm.cacheName,
        keyPrefix: queryForm.key || undefined,
        pageRequest: { pageNo: 1, pageSize: 100 }
      },
      queryForm.targets.length > 0 ? queryForm.targets : undefined
    )

    results.value = result.results.map(r => ({
      nodeId: r.nodeId,
      entries: r.entries
    }))

    if (results.value.length > 0) {
      activeTab.value = results.value[0].nodeId
    }
  } catch (error) {
    ElMessage.error(t('common.error'))
  } finally {
    loading.value = false
  }
}

const handleConsistencyCheck = async () => {
  if (!canConsistencyCheck.value) {
    ElMessage.warning(t('aggregator.keyRequired'))
    return
  }

  loading.value = true
  results.value = []
  try {
    consistencyResult.value = await api.checkConsistency(
      queryForm.serviceName,
      queryForm.area,
      queryForm.cacheName,
      queryForm.key,
      queryForm.targets.length > 0 ? queryForm.targets : undefined
    )
  } catch (error) {
    ElMessage.error(t('common.error'))
  } finally {
    loading.value = false
  }
}

const handleInvalidate = async () => {
  if (!canInvalidate.value) {
    ElMessage.warning(t('aggregator.keyRequired'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('aggregator.invalidateConfirm'),
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )

    loading.value = true
    try {
      await api.invalidate(
        queryForm.serviceName,
        {
          area: queryForm.area,
          cacheName: queryForm.cacheName,
          key: queryForm.key
        },
        queryForm.targets.length > 0 ? queryForm.targets : undefined
      )
      ElMessage.success(t('common.success'))
      // 刷新查询结果
      handleQuery()
    } catch (error) {
      ElMessage.error(t('common.error'))
    } finally {
      loading.value = false
    }
  } catch {
    // 用户取消操作
  }
}
</script>

<style scoped lang="scss">
.aggregator-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1600px;
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

.content-grid {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 20px;
}

.query-panel,
.results-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.panel-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
  
  &.required::after {
    content: '*';
    color: var(--danger-color);
    margin-left: 4px;
  }
}

.input-with-action {
  display: flex;
  gap: 8px;
}

.full-width {
  width: 100%;
}

.node-count {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.node-option {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .node-id {
    font-weight: 500;
  }
  
  .node-address {
    color: var(--text-muted);
    font-size: 12px;
  }
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
  
  .el-button {
    width: 100%;
    justify-content: center;
  }
}

.query-results {
  .tab-header {
    margin-bottom: 12px;
  }
  
  .entry-count {
    font-size: 13px;
    color: var(--text-secondary);
    
    strong {
      color: var(--primary-color);
    }
  }
}

.consistency-results {
  // 样式沿用全局定义
}

.results-table {
  :deep(.el-table__header-wrapper) {
    th {
      background: var(--bg-elevated);
    }
  }
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 56px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.empty-text {
  color: var(--text-muted);
  font-size: 14px;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
