import type { DashboardOverview } from '@/types/dashboard'

/** 工作台 Mock 数据（对齐 UI 原型） */
export const dashboardMock: DashboardOverview = {
  stats: [
    { key: 'apps', label: '应用总数', value: '32', change: '+12%', up: true },
    { key: 'agents', label: 'Agent 总数', value: '128', change: '+18%', up: true },
    { key: 'invocations', label: '调用次数', value: '1.2M', change: '+23%', up: true },
    { key: 'tokens', label: 'Token 消耗', value: '348.6M', change: '+16%', up: true },
    { key: 'cost', label: '成本（元）', value: '¥ 12,586.32', change: '-8%', up: false },
  ],
  recentItems: [
    { name: '智能客服 Agent', type: 'Agent', updatedAt: '2 小时前', path: '/agent' },
    { name: '合同审核工作流', type: '工作流', updatedAt: '5 小时前', path: '/workflow' },
    { name: '产品知识库', type: '知识库', updatedAt: '1 天前', path: '/knowledge' },
    { name: '数据分析 Agent', type: 'Agent', updatedAt: '2 天前', path: '/agent' },
    { name: '文档助手工作流', type: '工作流', updatedAt: '3 天前', path: '/workflow' },
  ],
  favoriteItems: [
    { name: '智能客服 Agent', type: 'Agent', updatedAt: '2 小时前', path: '/agent', favorite: true },
    { name: '产品知识库', type: '知识库', updatedAt: '1 天前', path: '/knowledge', favorite: true },
  ],
  recentLogs: [
    { name: '智能客服 Agent', status: '成功', success: true, time: '2 分钟前', duration: '2.3s', tokens: 1256 },
    { name: '合同审查工作流', status: '成功', success: true, time: '5 分钟前', duration: '4.8s', tokens: 3432 },
    { name: '财务报表解析', status: '失败', success: false, time: '15 分钟前', duration: '-', tokens: null },
    { name: '企业知识问答', status: '成功', success: true, time: '30 分钟前', duration: '1.6s', tokens: 987 },
    { name: '市场分析 Agent', status: '成功', success: true, time: '1 小时前', duration: '3.2s', tokens: 2145 },
  ],
  modelUsage: [
    { model: 'DeepSeek R1', percent: 42, tokens: '146.1M' },
    { model: 'Qwen 2.5', percent: 28, tokens: '97.6M' },
    { model: 'GPT-4o', percent: 18, tokens: '62.7M' },
    { model: 'Claude 3.5', percent: 12, tokens: '42.2M' },
  ],
  topApps: [
    { name: '智能客服 Agent', value: '128.6K', icon: 'robot', color: '#1677ff', iconBg: '#e8f3ff' },
    { name: '合同审查助手', value: '96.3K', icon: 'application', color: '#4f6ef7', iconBg: '#eef2ff' },
    { name: '财务分析 Agent', value: '72.1K', icon: 'agents', color: '#fa8c16', iconBg: '#fff7e6' },
    { name: '企业知识问答', value: '58.4K', icon: 'knowledge', color: '#9254de', iconBg: '#f9f0ff' },
    { name: '数据洞察工作流', value: '41.2K', icon: 'workflow', color: '#13c2c2', iconBg: '#e6fffb' },
  ],
  systemHealth: [
    { name: 'API 服务', status: '正常', healthy: true },
    { name: '向量数据库', status: '正常', healthy: true },
    { name: '消息队列', status: '正常', healthy: true },
    { name: '存储服务', status: '正常', healthy: true },
  ],
  trend: [
    { time: '00:00', value: 3200 },
    { time: '04:00', value: 1800 },
    { time: '08:00', value: 8600 },
    { time: '12:00', value: 15200 },
    { time: '16:00', value: 22532 },
    { time: '20:00', value: 12400 },
  ],
  quickActions: [
    { key: 'api-key', label: 'API Key 管理', path: '/settings' },
    { key: 'prompt', label: 'Prompt 模板', path: '/prompt' },
    { key: 'dataset', label: '数据集管理', path: '/knowledge' },
    { key: 'mcp', label: 'MCP 服务', path: '/tool' },
    { key: 'settings', label: '系统设置', path: '/settings' },
    { key: 'users', label: '用户管理', path: '/org' },
  ],
  quickStartTiles: [
    { key: 'agent', label: '创建 Agent', desc: '快速搭建对话助手', path: '/agent', color: '#1677ff' },
    { key: 'knowledge', label: '上传知识库', desc: '导入文档启用 RAG', path: '/knowledge', color: '#9254de' },
    { key: 'workflow', label: '编排工作流', desc: '可视化流程编排', path: '/workflow', color: '#13c2c2' },
  ],
  planInfo: {
    planType: '企业版',
    expireAt: '2028-12-31',
    usedPercent: 68,
  },
}

/** 指标卡迷你折线（原型右侧小趋势图） */
export const statSparklines: Record<string, number[]> = {
  apps: [8, 10, 12, 14, 16, 18, 20],
  agents: [12, 18, 14, 22, 28, 24, 32],
  invocations: [8, 12, 16, 14, 20, 24, 28],
  tokens: [10, 12, 14, 18, 20, 22, 26],
  cost: [22, 20, 18, 16, 14, 12, 10],
}
