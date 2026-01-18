<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { formatProductType, formatStockStatus, products } from '@/mock/fabrication'

const router = useRouter()

const query = ref('')
const onlyType = ref<'TOUS' | 'FINI' | 'INTERMEDIAIRE'>('TOUS')

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  return products
    .filter((p) => (onlyType.value === 'TOUS' ? true : p.type === onlyType.value))
    .filter((p) => (q ? p.name.toLowerCase().includes(q) : true))
})

function badgeClass(tone: 'ok' | 'warn' | 'bad') {
  switch (tone) {
    case 'ok':
      return 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'
    case 'warn':
      return 'bg-amber-50 text-amber-700 ring-1 ring-amber-200'
    case 'bad':
      return 'bg-rose-50 text-rose-700 ring-1 ring-rose-200'
  }
}

function goManufacture(productId: string) {
  router.push({ path: '/fabrication', query: { productId } })
}
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h2 class="text-lg font-semibold text-slate-900">Liste des produits</h2>
          <p class="mt-1 text-sm text-slate-600">
            Produits finis et intermédiaires avec stock disponible (données statiques).
          </p>
        </div>

        <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div class="relative">
            <i class="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"></i>
            <input
              v-model="query"
              class="w-full rounded-xl border border-slate-200 bg-white py-2 pl-10 pr-3 text-sm outline-none focus:border-slate-400"
              placeholder="Rechercher un produit..."
            />
          </div>

          <select
            v-model="onlyType"
            class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-slate-400"
          >
            <option value="TOUS">Tous</option>
            <option value="FINI">Produits finis</option>
            <option value="INTERMEDIAIRE">Intermédiaires</option>
          </select>
        </div>
      </div>

      <div class="mt-5 overflow-hidden rounded-xl border border-slate-200">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th class="px-4 py-3">Produit</th>
              <th class="px-4 py-3">Type</th>
              <th class="px-4 py-3">Stock</th>
              <th class="px-4 py-3">État</th>
              <th class="px-4 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-for="p in filtered" :key="p.id" class="bg-white">
              <td class="px-4 py-3">
                <div class="flex items-center gap-3">
                  <div
                    class="flex h-9 w-9 items-center justify-center rounded-xl"
                    :class="p.type === 'FINI' ? 'bg-indigo-50 text-indigo-700' : 'bg-slate-100 text-slate-700'"
                  >
                    <i :class="p.type === 'FINI' ? 'fas fa-cookie-bite' : 'fas fa-flask'"></i>
                  </div>
                  <div>
                    <div class="font-medium text-slate-900">{{ p.name }}</div>
                    <div class="text-xs text-slate-500">ID: {{ p.id }}</div>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3 text-slate-700">{{ formatProductType(p.type) }}</td>
              <td class="px-4 py-3 font-medium text-slate-900">{{ p.stock }} {{ p.unit }}</td>
              <td class="px-4 py-3">
                <span
                  class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium"
                  :class="badgeClass(formatStockStatus(p.stockStatus).tone)"
                >
                  {{ formatStockStatus(p.stockStatus).label }}
                </span>
              </td>
              <td class="px-4 py-3 text-right">
                <button
                  class="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-3 py-2 text-xs font-semibold text-white hover:bg-slate-800"
                  @click="goManufacture(p.id)"
                >
                  <i class="fas fa-cogs"></i>
                  Fabriquer
                </button>
              </td>
            </tr>

            <tr v-if="filtered.length === 0">
              <td class="px-4 py-6 text-center text-sm text-slate-500" colspan="5">
                Aucun produit ne correspond à votre recherche.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
