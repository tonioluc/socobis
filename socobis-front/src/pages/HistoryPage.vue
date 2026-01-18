<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api, type FabricationHistorique } from '@/services/api'

const items = ref<FabricationHistorique[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const expandedId = ref<string | null>(null)

onMounted(async () => {
  await loadHistorique()
})

async function loadHistorique() {
  loading.value = true
  error.value = null
  try {
    items.value = await api.getHistoriqueFabrications()
  } catch (e: any) {
    error.value = e.message || 'Erreur lors du chargement de l\'historique'
    console.error('Erreur:', e)
  } finally {
    loading.value = false
  }
}

function toggleExpand(id: string) {
  expandedId.value = expandedId.value === id ? null : id
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function badgeClass(status: string) {
  switch (status?.toUpperCase()) {
    case 'TERMINE':
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
  switch (status?.toUpperCase()) {
    case 'TERMINE':
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

function formatType(type: string): string {
  switch (type) {
    case 'PRODUIT_FINI': return 'Produit Fini'
    case 'PRODUIT_INTERMEDIAIRE': return 'Intermédiaire'
    case 'MATIERE_PREMIERE': return 'Matière Première'
    default: return type
  }
}
</script>

<template>
  <div class="space-y-6">
    <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2 class="text-lg font-semibold text-slate-900">Historique de fabrication</h2>
          <p class="mt-1 text-sm text-slate-600">Liste des fabrications effectuées avec traçabilité complète.</p>
        </div>
        <div class="flex gap-2">
          <button
            @click="loadHistorique"
            :disabled="loading"
            class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm hover:bg-slate-50"
          >
            <i class="fas fa-sync-alt" :class="{ 'animate-spin': loading }"></i>
            Actualiser
          </button>
          <div class="rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-600">
            <i class="fas fa-database mr-2"></i>
            Source: Oracle DB
          </div>
        </div>
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="mt-5 flex items-center justify-center py-12">
        <div class="text-center">
          <i class="fas fa-spinner fa-spin text-3xl text-slate-400"></i>
          <p class="mt-3 text-sm text-slate-600">Chargement de l'historique...</p>
        </div>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="mt-5 rounded-xl border border-rose-200 bg-rose-50 p-4">
        <div class="flex items-start gap-3">
          <i class="fas fa-exclamation-circle mt-0.5 text-rose-500"></i>
          <div>
            <p class="font-medium text-rose-700">Erreur de chargement</p>
            <p class="mt-1 text-sm text-rose-600">{{ error }}</p>
            <button
              @click="loadHistorique"
              class="mt-3 inline-flex items-center gap-2 rounded-lg bg-rose-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-rose-700"
            >
              <i class="fas fa-redo"></i>
              Réessayer
            </button>
          </div>
        </div>
      </div>

      <!-- Data table -->
      <div v-else class="mt-5 overflow-hidden rounded-xl border border-slate-200">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th class="px-4 py-3 w-8"></th>
              <th class="px-4 py-3">Référence</th>
              <th class="px-4 py-3">Date</th>
              <th class="px-4 py-3">Produit</th>
              <th class="px-4 py-3">Quantité</th>
              <th class="px-4 py-3">Statut</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <template v-for="h in items" :key="h.id">
              <tr class="bg-white hover:bg-slate-50 cursor-pointer" @click="toggleExpand(h.id)">
                <td class="px-4 py-3">
                  <i :class="expandedId === h.id ? 'fas fa-chevron-down' : 'fas fa-chevron-right'" class="text-slate-400"></i>
                </td>
                <td class="px-4 py-3 font-medium text-slate-900">{{ h.id }}</td>
                <td class="px-4 py-3 text-slate-700">{{ formatDate(h.dateFabrication) }}</td>
                <td class="px-4 py-3 text-slate-700">
                  <div class="flex items-center gap-2">
                    <i class="fas fa-cookie-bite text-indigo-500"></i>
                    {{ h.produitLibelle || h.produitId }}
                  </div>
                </td>
                <td class="px-4 py-3 font-semibold text-slate-900">{{ h.quantite }} {{ h.unite }}</td>
                <td class="px-4 py-3">
                  <span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium" :class="badgeClass(h.statut)">
                    <i
                      :class="{
                        'fas fa-check mr-1': h.statut?.toUpperCase() === 'TERMINE' || h.statut?.toUpperCase() === 'TERMINEE',
                        'fas fa-spinner mr-1': h.statut?.toUpperCase() === 'EN_COURS',
                        'fas fa-times mr-1': h.statut?.toUpperCase() === 'ANNULEE'
                      }"
                    ></i>
                    {{ label(h.statut) }}
                  </span>
                </td>
              </tr>
              
              <!-- Lignes détail (expandable) -->
              <tr v-if="expandedId === h.id && h.lignes && h.lignes.length > 0">
                <td colspan="6" class="bg-slate-50 px-4 py-3">
                  <div class="ml-8">
                    <div class="text-xs font-semibold uppercase text-slate-500 mb-2">Ingrédients consommés</div>
                    <div class="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                      <div v-for="ligne in h.lignes" :key="ligne.ingredientId" class="flex items-center gap-2 rounded-lg bg-white px-3 py-2 ring-1 ring-slate-200">
                        <i :class="{
                          'fas fa-flask text-indigo-500': ligne.type === 'PRODUIT_INTERMEDIAIRE',
                          'fas fa-seedling text-emerald-500': ligne.type === 'MATIERE_PREMIERE',
                          'fas fa-box text-slate-500': !ligne.type
                        }"></i>
                        <div class="flex-1 min-w-0">
                          <div class="font-medium text-slate-900 truncate">{{ ligne.ingredientLibelle || ligne.ingredientId }}</div>
                          <div class="text-xs text-slate-500">{{ formatType(ligne.type) }}</div>
                        </div>
                        <div class="text-sm font-semibold text-rose-600">-{{ ligne.quantiteUtilisee }} {{ ligne.unite }}</div>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
            </template>

            <tr v-if="items.length === 0">
              <td class="px-4 py-6 text-center text-sm text-slate-500" colspan="6">
                <i class="fas fa-inbox text-3xl text-slate-300 mb-2"></i>
                <p>Aucun historique de fabrication.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Stats -->
      <div v-if="!loading && !error && items.length > 0" class="mt-4 flex gap-4 text-xs text-slate-500">
        <span>{{ items.length }} fabrication(s)</span>
      </div>
    </section>
  </div>
</template>
