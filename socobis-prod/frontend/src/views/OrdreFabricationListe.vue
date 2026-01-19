<template>
  <div class="container">
    <!-- Header -->
    <header class="header">
      <div class="header-info">
        <h1 class="title">Liste des ordres de fabrication</h1>
      </div>
      <div class="header-actions">
        <button @click="nouveauOrdre" class="btn btn-primary">
          <Plus class="w-5 h-5" />
          <span>Nouveau</span>
        </button>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>

    <!-- Filters -->
    <div v-else class="card filters-card">
      <form @submit.prevent="rechercher" class="filters-form">
        <div class="filters-grid">
          <div class="filter-field">
            <label>ID</label>
            <input v-model="filters.id" type="text" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>Date début</label>
            <input v-model="filters.daty1" type="date" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>Date fin</label>
            <input v-model="filters.daty2" type="date" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>Date besoin début</label>
            <input v-model="filters.besoin1" type="date" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>Date besoin fin</label>
            <input v-model="filters.besoin2" type="date" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>Désignation</label>
            <input v-model="filters.libelle" type="text" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>Lancé par</label>
            <input v-model="filters.lancepar" type="text" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>Cible</label>
            <input v-model="filters.cible" type="text" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>Remarque</label>
            <input v-model="filters.remarque" type="text" class="filter-input" />
          </div>
        </div>
        <div class="filters-actions">
          <button type="submit" class="btn btn-primary">
            <Search class="w-5 h-5" />
            <span>Rechercher</span>
          </button>
          <button type="button" @click="resetFilters" class="btn btn-outline">
            <span>Réinitialiser</span>
          </button>
        </div>
      </form>
    </div>

    <!-- Results -->
    <div class="card">
      <div v-if="loading" class="empty-state">
        <div class="spinner"></div>
        <p>Chargement...</p>
      </div>
      <div v-else-if="ordres.length === 0" class="empty-state">
        <p>Aucun ordre de fabrication trouvé</p>
      </div>
      <div v-else class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Date</th>
              <th>Date de besoin</th>
              <th>Désignation</th>
              <th>Lancé par</th>
              <th>Cible</th>
              <th>Remarque</th>
              <th>État</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ordre in ordres" :key="ordre.id">
              <td>{{ ordre.id }}</td>
              <td>{{ formatDate(ordre.daty) }}</td>
              <td>{{ formatDate(ordre.besoin) }}</td>
              <td>{{ ordre.libelle }}</td>
              <td>{{ ordre.lancepar }}</td>
              <td>{{ ordre.cible }}</td>
              <td>{{ ordre.remarque }}</td>
              <td>{{ ordre.etatLib }}</td>
              <td class="actions-cell">
                <button @click="voirOrdre(ordre.id)" class="btn-action btn-view">
                  <Eye class="w-4 h-4" />
                </button>
                <button v-if="ordre.etat == 1" @click="validerOrdre(ordre.id)" class="btn-action btn-success">
                  <CheckCircle class="w-4 h-4" />
                </button>
                <button @click="modifierOrdre(ordre.id)" class="btn-action btn-edit">
                  <Edit class="w-4 h-4" />
                </button>
                <button @click="supprimerOrdre(ordre.id)" class="btn-action btn-delete">
                  <Trash class="w-4 h-4" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="ordres.length > 0" class="pagination">
      <button @click="pagePrecedente" :disabled="currentPage === 1" class="btn btn-outline">
        Précédent
      </button>
      <span>Page {{ currentPage }} sur {{ totalPages }}</span>
      <button @click="pageSuivante" :disabled="currentPage === totalPages" class="btn btn-outline">
        Suivant
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Plus,
  Search,
  Eye,
  Edit,
  Trash,
  CheckCircle
} from 'lucide-vue-next'

const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010/socobis/api'

// Reactive data
const ordres = ref([])
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const filters = ref({
  id: '',
  daty1: '',
  daty2: '',
  besoin1: '',
  besoin2: '',
  libelle: '',
  lancepar: '',
  cible: '',
  remarque: ''
})

// Methods
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('fr-FR')
}

const nouveauOrdre = () => {
  router.push('/ordre-fabrication/saisie')
}

const voirOrdre = (id) => {
  router.push(`/ordre-fabrication/fiche/${id}`)
}

const modifierOrdre = (id) => {
  router.push(`/ordre-fabrication/modif/${id}`)
}

const validerOrdre = async (id) => {
  if (!confirm('Êtes-vous sûr de vouloir viser cet ordre de fabrication ?')) return

  try {
    const response = await fetch(`${API_BASE}/aprestarif`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({
        acte: 'valider',
        classe: 'fabrication.Of',
        nomtable: 'OFAB',
        id: id
      })
    })

    const result = await response.json()
    if (result.success) {
      await loadOrdres() // Recharger la liste
    } else {
      alert('Erreur lors de la validation: ' + (result.message || 'Erreur inconnue'))
    }
  } catch (error) {
    console.error('Erreur lors de la validation:', error)
    alert('Erreur lors de la validation')
  }
}

