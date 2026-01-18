import { createRouter, createWebHistory } from 'vue-router'

import ProductsPage from '@/pages/ProductsPage.vue'
import ManufacturePage from '@/pages/ManufacturePage.vue'
import HistoryPage from '@/pages/HistoryPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/produits' },
    { path: '/produits', component: ProductsPage },
    { path: '/fabrication', component: ManufacturePage },
    { path: '/historique', component: HistoryPage },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/produits',
    },
  ],
})

export default router
