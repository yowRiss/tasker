import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/tasks' },
    { path: '/tasks', component: () => import('../views/TasksView.vue') },
    {
      path: '/tasks/:taskId',
      component: () => import('../views/TaskView.vue'),
    },
    {
      path: '/notes',
      component: () => import('../views/NotesView.vue'),
    },
    {
      path: '/notes/:noteId',
      component: () => import('../views/NoteView.vue'),
    },
    {
      path: '/search',
      component: () => import('../views/SearchView.vue'),
    },
    {
      path: '/money',
      component: () => import('../views/MoneyView.vue'),
    },
    {
      path: '/admin',
      component: () => import('../views/AdminView.vue'),
    },
  ],
})

export default router
