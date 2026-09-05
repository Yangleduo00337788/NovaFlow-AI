import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import { applyTheme, applyScope, readStoredTheme } from './stores/theme'
import { IS_PLATFORM_DEPLOY } from './config/deploy'
import 'ant-design-vue/dist/reset.css'
import './styles/global.css'
import './styles/auth.css'
import './styles/platform-theme.css'

applyTheme(readStoredTheme())
applyScope(IS_PLATFORM_DEPLOY ? 'platform' : 'tenant')

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Antd)
app.mount('#app')
