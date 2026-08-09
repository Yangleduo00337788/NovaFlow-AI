<template>
  <div class="dashboard-workflow-canvas">
    <div class="canvas-header">
      <div class="canvas-header-left">
        <div class="workflow-switcher" v-if="workflows.length > 1">
          <button type="button" class="switch-btn" :disabled="!canPrev" @click="switchPrev">
            <LeftOutlined />
          </button>
          <a-select
            v-model:value="selectedId"
            class="workflow-select"
            :options="workflowOptions"
            :field-names="{ label: 'workflowName', value: 'workflowId' }"
            @change="onSelectChange"
          />
          <button type="button" class="switch-btn" :disabled="!canNext" @click="switchNext">
            <RightOutlined />
          </button>
        </div>
        <router-link v-else :to="currentWorkflow.path" class="workflow-name">
          {{ currentWorkflow.workflowName }}
        </router-link>
        <div v-if="currentWorkflow.applicationName" class="workflow-meta">
          {{ currentWorkflow.applicationName }}
          <span v-if="currentWorkflow.updatedAt"> · 更新于 {{ currentWorkflow.updatedAt }}</span>
        </div>
      </div>
      <a-tag v-if="currentWorkflow.statusLabel" color="success" class="runtime-tag">
        {{ currentWorkflow.statusLabel }}
      </a-tag>
    </div>
    <WorkflowCanvasViewer :workflow-id="selectedId" compact class="canvas-body" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import { fetchPublishedWorkflows } from '@/api/dashboard'
import WorkflowCanvasViewer from '@/components/workflow/WorkflowCanvasViewer.vue'
import type { PublishedWorkflow, WorkflowRuntime } from '@/types/dashboard'

const props = defineProps<{
  runtime: WorkflowRuntime
}>()

const workflows = ref<PublishedWorkflow[]>([])
const selectedId = ref(props.runtime.workflowId)

const currentWorkflow = computed(() => {
  const matched = workflows.value.find((item) => item.workflowId === selectedId.value)
  if (matched) return matched
  return {
    workflowId: props.runtime.workflowId,
    workflowName: props.runtime.workflowName,
    applicationName: '',
    status: props.runtime.status,
    statusLabel: props.runtime.statusLabel,
    path: props.runtime.path,
    updatedAt: '',
  }
})

const workflowOptions = computed(() => workflows.value)
const currentIndex = computed(() => workflows.value.findIndex((item) => item.workflowId === selectedId.value))
const canPrev = computed(() => currentIndex.value > 0)
const canNext = computed(() => currentIndex.value >= 0 && currentIndex.value < workflows.value.length - 1)

watch(
  () => props.runtime.workflowId,
  (workflowId) => {
    if (workflowId) {
      selectedId.value = workflowId
    }
  },
)

async function loadWorkflows() {
  try {
    const res = await fetchPublishedWorkflows(20)
    workflows.value = res.data.data || []
    if (!workflows.value.length) {
      selectedId.value = props.runtime.workflowId
      return
    }
    if (!workflows.value.some((item) => item.workflowId === selectedId.value)) {
      selectedId.value = workflows.value[0].workflowId
    }
  } catch {
    workflows.value = []
    selectedId.value = props.runtime.workflowId
  }
}

function switchPrev() {
  if (!canPrev.value) return
  selectedId.value = workflows.value[currentIndex.value - 1].workflowId
}

function switchNext() {
  if (!canNext.value) return
  selectedId.value = workflows.value[currentIndex.value + 1].workflowId
}

function onSelectChange(value: number) {
  selectedId.value = value
}

onMounted(() => {
  loadWorkflows()
})
</script>

<style scoped>
.dashboard-workflow-canvas {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1 1 0;
  min-height: 0;
}

.canvas-body {
  flex: 1 1 0;
  min-height: 0;
  max-height: 100%;
}

.canvas-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  flex-shrink: 0;
}

.canvas-header-left {
  min-width: 0;
  flex: 1;
}

.workflow-switcher {
  display: flex;
  align-items: center;
  gap: 6px;
}

.workflow-select {
  min-width: 180px;
  max-width: 260px;
}

.switch-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--card-bg, #fff);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.switch-btn:hover:not(:disabled) {
  color: #1677ff;
  border-color: #91caff;
}

.switch-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.workflow-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.workflow-name:hover {
  color: #1677ff;
}

.workflow-meta {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.runtime-tag {
  margin: 0;
  font-size: 11px;
  flex-shrink: 0;
}
</style>
