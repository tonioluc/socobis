<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { api, type Fabrication, type Page, FABRICATION_ETATS, FABRICATION_ETATS_LABELS } from '@/services/api'

const items = ref<Fabrication[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const expandedId = ref<string | null>(null)

// Pagination
const currentPage = ref(0)
const pageSize = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

// Filtres - Correspond aux vues SQL SOCOBIS
const dateMin = ref<string>('')
const dateMax = ref<string>('')
const etatFilter = ref<string>('FABRICATIONCPL') // Tous par défaut

// Options d'état basées sur SOCOBIS
const etatOptions = [
  { value: 'FABRICATIONCPL', label: 'Tous' },
  { value: 'FABRICATIONCPLCREE', label: 'Créée(s)' },
  { value: 'FABRICATIONCPLVISEE', label: 'Validée(s)' },
  { value: 'FABRICATIONCPLENTAMEE', label: 'Entamée(s)' },
  { value: 'FABRICATIONCPLBLOQUEE', label: 'Bloquée(s)' },
  { value: 'FABRICATIONCPLBTERMINEE', label: 'Terminée(s)' },
  { value: 'FABRICATIONCPLANNULE', label: 'Annulée(s)' }
]

// Computed
const canGoPrevious = computed(() => currentPage.value > 0)
const canGoNext = computed(() => currentPage.value < totalPages.value - 1)
const displayedRange = computed(() => {
  const start = currentPage.value * pageSize.value + 1
  const end = Math.min((currentPage.value + 1) * pageSize.value, totalElements.value)
  return { start, end }
})

onMounted(async () => {
  await loadHistorique()
})

// Watch les filtres pour recharger automatiquement
watch([dateMin, dateMax, etatFilter], () => {
  currentPage.value = 0 // Reset à la première page
  loadHistorique()
})

async function loadHistorique() {
  loading.value = true
  error.value = null
  try {
    const result = await api.getHistoriqueFabricationsPage({
      etat: etatFilter.value,
      dateMin: dateMin.value || undefined,
      dateMax: dateMax.value || undefined,
      page: currentPage.value,
      size: pageSize.value
    })
    items.value = result.content
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages
  } catch (e: any) {
    error.value = e.message || 'Erreur lors du chargement de l historique'
    console.error('Erreur:', e)
  } finally {
    loading.value = false
  }
}

function goToPage(page: number) {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    loadHistorique()
  }
}

function previousPage() {
  if (canGoPrevious.value) {
    goToPage(currentPage.value - 1)
  }
}

function nextPage() {
  if (canGoNext.value) {
    goToPage(currentPage.value + 1)
  }
}

function clearFilters() {
  dateMin.value = ''
  dateMax.value = ''
  etatFilter.value = 'FABRICATIONCPL'
  currentPage.value = 0
  loadHistorique()
}

