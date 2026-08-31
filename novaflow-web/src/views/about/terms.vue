<template>
  <div class="about-body">
    <p class="doc-updated">生效日期：2026-08-31 · 版本 v1.0</p>

    <section v-for="section in sections" :key="section.title" class="doc-section">
      <h2>{{ section.title }}</h2>
      <p v-for="(para, idx) in section.paragraphs" :key="idx">{{ para }}</p>
      <ul v-if="section.list?.length">
        <li v-for="item in section.list" :key="item">{{ item }}</li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
const sections = [
  {
    title: '1. 服务说明',
    paragraphs: [
      'NovaFlow AI（以下简称「本平台」）是由部署方或平台运营方提供的企业级 AI Agent 开发与运行平台，包含 Agent 编排、工作流、知识库、模型接入、开放 API 等功能。',
      '您通过注册、登录或使用本平台，即表示已阅读并同意本协议。若您代表企业或组织使用本平台，则表明您已获得相应授权。',
    ],
  },
  {
    title: '2. 账号与安全',
    paragraphs: ['您应妥善保管账号与密码，对账号下的所有操作负责。'],
    list: [
      '不得将账号转让、出借或与他人共享；',
      '发现账号异常时，应及时联系管理员或平台运营方；',
      '私有化部署场景下，账号安全策略由部署方制定并执行。',
    ],
  },
  {
    title: '3. 合理使用',
    paragraphs: ['您承诺在使用本平台时遵守适用法律法规，不得利用平台从事以下行为：'],
    list: [
      '上传、传播违法、侵权或有害内容；',
      '绕过权限控制、滥用 API 或影响系统稳定性；',
      '未经授权访问他人数据或租户资源；',
      '将平台用于未经授权的自动化爬取、攻击或欺诈活动。',
    ],
  },
  {
    title: '4. 数据与内容',
    paragraphs: [
      '您在平台中创建的配置、知识库文档、对话内容等数据的权属，由您与部署方/运营方根据实际合同约定确定。',
      '平台可能为提供服务而处理上述数据，具体存储位置、留存周期与安全措施见「安全与隐私」说明。',
    ],
  },
  {
    title: '5. 服务变更与中断',
    paragraphs: [
      '平台运营方或部署方可根据业务需要调整功能、升级版本或暂停服务，并尽可能提前通知。',
      '因维护、不可抗力或第三方原因导致的服务中断，平台将在合理范围内协助恢复，但不承担超出法律规定的责任。',
    ],
  },
  {
    title: '6. 免责声明',
    paragraphs: [
      'AI 模型输出具有不确定性，平台不对模型生成内容的准确性、完整性或适用性作保证。您应在使用前进行人工审核，尤其在生产或面向终端用户的场景中。',
      '开放平台 API 与嵌入能力需由集成方自行评估安全风险并落实调用方隔离、密钥保管等措施。',
    ],
  },
  {
    title: '7. 协议更新',
    paragraphs: [
      '本协议可能随产品迭代更新，更新后将通过控制台「关于」页面或适当方式公布。若您继续使用本平台，即视为接受更新后的协议。',
      '如有疑问，请通过「联系我们」或「报告问题」与我们沟通。',
    ],
  },
]
</script>

<style scoped>
@import './doc-styles.css';
</style>
