<template>
  <div class="settings-page page-shell">
    <div class="page-header">
      <div>
        <h1>系统设置</h1>
        <p>管理组织、权限、模型与平台配置入口</p>
      </div>
    </div>

    <div class="settings-panel page-card">
      <section v-for="group in settingGroups" :key="group.title" class="settings-group">
        <h3 class="group-title">{{ group.title }}</h3>
        <div class="settings-grid">
          <router-link
            v-for="item in group.items"
            :key="item.key"
            :to="item.path"
            class="setting-card"
          >
            <component :is="item.icon" class="setting-icon" />
            <div class="setting-text">
              <div class="setting-title">{{ item.title }}</div>
              <div class="setting-desc">{{ item.desc }}</div>
            </div>
            <RightOutlined class="setting-arrow" />
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ApartmentOutlined,
  ApiOutlined,
  BankOutlined,
  DatabaseOutlined,
  RightOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'

const settingGroups = [
  {
    title: '组织与权限',
    items: [
      { key: 'org', title: '组织管理', desc: '成员、部门与租户信息', path: '/org', icon: ApartmentOutlined },
      { key: 'permission', title: '权限管理', desc: '角色与访问控制', path: '/permission', icon: SafetyCertificateOutlined },
      { key: 'billing', title: '账单与用量', desc: '费用统计与套餐信息', path: '/billing', icon: BankOutlined },
    ],
  },
  {
    title: 'AI 资源',
    items: [
      { key: 'model', title: '模型中心', desc: '模型接入与参数配置', path: '/model', icon: RobotOutlined },
      { key: 'knowledge', title: '知识库 Hub', desc: '知识库与文档管理', path: '/knowledge', icon: DatabaseOutlined },
      { key: 'tool', title: '工具市场', desc: 'Skill 技能与 MCP 插件', path: '/tool', icon: ApiOutlined },
    ],
  },
  {
    title: '运维监控',
    items: [
      { key: 'monitor', title: '运行监控', desc: '服务健康与调用指标', path: '/monitor', icon: SettingOutlined },
    ],
  },
]
</script>

<style scoped>
.settings-page {
  min-height: auto;
}

.settings-panel {
  padding: 8px 4px;
}

.settings-group + .settings-group {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.group-title {
  margin: 0 0 10px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 0 8px 8px;
}

.setting-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid transparent;
  color: inherit;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s;
}

.setting-card:hover {
  background: var(--bg-subtle);
  border-color: var(--border);
}

.setting-icon {
  font-size: 20px;
  color: #1677ff;
  flex-shrink: 0;
}

.setting-text {
  flex: 1;
  min-width: 0;
}

.setting-title {
  font-size: 14px;
  font-weight: 600;
}

.setting-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.setting-arrow {
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
}

.setting-card:hover .setting-arrow {
  opacity: 1;
}

@media (max-width: 1100px) {
  .settings-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
