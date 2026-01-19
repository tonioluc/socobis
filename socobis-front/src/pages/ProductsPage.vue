<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, type Produit } from '@/services/api'

const router = useRouter()

const query = ref('')
const onlyType = ref<'TOUS' | 'PRODUIT_FINI' | 'PRODUIT_INTERMEDIAIRE' | 'MATIERE_PREMIERE'>('TOUS')
const products = ref<Produit[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

// Autocomplete
const autocompleteResults = ref<Produit[]>([])
const showAutocomplete = ref(false)
const autocompleteLoading = ref(false)
let autocompleteTimeout: ReturnType<typeof setTimeout> | null = null

onMounted(async () => {
  await loadProduits()
})

async function loadProduits() {
  loading.value = true
  error.value = null
  try {
    products.value = await api.getAllProduits()
  } catch (e: any) {
    error.value = e.message || 'Erreur lors du chargement des produits'
    console.error('Erreur:', e)
  } finally {
    loading.value = false
  }
}

// Watch query pour autocomplete
watch(query, (newQuery) => {
  if (autocompleteTimeout) {
    clearTimeout(autocompleteTimeout)
  }
  
  if (!newQuery || newQuery.trim().length < 2) {
    autocompleteResults.value = []
    showAutocomplete.value = false
    return
  }
  
  // Debounce 300ms
  autocompleteTimeout = setTimeout(async () => {
    await searchAutocomplete(newQuery)
  }, 300)
})

async function searchAutocomplete(searchQuery: string) {
  autocompleteLoading.value = true
  try {
    autocompleteResults.value = await api.autocomplete(searchQuery)
    showAutocomplete.value = autocompleteResults.value.length > 0
  } catch (e) {
    console.error('Erreur autocomplete:', e)
    autocompleteResults.value = []
    showAutocomplete.value = false
  } finally {
    autocompleteLoading.value = false
  }
}

function selectFromAutocomplete(product: Produit) {
  query.value = product.libelle
  showAutocomplete.value = false
}

function hideAutocomplete() {
  // Petit délai pour permettre le clic sur un résultat
  setTimeout(() => {
    showAutocomplete.value = false
  }, 200)
}

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  return products.value
    .filter((p) => (onlyType.value === 'TOUS' ? true : p.type === onlyType.value))
    .filter((p) => (q ? p.libelle.toLowerCase().includes(q) : true))
})

function formatProductType(type: string): string {
  switch (type) {
    case 'PRODUIT_FINI':
      return 'Produit Fini'
    case 'PRODUIT_INTERMEDIAIRE':
      return 'Intermédiaire'
    case 'MATIERE_PREMIERE':
      return 'Matière Première'
    default:
      return type
  }
}