const supprimerOrdre = async (id) => {
  if (!confirm('Êtes-vous sûr de vouloir supprimer cet ordre de fabrication ?')) return

  try {
    const response = await fetch(`${API_BASE}/aprestarif`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({
        acte: 'delete',
        classe: 'fabrication.Of',
        nomtable: 'OFAB',
        id: id
      })
    })

    const result = await response.json()
    if (result.success) {
      await loadOrdres() // Recharger la liste
    } else {
      alert('Erreur lors de la suppression: ' + (result.message || 'Erreur inconnue'))
    }
  } catch (error) {
    console.error('Erreur lors de la suppression:', error)
    alert('Erreur lors de la suppression')
  }
}

const rechercher = () => {
  currentPage.value = 1
  loadOrdres()
}

const resetFilters = () => {
  filters.value = {
    id: '',
    daty1: '',
    daty2: '',
    besoin1: '',
    besoin2: '',
    libelle: '',
    lancepar: '',
    cible: '',
    remarque: ''
  }
  loadOrdres()
}

const pagePrecedente = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    loadOrdres()
  }
}

const pageSuivante = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    loadOrdres()
  }
}

// Charger les ordres
const loadOrdres = async () => {
  loading.value = true
  try {
    // Construire le payload JSON
    const payload = {
      acte: 'liste',
      classe: 'fabrication.Of',
      nomTable: 'OFABLIB'
    }

    // Ajouter les filtres
    if (filters.value.id) payload.id = filters.value.id
    if (filters.value.daty1) payload.daty1 = filters.value.daty1
    if (filters.value.daty2) payload.daty2 = filters.value.daty2
    if (filters.value.besoin1) payload.besoin1 = filters.value.besoin1
    if (filters.value.besoin2) payload.besoin2 = filters.value.besoin2
    if (filters.value.libelle) payload.libelle = filters.value.libelle
    if (filters.value.lancepar) payload.lancepar = filters.value.lancepar
    if (filters.value.cible) payload.cible = filters.value.cible
    if (filters.value.remarque) payload.remarque = filters.value.remarque

    const response = await fetch(`${API_BASE}/apresmultiple`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify(payload)
    })

    const data = await response.json()
    console.log('Ordres chargés:', data)
    if (data.data && data.data.liste && Array.isArray(data.data.liste)) {
      ordres.value = data.data.liste
      // Pour la pagination simple, on peut ajuster selon les besoins
      totalPages.value = Math.ceil(ordres.value.length / 20) || 1
    }
  } catch (error) {
    console.error('Erreur lors du chargement des ordres:', error)
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(() => {
  // Initialiser les dates par défaut (semaine actuelle)
  const today = new Date()
  const weekStart = new Date(today.setDate(today.getDate() - today.getDay()))
  const weekEnd = new Date(today.setDate(today.getDate() - today.getDay() + 6))

  filters.value.daty1 = weekStart.toISOString().split('T')[0]
  filters.value.daty2 = weekEnd.toISOString().split('T')[0]
  filters.value.besoin1 = weekStart.toISOString().split('T')[0]
  filters.value.besoin2 = weekEnd.toISOString().split('T')[0]

  loadOrdres()
})
</script>

<style scoped>
.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}

.header-info .title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #111827;
  letter-spacing: -0.5px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn {
  padding: 10px 18px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.btn:active {
  transform: translateY(0);
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.btn-outline {
  background: white;
  color: #374151;
  border: 1.5px solid #d1d5db;
}

.btn-outline:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.card {
  background: white;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  border: 1px solid #f3f4f6;
}

.filters-card {
  padding: 0;
  overflow: hidden;
}

.filters-form {
  padding: 24px;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-field label {
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
}

.filter-input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background-color: white;
}

.filter-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.filters-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
}

.empty-state p {
  margin: 8px 0;
  font-size: 15px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f4f6;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.table-wrapper {
  overflow-x: auto;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
}

.data-table thead {
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
}

.data-table th {
  padding: 14px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid #e5e7eb;
  white-space: nowrap;
}

.data-table td {
  padding: 14px 16px;
  border-bottom: 1px solid #f3f4f6;
  color: #374151;
  font-size: 14px;
}

.data-table tbody tr:hover {
  background: #fafafa;
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.actions-cell {
  display: flex;
  gap: 8px;
}

.btn-action {
  padding: 6px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-action:hover {
  transform: scale(1.1);
}

.btn-view {
  background: #e5e7eb;
  color: #374151;
}

.btn-edit {
  background: #3b82f6;
  color: white;
}

.btn-delete {
  background: #ef4444;
  color: white;
}

.btn-success {
  background: #10b981;
  color: white;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f4f6;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@media (max-width: 768px) {
  .container {
    padding: 16px;
  }

  .header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .btn {
    flex: 1;
    justify-content: center;
  }

  .filters-grid {
    grid-template-columns: 1fr;
  }

  .actions-cell {
    flex-direction: column;
  }
}
</style>