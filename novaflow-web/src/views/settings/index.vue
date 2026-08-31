<template>
  <div class="settings-page page-shell">
    <div class="page-header">
      <div>
        <h1>系统设置</h1>
        <p>管理组织、权限、模型与平台配置入口</p>
      </div>
    </div>

    <div class="settings-grid">
      <router-link
        v-for="item in settingItems"
        :key="item.key"
        :to="item.path"
        class="setting-card page-card"
      >
        <component :is="item.icon" class="setting-icon" />
        <div class="setting-title">{{ item.title }}</div>
        <div class="setting-desc">{{ item.desc }}</div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ApartmentOutlined,
  ApiOutlined,
  BankOutlined,
  DatabaseOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'

const settingItems = [
  { key: 'org', title: '组织管理', desc: '成员、部门与租户信息', path: '/org', icon: ApartmentOutlined },
  { key: 'permission', title: '权限管理', desc: '角色与访问控制', path: '/permission', icon: SafetyCertificateOutlined },
  { key: 'model', title: '模型中心', desc: '模型接入与参数配置', path: '/model', icon: RobotOutlined },
  { key: 'billing', title: '账单与用量', desc: '费用统计与套餐信息', path: '/billing', icon: BankOutlined },
  { key: 'knowledge', title: '知识库 Hub', desc: '知识库与文档管理', path: '/knowledge', icon: DatabaseOutlined },
  { key: 'tool', title: '工具市场', desc: 'Skill 技能与 MCP 插件', path: '/tool', icon: ApiOutlined },
  { key: 'monitor', title: '运行监控', desc: '服务健康与调用指标', path: '/monitor', icon: SettingOutlined },
]
</script>

<style scoped>
.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.setting-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px;
  color: inherit;
  text-decoration: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.setting-card:hover {
  border-color: #91caff;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.08);
}

.setting-icon {
  font-size: 22px;
  color: #1677ff;
}

.setting-title {
  font-size: 16px;
  font-weight: 600;
}

.setting-desc {
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