function toggleExpand(id: string) {
  expandedId.value = expandedId.value === id ? null : id
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

// Obtenir le libellé de l etat
function getEtatLabel(etat: number): string {
  return FABRICATION_ETATS_LABELS[etat] || 'INCONNU'
}

// Classes CSS pour les badges d etat
function badgeClass(etat: number) {
  switch (etat) {
    case FABRICATION_ETATS.TERMINE:
      return 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'
    case FABRICATION_ETATS.ENTAME:
      return 'bg-indigo-50 text-indigo-700 ring-1 ring-indigo-200'
    case FABRICATION_ETATS.BLOQUE:
      return 'bg-amber-50 text-amber-700 ring-1 ring-amber-200'
    case FABRICATION_ETATS.VALIDE:
      return 'bg-sky-50 text-sky-700 ring-1 ring-sky-200'
    case FABRICATION_ETATS.CREE:
    default:
      return 'bg-slate-50 text-slate-700 ring-1 ring-slate-200'
  }
}

function statusIcon(etat: number) {
  switch (etat) {
    case FABRICATION_ETATS.TERMINE:
      return 'fas fa-check-circle'
    case FABRICATION_ETATS.ENTAME:
      return 'fas fa-play-circle'
    case FABRICATION_ETATS.BLOQUE:
      return 'fas fa-pause-circle'
    case FABRICATION_ETATS.VALIDE:
      return 'fas fa-check'
    case FABRICATION_ETATS.CREE:
    default:
      return 'fas fa-file'
  }
}

// Actions sur fabrication
async function validerFabrication(id: string) {
  try {
    await api.validerFabrication(id)
    await loadHistorique()
  } catch (e: any) {
    alert('Erreur: ' + e.message)
  }
}

async function entamerFabrication(id: string) {
  try {
    await api.entamerFabrication(id)
    await loadHistorique()
  } catch (e: any) {
    alert('Erreur: ' + e.message)
  }
}

async function bloquerFabrication(id: string) {
  try {
    await api.bloquerFabrication(id)
    await loadHistorique()
  } catch (e: any) {
    alert('Erreur: ' + e.message)
  }
}

async function debloquerFabrication(id: string) {
  try {
    await api.debloquerFabrication(id)
    await loadHistorique()
  } catch (e: any) {
    alert('Erreur: ' + e.message)
  }
}

async function terminerFabrication(id: string) {
  try {
    await api.terminerFabrication(id)
    await loadHistorique()
  } catch (e: any) {
    alert('Erreur: ' + e.message)
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Filtre de recherche (style SOCOBIS) -->
    <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-base font-medium text-slate-700">Filtre de recherche</h3>
        <button class="text-slate-400 hover:text-slate-600">
          <i class="fas fa-filter"></i>
        </button>
      </div>
      
      <div class="flex flex-wrap items-end gap-4">
        <!-- Filtre État (basé sur les vues SQL SOCOBIS) -->
        <div class="flex items-center gap-2">
          <label class="text-sm text-slate-600">État</label>
          <select 
            v-model="etatFilter"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          >
            <option v-for="opt in etatOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>

        <!-- Date min -->
        <div>
          <label class="block text-xs font-medium text-slate-600 mb-1">Date minimum</label>
          <input
            type="date"
            v-model="dateMin"
            class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          />
        </div>

        <!-- Date max -->
        <div>
          <label class="block text-xs font-medium text-slate-600 mb-1">Date maximum</label>
          <input
            type="date"
            v-model="dateMax"
            class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          />
        </div>

        <!-- Bouton effacer -->
        <button
          v-if="dateMin || dateMax || etatFilter !== 'Tous'"
          @click="clearFilters"
          class="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600 hover:bg-slate-100"
        >
          <i class="fas fa-times"></i>
          Effacer
        </button>
      </div>
    </section>

    <!-- Récapitulation -->
    <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 class="text-base font-medium text-slate-700 mb-4">Récapitulation</h3>
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-slate-200">
            <th class="py-2 text-left text-slate-500"></th>
            <th class="py-2 text-right text-slate-700 font-semibold">Nombre</th>
          </tr>
        </thead>
        <tbody>
          <tr class="border-b border-slate-100">
            <td class="py-2 text-slate-600">Total</td>
            <td class="py-2 text-right font-semibold text-slate-900">{{ totalElements }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- Liste des fabrications -->
    <section class="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
      <!-- Loading state -->
      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="text-center">
          <i class="fas fa-spinner fa-spin text-3xl text-slate-400"></i>
          <p class="mt-3 text-sm text-slate-600">Chargement de l historique...</p>
        </div>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="p-5">
        <div class="rounded-xl border border-rose-200 bg-rose-50 p-4">
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
      </div>

      <!-- Data table (structure SOCOBIS) -->
      <div v-else class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-50 text-xs text-slate-600 border-b border-slate-200">
            <tr>
              <th class="px-4 py-3 font-medium">
                ID <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 font-medium">
                Lancée Par <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 font-medium">
                Cible <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 font-medium">
                Remarque <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 font-medium">
                Désignation <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 font-medium">
                Date <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 font-medium">
                Id Ordre De Fabrication <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 font-medium">
                Id Ordre De Fabrication Fille <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 font-medium">
                État <i class="fas fa-sort text-slate-400 ml-1"></i>
              </th>
              <th class="px-4 py-3 w-10"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <template v-for="fab in items" :key="fab.id">
              <tr class="bg-white hover:bg-slate-50 cursor-pointer" @click="toggleExpand(fab.id)">
                <td class="px-4 py-3">
                  <span class="text-indigo-600 font-medium hover:underline">+ {{ fab.id }}</span>
                </td>
                <td class="px-4 py-3 text-slate-700">{{ fab.lanceParLibelle || fab.lancePar || '-' }}</td>
                <td class="px-4 py-3 text-slate-700">{{ fab.cibleLibelle || fab.cible || '-' }}</td>
                <td class="px-4 py-3 text-slate-600">{{ fab.remarque || '-' }}</td>
                <td class="px-4 py-3 text-slate-700">{{ fab.libelle || '-' }}</td>
                <td class="px-4 py-3 text-slate-700">{{ formatDate(fab.daty || '') }}</td>
                <td class="px-4 py-3">
                  <span v-if="fab.idOf" class="text-indigo-600 hover:underline">{{ fab.idOf }}</span>
                  <span v-else class="text-slate-400">-</span>
                </td>
                <td class="px-4 py-3">
                  <span v-if="fab.idOfFille" class="text-indigo-600 hover:underline">{{ fab.idOfFille }}</span>
                  <span v-else class="text-slate-400">-</span>
                </td>
                <td class="px-4 py-3">
                  <span 
                    class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium" 
                    :class="badgeClass(fab.etat)"
                  >
                    {{ getEtatLabel(fab.etat) }}
                  </span>
                </td>
                <td class="px-4 py-3" @click.stop>
                  <button class="text-slate-400 hover:text-slate-600">
                    <i class="fas fa-ellipsis-v"></i>
                  </button>
                </td>
              </tr>
              
              <!-- Détails expandables -->
              <tr v-if="expandedId === fab.id">
                <td colspan="10" class="bg-slate-50 px-4 py-4">
                  <div class="rounded-xl bg-white p-4 shadow-sm border border-slate-200">
                    <h4 class="text-sm font-semibold text-slate-700 mb-3">
                      <i class="fas fa-info-circle text-indigo-500 mr-2"></i>
                      Fiche de Fabrication - {{ fab.id }}
                    </h4>
                    
                    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span class="text-xs text-slate-500 block">ID</span>
                        <span class="font-medium text-indigo-600">{{ fab.id }}</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Remarque</span>
                        <span class="text-slate-700">{{ fab.remarque || '-' }}</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Désignation</span>
                        <span class="text-slate-700">{{ fab.libelle || '-' }}</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Date De Besoin</span>
                        <span class="text-slate-700">{{ formatDate(fab.dateBesoinn || '') }}</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Date</span>
                        <span class="text-slate-700">{{ formatDate(fab.daty || '') }}</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Ordre De Fabrication Fille Associé</span>
                        <span v-if="fab.idOfFille" class="text-indigo-600">{{ fab.idOfFille }}</span>
                        <span v-else class="text-slate-400">-</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Ordre De Fabrication Associé</span>
                        <span v-if="fab.idOf" class="text-indigo-600">{{ fab.idOf }}</span>
                        <span v-else class="text-slate-400">-</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Lancée Par</span>
                        <span class="text-slate-700">{{ fab.lanceParLibelle || fab.lancePar || '-' }}</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Cible</span>
                        <span class="text-slate-700">{{ fab.cibleLibelle || fab.cible || '-' }}</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">ÉTAT</span>
                        <span 
                          class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium" 
                          :class="badgeClass(fab.etat)"
                        >
                          {{ getEtatLabel(fab.etat) }}
                        </span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Fabrication Précédente</span>
                        <span v-if="fab.fabricationPrec" class="text-indigo-600">{{ fab.fabricationPrec }}</span>
                        <span v-else class="text-slate-400">-</span>
                      </div>
                      <div>
                        <span class="text-xs text-slate-500 block">Fabrication Suivant</span>
                        <span v-if="fab.fabricationSuiv" class="text-slate-700">{{ fab.fabricationSuiv }}</span>
                        <span v-else class="text-slate-400">-</span>
                      </div>
                    </div>

                    <!-- Actions -->
                    <div class="mt-4 flex flex-wrap gap-2">
                      <button
                        v-if="fab.etat === FABRICATION_ETATS.CREE"
                        @click.stop="validerFabrication(fab.id)"
                        class="rounded-lg bg-sky-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-sky-700"
                      >
                        Valider
                      </button>
                      <button
                        v-if="fab.etat === FABRICATION_ETATS.VALIDE"
                        @click.stop="entamerFabrication(fab.id)"
                        class="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-700"
                      >
                        (Re)Entamer
                      </button>
                      <button
                        v-if="fab.etat === FABRICATION_ETATS.ENTAME"
                        @click.stop="bloquerFabrication(fab.id)"
                        class="rounded-lg bg-amber-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-amber-700"
                      >
                        Bloquer
                      </button>
                      <button
                        v-if="fab.etat === FABRICATION_ETATS.BLOQUE"
                        @click.stop="debloquerFabrication(fab.id)"
                        class="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-indigo-700"
                      >
                        Débloquer
                      </button>
                      <button
                        v-if="fab.etat === FABRICATION_ETATS.ENTAME"
                        @click.stop="terminerFabrication(fab.id)"
                        class="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700"
                      >
                        Terminer
                      </button>
                    </div>

                    <!-- Composants/Lignes de fabrication -->
                    <div v-if="fab.lignes && fab.lignes.length > 0" class="mt-4">
                      <h5 class="text-xs font-semibold uppercase text-slate-500 mb-2">Détails</h5>
                      <table class="w-full text-xs">
                        <thead class="bg-slate-100">
                          <tr>
                            <th class="px-3 py-2 text-left">Id</th>
                            <th class="px-3 py-2 text-left">ID Ingrédient</th>
                            <th class="px-3 py-2 text-left">Composants</th>
                            <th class="px-3 py-2 text-left">Désignation</th>
                            <th class="px-3 py-2 text-left">Date De Besoin</th>
                            <th class="px-3 py-2 text-right">Prix Unitaire</th>
                            <th class="px-3 py-2 text-right">Quantité</th>
                            <th class="px-3 py-2 text-right">Montant</th>
                            <th class="px-3 py-2 text-left">Unité</th>
                            <th class="px-3 py-2 text-left">Machine</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="ligne in fab.lignes" :key="ligne.id" class="border-b border-slate-100">
                            <td class="px-3 py-2 text-slate-700">{{ ligne.id }}</td>
                            <td class="px-3 py-2">
                              <span class="text-indigo-600">{{ ligne.idIngredients }}</span>
                            </td>
                            <td class="px-3 py-2 text-slate-700">{{ ligne.libelle || '-' }}</td>
                            <td class="px-3 py-2 text-slate-600">{{ ligne.remarque || '-' }}</td>
                            <td class="px-3 py-2 text-slate-700">{{ formatDate(ligne.dateBesoinn || '') }}</td>
                            <td class="px-3 py-2 text-right text-slate-700">{{ ligne.pu?.toLocaleString('fr-FR') || '-' }}</td>
                            <td class="px-3 py-2 text-right">
                              <span class="text-indigo-600 font-medium">{{ ligne.qte || 0 }}</span>
                            </td>
                            <td class="px-3 py-2 text-right text-slate-700">
                              {{ ligne.pu && ligne.qte ? (ligne.pu * ligne.qte).toLocaleString('fr-FR') : '-' }}
                            </td>
                            <td class="px-3 py-2 text-slate-600">{{ ligne.unite || '-' }}</td>
                            <td class="px-3 py-2 text-slate-600">{{ ligne.machine || '-' }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </td>
              </tr>
            </template>

            <tr v-if="items.length === 0">
              <td class="px-4 py-8 text-center text-sm text-slate-500" colspan="10">
                <i class="fas fa-inbox text-3xl text-slate-300 mb-2 block"></i>
                <p>Aucune fabrication trouvée.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="!loading && !error && totalElements > 0" class="px-4 py-3 border-t border-slate-200 bg-slate-50 flex flex-wrap items-center justify-between gap-4">
        <div class="text-sm text-slate-600">
          Affichage {{ displayedRange.start }}-{{ displayedRange.end }} sur {{ totalElements }} fabrication(s)
        </div>
        
        <div class="flex items-center gap-2">
          <button
            @click="previousPage"
            :disabled="!canGoPrevious"
            class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-100"
          >
            <i class="fas fa-chevron-left mr-1"></i>
            Précédent
          </button>
          
          <div class="flex gap-1">
            <button
              v-for="page in Math.min(5, totalPages)"
              :key="page"
              @click="goToPage(page - 1)"
              :class="[
                'rounded-lg px-3 py-1.5 text-sm',
                currentPage === page - 1
                  ? 'bg-indigo-600 text-white'
                  : 'border border-slate-200 bg-white hover:bg-slate-100'
              ]"
            >
              {{ page }}
            </button>
            <span v-if="totalPages > 5" class="px-2 text-slate-400">...</span>
            <button
              v-if="totalPages > 5"
              @click="goToPage(totalPages - 1)"
              :class="[
                'rounded-lg px-3 py-1.5 text-sm',
                currentPage === totalPages - 1
                  ? 'bg-indigo-600 text-white'
                  : 'border border-slate-200 bg-white hover:bg-slate-100'
              ]"
            >
              {{ totalPages }}
            </button>
          </div>
          
          <button
            @click="nextPage"
            :disabled="!canGoNext"
            class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-100"
          >
            Suivant
            <i class="fas fa-chevron-right ml-1"></i>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>