function formatStockStatus(status: string): { label: string; tone: 'ok' | 'warn' | 'bad' } {
  switch (status) {
    case 'OK':
      return { label: 'Stock OK', tone: 'ok' }
    case 'BAS':
      return { label: 'Stock Bas', tone: 'warn' }
    case 'CRITIQUE':
      return { label: 'Stock Critique', tone: 'bad' }
    case 'RUPTURE':
      return { label: 'Rupture', tone: 'bad' }
    default:
      return { label: status, tone: 'ok' }
  }
}

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
            Produits finis, intermédiaires et matières premières avec stock disponible.
          </p>
        </div>

        <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
          <button
            @click="loadProduits"
            :disabled="loading"
            class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm hover:bg-slate-50"
          >
            <i class="fas fa-sync-alt" :class="{ 'animate-spin': loading }"></i>
            Actualiser
          </button>

          <div class="relative">
            <i class="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"></i>
            <input
              v-model="query"
              @focus="query.length >= 2 && (showAutocomplete = true)"
              @blur="hideAutocomplete"
              class="w-full rounded-xl border border-slate-200 bg-white py-2 pl-10 pr-3 text-sm outline-none focus:border-slate-400"
              placeholder="Rechercher un produit..."
            />
            <i v-if="autocompleteLoading" class="fas fa-spinner fa-spin absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"></i>
            
            <!-- Dropdown Autocomplete -->
            <div
              v-if="showAutocomplete && autocompleteResults.length > 0"
              class="absolute z-50 mt-1 w-full max-h-60 overflow-auto rounded-xl border border-slate-200 bg-white shadow-lg"
            >
              <div
                v-for="result in autocompleteResults"
                :key="result.id"
                @mousedown="selectFromAutocomplete(result)"
                class="flex items-center gap-3 px-4 py-2 hover:bg-slate-50 cursor-pointer"
              >
                <div
                  class="flex h-8 w-8 items-center justify-center rounded-lg"
                  :class="{
                    'bg-indigo-50 text-indigo-700': result.type === 'PRODUIT_FINI',
                    'bg-amber-50 text-amber-700': result.type === 'PRODUIT_INTERMEDIAIRE',
                    'bg-slate-100 text-slate-700': result.type === 'MATIERE_PREMIERE'
                  }"
                >
                  <i :class="{
                    'fas fa-cookie-bite': result.type === 'PRODUIT_FINI',
                    'fas fa-flask': result.type === 'PRODUIT_INTERMEDIAIRE',
                    'fas fa-seedling': result.type === 'MATIERE_PREMIERE'
                  }"></i>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="font-medium text-slate-900 truncate">{{ result.libelle }}</div>
                  <div class="text-xs text-slate-500">{{ formatProductType(result.type) }} • Stock: {{ result.stock }} {{ result.unite }}</div>
                </div>
              </div>
            </div>
          </div>

          <select
            v-model="onlyType"
            class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-slate-400"
          >
            <option value="TOUS">Tous</option>
            <option value="PRODUIT_FINI">Produits finis</option>
            <option value="PRODUIT_INTERMEDIAIRE">Intermédiaires</option>
            <option value="MATIERE_PREMIERE">Matières premières</option>
          </select>
        </div>
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="mt-5 flex items-center justify-center py-12">
        <div class="text-center">
          <i class="fas fa-spinner fa-spin text-3xl text-slate-400"></i>
          <p class="mt-3 text-sm text-slate-600">Chargement des produits...</p>
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
              @click="loadProduits"
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
              <th class="px-4 py-3">Produit</th>
              <th class="px-4 py-3">Type</th>
              <th class="px-4 py-3">Stock</th>
              <th class="px-4 py-3">État</th>
              <th class="px-4 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-for="p in filtered" :key="p.id" class="bg-white hover:bg-slate-50">
              <td class="px-4 py-3">
                <div class="flex items-center gap-3">
                  <div
                    class="flex h-9 w-9 items-center justify-center rounded-xl"
                    :class="{
                      'bg-indigo-50 text-indigo-700': p.type === 'PRODUIT_FINI',
                      'bg-amber-50 text-amber-700': p.type === 'PRODUIT_INTERMEDIAIRE',
                      'bg-slate-100 text-slate-700': p.type === 'MATIERE_PREMIERE'
                    }"
                  >
                    <i :class="{
                      'fas fa-cookie-bite': p.type === 'PRODUIT_FINI',
                      'fas fa-flask': p.type === 'PRODUIT_INTERMEDIAIRE',
                      'fas fa-seedling': p.type === 'MATIERE_PREMIERE'
                    }"></i>
                  </div>
                  <div>
                    <div class="font-medium text-slate-900">{{ p.libelle }}</div>
                    <div class="text-xs text-slate-500">ID: {{ p.id }}</div>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3 text-slate-700">{{ formatProductType(p.type) }}</td>
              <td class="px-4 py-3 font-medium text-slate-900">{{ p.stock }} {{ p.unite }}</td>
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
                  v-if="p.type === 'PRODUIT_FINI' || p.type === 'PRODUIT_INTERMEDIAIRE'"
                  class="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-3 py-2 text-xs font-semibold text-white hover:bg-slate-800"
                  @click="goManufacture(p.id)"
                >
                  <i class="fas fa-cogs"></i>
                  Fabriquer
                </button>
                <span v-else class="text-xs text-slate-400">—</span>
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

      <!-- Stats -->
      <div v-if="!loading && !error" class="mt-4 flex gap-4 text-xs text-slate-500">
        <span>{{ filtered.length }} produit(s) affiché(s)</span>
        <span>•</span>
        <span>{{ products.length }} produit(s) au total</span>
      </div>
    </section>
  </div>
</template>
