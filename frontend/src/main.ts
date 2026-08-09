import { createApp } from 'vue'

import App from './app/App.vue'
import router from './app/router'
import './styles/global.css'

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then((registrations) => {
    for (const registration of registrations) {
      void registration.unregister()
    }
  })
}

createApp(App).use(router).mount('#app')
