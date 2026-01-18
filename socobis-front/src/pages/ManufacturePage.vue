<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formulasByProductId, products, round2, performMockManufacture } from '@/mock/fabrication'

const route = useRoute()
const router = useRouter()

const finishedProducts = computed(() => products.filter((p) => p.type === 'FINI'))

const selectedProductId = ref<string>('')
const qtyToProduce = ref<number>(100)

watchEffect(() => {
  const fromQuery = typeof route.query.productId === 'string' ? route.query.productId : ''
  if (fromQuery && fromQuery !== selectedProductId.value) {
    selectedProductId.value = fromQuery
  }

  if (!selectedProductId.value) {
    selectedProductId.value = finishedProducts.value[0]?.id ?? ''
  }
})

const selectedProduct = computed(() => products.find((p) => p.id === selectedProductId.value))

const formulaItems = computed(() => formulasByProductId[selectedProductId.value] ?? [])

const needs = computed(() => {
  const q = Number.isFinite(qtyToProduce.value) ? Math.max(0, qtyToProduce.value) : 0
  return formulaItems.value.map((it) => {
    const required = round2(it.qtyPerUnit * q)
    const stockItem = products.find((p) => p.id === it.itemId)
    const available = stockItem?.stock ?? 0
    const missing = round2(Math.max(0, required - available))

    return {
      ...it,
      required,
      available,
      missing,
      enough: missing === 0,
    }
  })
})

const missingIntermediates = computed(() => needs.value.filter((n) => n.type === 'INTERMEDIAIRE' && n.missing > 0))

// local tick to force recompute when module data mutated
const tick = ref(0)

// last stock entry created by mock manufacture
const lastEntry = ref(null as any)

function pillClass(ok: boolean) {
  return ok
    ? 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200'
    : 'bg-rose-50 text-rose-700 ring-1 ring-rose-200'
}

function applyProduct() {
  router.replace({ path: '/fabrication', query: { productId: selectedProductId.value } })
}

function onMockManufacture() {
  const qty = Number.isFinite(qtyToProduce.value) ? Math.max(0, qtyToProduce.value) : 0
  if (!selectedProductId.value || qty <= 0) return

  try {
    const created = performMockManufacture(selectedProductId.value, qty)
    lastEntry.value = created
    tick.value++
  } catch (e) {
    // ignore in mock
    console.error(e)
  }
}
</script>

<template>
  <div class="space-y-6">
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
            <label class="mb-1 block text-xs font-medium text-slate-600">Produit fini</label>
            <select
              v-model="selectedProductId"
              class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-slate-400"
              @change="applyProduct"
            >
              <option v-for="p in finishedProducts" :key="p.id" :value="p.id">{{ p.name }}</option>
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
            <span class="font-semibold text-slate-900">{{ selectedProduct?.name ?? '—' }}</span>
          </div>
          <div class="inline-flex items-center gap-2 rounded-full bg-white px-3 py-1.5 ring-1 ring-slate-200">
            <i class="fas fa-hashtag text-slate-500"></i>
            <span class="text-slate-600">Quantité:</span>
            <span class="font-semibold text-slate-900">{{ qtyToProduce }}</span>
            <span class="text-slate-600">{{ selectedProduct?.unit }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-slate-900">Besoins (calcul formule)</h3>
        <div class="text-xs text-slate-500">SELECT 1 FROM DUAL sera validé côté backend</div>
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
                    :class="
                      n.type === 'INTERMEDIAIRE'
                        ? 'bg-indigo-50 text-indigo-700'
                        : n.type === 'CHARGE'
                          ? 'bg-amber-50 text-amber-700'
                          : 'bg-slate-100 text-slate-700'
                    "
                  >
                    <i
                      :class="
                        n.type === 'INTERMEDIAIRE'
                          ? 'fas fa-flask'
                          : n.type === 'CHARGE'
                            ? 'fas fa-bolt'
                            : 'fas fa-seedling'
                      "
                    ></i>
                  </div>
                  <div>
                    <div class="font-medium text-slate-900">{{ n.name }}</div>
                    <div class="text-xs text-slate-500">{{ n.itemId }}</div>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3 text-slate-700">{{ n.type }}</td>
              <td class="px-4 py-3 text-slate-700">{{ n.qtyPerUnit }} {{ n.unit }}</td>
              <td class="px-4 py-3 font-semibold text-slate-900">{{ n.required }} {{ n.unit }}</td>
              <td class="px-4 py-3 text-slate-700">{{ n.available }} {{ n.unit }}</td>
              <td class="px-4 py-3">
                <span class="inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium" :class="pillClass(n.enough)">
                  <i :class="n.enough ? 'fas fa-check mr-1' : 'fas fa-exclamation-triangle mr-1'"></i>
                  <span v-if="n.enough">OK</span>
                  <span v-else>Manquant: {{ n.missing }} {{ n.unit }}</span>
                </span>
              </td>
            </tr>

            <tr v-if="needs.length === 0">
              <td class="px-4 py-6 text-center text-sm text-slate-500" colspan="6">
                Aucune formule n'est définie (données statiques) pour ce produit.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

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
                  <span class="font-medium text-slate-900">{{ m.name }}</span>
                </div>
                <div class="text-amber-900">
                  À fabriquer: <span class="font-semibold">{{ m.missing }}</span> {{ m.unit }}
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div class="mt-5 flex flex-wrap gap-3">
        <button
          class="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800"
          :disabled="needs.some((n) => !n.enough && n.type !== 'CHARGE')"
          @click="onMockManufacture"
        >
          <i class="fas fa-play"></i>
          Fabriquer (mock)
        </button>
        <button
          class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          @click="router.push('/produits')"
        >
          <i class="fas fa-arrow-left"></i>
          Retour produits
        </button>
      </div>
      <div v-if="lastEntry" class="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="rounded-full bg-emerald-700 p-2 text-white">
              <i class="fas fa-plus"></i>
            </div>
            <div>
              <div class="font-medium text-emerald-900">Entrée en stock enregistrée</div>
              <div class="text-emerald-800/80">{{ lastEntry.productName }} : +{{ lastEntry.qtyProduced }} {{ lastEntry.unit }}</div>
            </div>
          </div>
          <div class="text-emerald-900">Réf: {{ lastEntry.id }}</div>
        </div>
      </div>
    </section>
  </div>
</template>
