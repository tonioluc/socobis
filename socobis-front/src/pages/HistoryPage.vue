<script setup lang="ts">
import { computed } from 'vue'
import { history } from '@/mock/fabrication'

const items = computed(() => history)

function badgeClass(status: string) {
  switch (status) {
    case 'TERMINEE':
      return 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'
    case 'EN_COURS':
      return 'bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200'
    case 'ANNULEE':
      return 'bg-rose-50 text-rose-700 ring-1 ring-rose-200'
    default:
      return 'bg-slate-50 text-slate-700 ring-1 ring-slate-200'
  }
}

function label(status: string) {
  switch (status) {
    case 'TERMINEE':
      return 'Terminée'
    case 'EN_COURS':
      return 'En cours'
    case 'ANNULEE':
      return 'Annulée'
    default:
      return status
  }
}
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2 class="text-lg font-semibold text-slate-900">Historique de fabrication</h2>
          <p class="mt-1 text-sm text-slate-600">Liste mock des fabrications (à brancher sur Oracle plus tard).</p>
        </div>
        <div class="rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-600">
          <i class="fas fa-database mr-2"></i>
          Source: données statiques
        </div>
      </div>

      <div class="mt-5 overflow-hidden rounded-xl border border-slate-200">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th class="px-4 py-3">Référence</th>
              <th class="px-4 py-3">Date</th>
              <th class="px-4 py-3">Produit</th>
              <th class="px-4 py-3">Quantité</th>
              <th class="px-4 py-3">Statut</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-for="h in items" :key="h.id" class="bg-white">
              <td class="px-4 py-3 font-medium text-slate-900">{{ h.id }}</td>
              <td class="px-4 py-3 text-slate-700">{{ h.date }}</td>
              <td class="px-4 py-3 text-slate-700">{{ h.productName }}</td>
              <td class="px-4 py-3 font-semibold text-slate-900">{{ h.qtyProduced }} {{ h.unit }}</td>
              <td class="px-4 py-3">
                <span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium" :class="badgeClass(h.status)">
                  <i
                    :class="
                      h.status === 'TERMINEE'
                        ? 'fas fa-check mr-1'
                        : h.status === 'EN_COURS'
                          ? 'fas fa-spinner mr-1'
                          : 'fas fa-times mr-1'
                    "
                  ></i>
                  {{ label(h.status) }}
                </span>
              </td>
            </tr>

            <tr v-if="items.length === 0">
              <td class="px-4 py-6 text-center text-sm text-slate-500" colspan="5">Aucun historique.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
