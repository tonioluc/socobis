<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const currentPath = computed(() => route.path)

function linkClass(path: string) {
  const active = currentPath.value === path
  return [
    'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition',
    active ? 'bg-slate-900 text-white' : 'text-slate-700 hover:bg-slate-100',
  ].join(' ')
}
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <div class="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <div class="flex gap-6">
        <!-- Sidebar -->
        <aside class="hidden w-72 shrink-0 lg:block">
          <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <div class="mb-4 flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-900 text-white"
              >
                <i class="fas fa-industry"></i>
              </div>
              <div>
                <div class="text-sm font-semibold">Socobis</div>
                <div class="text-xs text-slate-500">Fabrication (templates)</div>
              </div>
            </div>

            <nav class="space-y-1">
              <RouterLink to="/produits" :class="linkClass('/produits')">
                <i class="fas fa-cookie-bite w-5 text-center"></i>
                <span>Produits</span>
              </RouterLink>
              <RouterLink to="/fabrication" :class="linkClass('/fabrication')">
                <i class="fas fa-cogs w-5 text-center"></i>
                <span>Fabriquer</span>
              </RouterLink>
              <RouterLink to="/historique" :class="linkClass('/historique')">
                <i class="fas fa-history w-5 text-center"></i>
                <span>Historique</span>
              </RouterLink>
            </nav>

            <div class="mt-6 rounded-xl bg-slate-50 p-3 text-xs text-slate-600">
              <div class="font-semibold text-slate-700">Objectif</div>
              <div class="mt-1">
                Écran de fabrication (biscuit/bonbons) avec données statiques avant branchement backend.
              </div>
            </div>
          </div>
        </aside>

        <!-- Main -->
        <main class="min-w-0 flex-1">
          <header class="mb-6">
            <div class="flex items-center justify-between">
              <div>
                <div class="text-xs uppercase tracking-wide text-slate-500">Production</div>
                <h1 class="text-2xl font-bold text-slate-900">Étape de fabrication</h1>
              </div>
              <div class="hidden items-center gap-3 sm:flex">
                <div class="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700">
                  <i class="far fa-calendar-alt mr-2"></i>
                  18/01/2026
                </div>
              </div>
            </div>
          </header>

          <RouterView />
        </main>
      </div>
    </div>
  </div>
</template>
