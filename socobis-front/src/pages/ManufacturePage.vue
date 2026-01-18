<script setup lang="ts">
import { computed, ref, watchEffect, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, type Produit, type SimulationFabrication, type FormuleItem, type FabricationHistorique } from '@/services/api'

const route = useRoute()
const router = useRouter()

// Data
const allProducts = ref<Produit[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const simulation = ref<SimulationFabrication | null>(null)
const simulating = ref(false)
const fabricating = ref(false)
const lastEntry = ref<FabricationHistorique | null>(null)

// Form
const selectedProductId = ref<string>('')
const qtyToProduce = ref<number>(100)

// Computed
const finishedProducts = computed(() => 
  allProducts.value.filter((p) => p.type === 'PRODUIT_FINI' || p.type === 'PRODUIT_INTERMEDIAIRE')
)

const selectedProduct = computed(() => 
  allProducts.value.find((p) => p.id === selectedProductId.value)
)

const needs = computed(() => simulation.value?.besoins ?? [])

const missingIntermediates = computed(() => 
  needs.value.filter((n) => n.type === 'PRODUIT_INTERMEDIAIRE' && !n.suffisant)
)

const canManufacture = computed(() => simulation.value?.peutFabriquer ?? false)

// Lifecycle
onMounted(async () => {
  await loadProduits()
})

watchEffect(() => {
  const fromQuery = typeof route.query.productId === 'string' ? route.query.productId : ''
  if (fromQuery && fromQuery !== selectedProductId.value) {
    selectedProductId.value = fromQuery
  }

  if (!selectedProductId.value && finishedProducts.value.length > 0) {
    selectedProductId.value = finishedProducts.value[0]?.id ?? ''
  }
})

// Simuler quand le produit ou la quantité change
watchEffect(async () => {
  if (selectedProductId.value && qtyToProduce.value > 0 && !loading.value) {
    await simulerFabrication()
  }
})

// Methods
async function loadProduits() {
  loading.value = true
  error.value = null
  try {
    allProducts.value = await api.getAllProduits()
  } catch (e: any) {
    error.value = e.message || 'Erreur lors du chargement des produits'
    console.error('Erreur:', e)
  } finally {
    loading.value = false
  }
}

async function simulerFabrication() {
  if (!selectedProductId.value || qtyToProduce.value <= 0) return
  
  simulating.value = true
  try {
    simulation.value = await api.simulerFabrication(selectedProductId.value, qtyToProduce.value)
  } catch (e: any) {
    console.error('Erreur simulation:', e)
  } finally {
    simulating.value = false
  }
}

async function executerFabrication() {
  if (!selectedProductId.value || qtyToProduce.value <= 0) return
  
  fabricating.value = true
  error.value = null
  
  try {
    const result = await api.executerFabrication({
      produitId: selectedProductId.value,
      quantite: qtyToProduce.value
    })
    lastEntry.value = result
    // Rafraîchir la simulation après fabrication
    await simulerFabrication()
    // Rafraîchir la liste des produits (stocks mis à jour)
    await loadProduits()
  } catch (e: any) {
    error.value = e.message || 'Erreur lors de la fabrication'
    console.error('Erreur fabrication:', e)
  } finally {
    fabricating.value = false
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

function pillClass(ok: boolean) {
  return ok
    ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'
    : 'bg-rose-50 text-rose-700 ring-1 ring-rose-200'
}

function applyProduct() {
  router.replace({ path: '/fabrication', query: { productId: selectedProductId.value } })
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleString('fr-FR')
}
</script>

<template>
  <div class="space-y-6">
    <!-- Loading state -->
    <div v-if="loading" class="flex items-center justify-center py-12">
      <div class="text-center">
        <i class="fas fa-spinner fa-spin text-3xl text-slate-400"></i>
        <p class="mt-3 text-sm text-slate-600">Chargement...</p>
      </div>
    </div>

    <template v-else>
      <!-- Sélection produit -->
      <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 class="text-lg font-semibold text-slate-900">Fabriquer un produit</h2>
            <p class="mt-1 text-sm text-slate-600">
              Choisir le produit et la quantité. Le système calcule les besoins à partir de la formule.
            </p>
          </div>

          <div class="grid gap-3 sm:grid-cols-3">
            <div class="sm:col-span-2">
              <label class="mb-1 block text-xs font-medium text-slate-600">Produit à fabriquer</label>
              <select
                v-model="selectedProductId"
                class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-slate-400"
                @change="applyProduct"
              >
                <option v-for="p in finishedProducts" :key="p.id" :value="p.id">{{ p.libelle }}</option>
              </select>
            </div>

            <div>
              <label class="mb-1 block text-xs font-medium text-slate-600">Quantité à fabriquer</label>
              <input
                v-model.number="qtyToProduce"
                type="number"
                min="0"
                class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-slate-400"
              />
            </div>
          </div>
        </div>

        <div class="mt-5 rounded-xl bg-slate-50 p-4">
          <div class="flex flex-wrap items-center gap-3 text-sm">
            <div class="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1.5 ring-1 ring-slate-200">
              <i class="fas fa-box-open text-slate-500"></i>
              <span class="text-slate-600">Produit:</span>
              <span class="font-semibold text-slate-900">{{ selectedProduct?.libelle ?? '—' }}</span>
            </div>
            <div class="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1.5 ring-1 ring-slate-200">
              <i class="fas fa-hashtag text-slate-500"></i>
              <span class="text-slate-600">Quantité:</span>
              <span class="font-semibold text-slate-900">{{ qtyToProduce }}</span>
              <span class="text-slate-600">{{ selectedProduct?.unite }}</span>
            </div>
            <div v-if="simulating" class="inline-flex items-center gap-2 rounded-full bg-blue-50 px-3 py-1.5 ring-1 ring-blue-200">
              <i class="fas fa-spinner fa-spin text-blue-500"></i>
              <span class="text-blue-700">Calcul en cours...</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Besoins calculés -->
      <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <h3 class="text-base font-semibold text-slate-900">Besoins (formule)</h3>
          <div v-if="simulation" class="text-xs" :class="simulation.peutFabriquer ? 'text-emerald-600' : 'text-rose-600'">
            {{ simulation.message }}
          </div>
        </div>

        <div class="mt-4 overflow-hidden rounded-xl border border-slate-200">
          <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
              <tr>
                <th class="px-4 py-3">Élément</th>
                <th class="px-4 py-3">Type</th>
                <th class="px-4 py-3">Par unité</th>
                <th class="px-4 py-3">Besoin total</th>
                <th class="px-4 py-3">Stock</th>
                <th class="px-4 py-3">État</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200">
              <tr v-for="n in needs" :key="n.itemId" class="bg-white">
                <td class="px-4 py-3">
                  <div class="flex items-center gap-3">
                    <div
                      class="flex h-9 w-9 items-center justify-center rounded-xl"
                      :class="{
                        'bg-indigo-50 text-indigo-700': n.type === 'PRODUIT_INTERMEDIAIRE',
                        'bg-amber-50 text-amber-700': n.type === 'PRODUIT_FINI',
                        'bg-slate-100 text-slate-700': n.type === 'MATIERE_PREMIERE'
                      }"
                    >
                      <i :class="{
                        'fas fa-flask': n.type === 'PRODUIT_INTERMEDIAIRE',
                        'fas fa-cookie-bite': n.type === 'PRODUIT_FINI',
                        'fas fa-seedling': n.type === 'MATIERE_PREMIERE'
                      }"></i>
                    </div>
                    <div>
                      <div class="font-medium text-slate-900">{{ n.libelle }}</div>
                      <div class="text-xs text-slate-500">{{ n.itemId }}</div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-3 text-slate-700">{{ formatType(n.type) }}</td>
                <td class="px-4 py-3 text-slate-700">{{ n.qteParUnite }} {{ n.unite }}</td>
                <td class="px-4 py-3 font-semibold text-slate-900">{{ n.besoinTotal }} {{ n.unite }}</td>
                <td class="px-4 py-3 text-slate-700">{{ n.stockDisponible }} {{ n.unite }}</td>
                <td class="px-4 py-3">
                  <span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium" :class="pillClass(n.suffisant)">
                    <i :class="n.suffisant ? 'fas fa-check mr-1' : 'fas fa-exclamation-triangle mr-1'"></i>
                    <span v-if="n.suffisant">OK</span>
                    <span v-else>Manquant: {{ n.manquant }} {{ n.unite }}</span>
                  </span>
                </td>
              </tr>

              <tr v-if="needs.length === 0">
                <td class="px-4 py-6 text-center text-sm text-slate-500" colspan="6">
                  Aucune formule n'est définie pour ce produit.
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Alerte intermédiaires manquants -->
        <div v-if="missingIntermediates.length > 0" class="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4">
          <div class="flex items-start gap-3">
            <div class="mt-0.5 text-amber-700"><i class="fas fa-exclamation-circle"></i></div>
            <div>
              <div class="text-sm font-semibold text-amber-900">Produit(s) intermédiaire(s) insuffisant(s)</div>
              <div class="mt-1 text-sm text-amber-900/80">
                Il faut fabriquer d'abord les intermédiaires suivants avant de continuer la fabrication du produit fini.
              </div>
              <ul class="mt-3 space-y-2 text-sm">
                <li
                  v-for="m in missingIntermediates"
                  :key="m.itemId"
                  class="flex items-center justify-between rounded-lg bg-white px-3 py-2 ring-1 ring-amber-200"
                >
                  <div class="flex items-center gap-2">
                    <i class="fas fa-flask text-amber-700"></i>
                    <span class="font-medium text-slate-900">{{ m.libelle }}</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <div class="text-amber-900">
                      À fabriquer: <span class="font-semibold">{{ m.manquant }}</span> {{ m.unite }}
                    </div>
                    <button 
                      @click="router.push({ path: '/fabrication', query: { productId: m.itemId } })"
                      class="inline-flex items-center gap-1 rounded-lg bg-amber-600 px-2 py-1 text-xs font-medium text-white hover:bg-amber-700"
                    >
                      <i class="fas fa-cogs"></i>
                      Fabriquer
                    </button>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Erreur -->
        <div v-if="error" class="mt-5 rounded-xl border border-rose-200 bg-rose-50 p-4">
          <div class="flex items-start gap-3">
            <i class="fas fa-exclamation-circle mt-0.5 text-rose-500"></i>
            <div>
              <p class="font-medium text-rose-700">Erreur</p>
              <p class="mt-1 text-sm text-rose-600">{{ error }}</p>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="mt-5 flex flex-wrap gap-3">
          <button
            class="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!canManufacture || fabricating"
            @click="executerFabrication"
          >
            <i :class="fabricating ? 'fas fa-spinner fa-spin' : 'fas fa-play'"></i>
            {{ fabricating ? 'Fabrication...' : 'Fabriquer' }}
          </button>
          <button
            class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
            @click="router.push('/produits')"
          >
            <i class="fas fa-arrow-left"></i>
            Retour produits
          </button>
        </div>

        <!-- Confirmation entrée stock -->
        <div v-if="lastEntry" class="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="rounded-full bg-emerald-700 p-2 text-white">
                <i class="fas fa-check"></i>
              </div>
              <div>
                <div class="font-medium text-emerald-900">Fabrication réussie - Entrée en stock</div>
                <div class="text-emerald-800/80">
                  {{ lastEntry.produitLibelle }} : +{{ lastEntry.quantite }} {{ lastEntry.unite }}
                </div>
                <div class="text-xs text-emerald-700 mt-1">
                  {{ formatDate(lastEntry.dateFabrication) }}
                </div>
              </div>
            </div>
            <div class="text-emerald-900">Réf: {{ lastEntry.id }}</div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
