import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/observer',
    children: [
      {
        path: 'observer',
        name: 'Observer',
        component: () => import('@/views/Observer/index.vue'),
        meta: {
          title: 'nav.observer'
        }
      },
      {
        path: 'aggregator',
        name: 'Aggregator',
        component: () => import('@/views/Aggregator/index.vue'),
        meta: {
          title: 'nav.aggregator'
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

