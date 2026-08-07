import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import { applyTheme, readStoredTheme } from './stores/theme'
import 'ant-design-vue/dist/reset.css'
import './styles/global.css'

applyTheme(readStoredTheme())

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Antd)
app.mount('#app')
