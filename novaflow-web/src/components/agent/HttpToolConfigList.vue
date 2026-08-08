<template>
  <div class="tool-list">
    <div v-for="(tool, index) in tools" :key="index" class="tool-item">
      <a-row :gutter="8">
        <a-col :span="6">
          <a-input v-model:value="tool.name" placeholder="工具名，如 get_weather" />
        </a-col>
        <a-col :span="4">
          <a-select v-model:value="tool.method" :options="httpMethodOptions" />
        </a-col>
        <a-col :span="14">
          <a-input v-model:value="tool.url" placeholder="https://api.example.com?q={{city}}" />
        </a-col>
      </a-row>
      <a-textarea
        v-model:value="tool.description"
        :rows="2"
        placeholder="工具描述，帮助模型判断何时调用"
        class="tool-field"
      />
      <a-collapse ghost>
        <a-collapse-panel key="advanced" header="高级配置（Headers / Body / 参数 Schema）">
          <div class="advanced-block">
            <div class="advanced-label">请求头</div>
            <div v-for="(row, rowIndex) in tool.headerRows" :key="rowIndex" class="header-row">
              <a-input v-model:value="row.key" placeholder="Header 名" />
              <a-input v-model:value="row.value" placeholder="Header 值，支持占位符如 token" />
              <a-button type="link" danger @click="removeHeaderRow(tool, rowIndex)">删除</a-button>
            </div>
            <a-button type="dashed" size="small" @click="addHeaderRow(tool)">添加 Header</a-button>
          </div>
          <div v-if="isBodyMethod(tool.method)" class="advanced-block">
            <div class="advanced-label">Body 模板（JSON，支持占位符）</div>
            <a-textarea
              v-model:value="tool.bodyTemplate"
              :rows="4"
              placeholder='{"city":"{{city}}"}；留空则自动将模型参数序列化为 JSON'
            />
          </div>
          <div class="advanced-block">
            <div class="advanced-label">参数 Schema（OpenAI JSON Schema）</div>
            <a-textarea
              v-model:value="tool.inputSchemaJson"
              :rows="6"
              placeholder='{"type":"object","properties":{"city":{"type":"string"}},"required":["city"]}'
            />
          </div>
        </a-collapse-panel>
      </a-collapse>
      <a-button type="link" danger class="tool-remove" @click="removeTool(index)">删除工具</a-button>
    </div>
    <a-button type="dashed" block @click="addTool">添加 HTTP 工具</a-button>
  </div>
</template>

<script setup lang="ts">
import type { ToolFormItem } from '@/utils/toolForm'
import { createToolFormItem } from '@/utils/toolForm'

const tools = defineModel<ToolFormItem[]>('tools', { required: true })

const httpMethodOptions = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'PATCH', label: 'PATCH' },
]

function createTool(): ToolFormItem {
  return createToolFormItem()
}

function isBodyMethod(method?: string) {
  const normalized = (method || 'GET').toUpperCase()
  return normalized === 'POST' || normalized === 'PUT' || normalized === 'PATCH'
}

function addHeaderRow(tool: ToolFormItem) {
  tool.headerRows.push({ key: '', value: '' })
}

function removeHeaderRow(tool: ToolFormItem, index: number) {
  tool.headerRows.splice(index, 1)
}

function addTool() {
  tools.value.push(createTool())
}

function removeTool(index: number) {
  tools.value.splice(index, 1)
}
</script>

<style scoped>
.tool-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-item {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
}

.tool-field {
  margin-top: 8px;
}

.advanced-block {
  margin-bottom: 12px;
}

.advanced-label {
  margin-bottom: 6px;
  font-size: 13px;
  color: #64748b;
}

.header-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 8px;
  margin-bottom: 8px;
}

.tool-remove {
  padding-left: 0;
}
</style>
