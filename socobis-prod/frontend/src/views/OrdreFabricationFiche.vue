<template>
  <div class="container">
    <!-- Header -->
    <header class="header">
      <div class="header-info">
        <h1 class="title">Détails de l'ordre de fabrication</h1>
        <p class="subtitle">{{ ordre.id || 'Nouvel Ordre' }}</p>
      </div>
      <div class="header-actions">
        <button v-if="canValidate" @click="validerOrdre" class="btn btn-success">
          <CheckCircle class="w-5 h-5" />
          <span>Viser</span>
        </button>
        <button v-if="canEdit" @click="modifierOrdre" class="btn btn-primary">
          <Edit class="w-5 h-5" />
          <span>Modifier</span>
        </button>
        <button @click="supprimerOrdre" class="btn btn-danger">
          <Trash class="w-5 h-5" />
          <span>Supprimer</span>
        </button>
        <button v-if="ordre.etat >= 11" @click="demandeTransfert" class="btn btn-secondary">
          <span>Demande</span>
        </button>
        <button @click="situationGlobale" class="btn btn-secondary">
          <span>Situation globale</span>
        </button>
        <button @click="imprimerPDF" class="btn btn-secondary">
          <span>Imprimer en PDF</span>
        </button>
        <button @click="goBack" class="btn btn-outline">
          <ArrowLeft class="w-5 h-5" />
          <span>Retour</span>
        </button>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>

    <!-- Content -->
    <div v-else>
      <!-- Informations générales -->
      <section class="card">
        <div class="info-grid">
          <div class="info-field">
            <label>ID</label>
            <span>{{ ordre.id }}</span>
          </div>
          <div class="info-field">
            <label>Date</label>
            <span>{{ formatDate(ordre.daty) }}</span>
          </div>
          <div class="info-field">
            <label>Date de besoin</label>
            <span>{{ formatDate(ordre.besoin) }}</span>
          </div>
          <div class="info-field">
            <label>Désignation</label>
            <span>{{ ordre.libelle }}</span>
          </div>
          <div class="info-field">
            <label>Lancé par</label>
            <span>{{ ordre.lancepar }}</span>
          </div>
          <div class="info-field">
            <label>Cible</label>
            <span>{{ ordre.cible }}</span>
          </div>
          <div class="info-field">
            <label>Remarque</label>
            <span>{{ ordre.remarque || '-' }}</span>
          </div>
          <div class="info-field">
            <label>État</label>
            <span>{{ ordre.etatLib }}</span>
          </div>
          <div class="info-field">
            <label>Numéro de bon de commande</label>
            <span>{{ ordre.idBc || '-' }}</span>
          </div>
        </div>
      </section>

      <!-- Onglets -->
      <section class="card tabs-card">
        <div class="tabs-header">
          <button
            @click="activeTab = 'details'"
            :class="['tab-btn', { active: activeTab === 'details' }]">
            Détails
          </button>
          <button
            @click="activeTab = 'besoins'"
            :class="['tab-btn', { active: activeTab === 'besoins' }]">
            Besoins
          </button>
          <button
            @click="activeTab = 'fabrications'"
            :class="['tab-btn', { active: activeTab === 'fabrications' }]">
            Fabrications
          </button>
        </div>

        <div class="tabs-content">
          <!-- Onglet Détails -->
          <div v-if="activeTab === 'details'">
            <div v-if="loadingDetails" class="empty-state">
              <div class="spinner"></div>
              <p>Chargement des détails...</p>
            </div>
            <div v-else-if="ordreDetails.length === 0" class="empty-state">
              <p>Aucun détail trouvé</p>
            </div>
            <div v-else class="table-wrapper">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Composants</th>
                    <th>Unité</th>
                    <th>Désignation</th>
                    <th>PU de revient</th>
                    <th>Qté Ordre</th>
                    <th>Qté Fabriqué</th>
                    <th>Qté restante</th>
                    <th>Prix de vente</th>
                    <th>Montant Théorique</th>
                    <th>Valeur Fabriquée</th>
                    <th>Dépense de Fabrication</th>
                    <th>Taux de revient (%)</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="detail in ordreDetails" :key="detail.id">
                    <td>{{ detail.id }}</td>
                    <td>{{ detail.idIngredients }}</td>
                    <td>{{ detail.idunite }}</td>
                    <td>{{ detail.libelleexacte }}</td>
                    <td>{{ formatCurrency(detail.puRevient) }}</td>
                    <td>{{ detail.qte }}</td>
                    <td>{{ detail.qteFabrique }}</td>
                    <td>{{ detail.qteReste }}</td>
                    <td>{{ formatCurrency(detail.pv) }}</td>
                    <td>{{ formatCurrency(detail.montantRevient) }}</td>
                    <td>{{ formatCurrency(detail.montantentree) }}</td>
                    <td>{{ formatCurrency(detail.montantsortie) }}</td>
                    <td>{{ detail.tauxRevient }}%</td>
                    <td>
                      <div style="display: flex; gap: 4px; flex-wrap: wrap;">
                        <button @click="fabriquerLigne(detail.id, detail.libelleexacte)" class="btn-action" title="Fabriquer">
                          Fabriquer
                        </button>
                        <button @click="fabriquerUnParUnLigne(detail.id, detail.libelleexacte)" class="btn-action" title="Fabriquer un par un">
                          Un par un
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Onglet Besoins -->
          <div v-if="activeTab === 'besoins'">
            <div class="empty-state">
              <p>Fonctionnalité à implémenter</p>
            </div>
          </div>

          <!-- Onglet Fabrications -->
          <div v-if="activeTab === 'fabrications'">
            <div class="empty-state">
              <p>Fonctionnalité à implémenter</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  CheckCircle,
  Edit,
  Trash
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010/socobis/api'

// Reactive data
const ordre = ref({})
const ordreDetails = ref([])
const loading = ref(false)
const loadingDetails = ref(false)
const activeTab = ref(route.query.tab || 'details')

// Computed properties
const canValidate = computed(() => {
  return ordre.value.etat == 1
})

const canEdit = computed(() => {
  return true // Adjust based on permissions
})

// Methods
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('fr-FR')
}

const formatCurrency = (amount) => {
  if (!amount) return '0 Ar'
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'MGA',
    minimumFractionDigits: 0
  }).format(amount)
}

// Charger les données de l'ordre
const loadOrdre = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/apresmultiple`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({
        acte: 'consulte',
        classe: 'fabrication.Of',
        nomTable: 'OFABLIB',
        id: id
      })
    })

    const data = await response.json()
    console.log('Ordre chargé:', data)
    if (data.data) {
      ordre.value = data.data
      await loadOrdreDetails(id)
    }
  } catch (error) {
    console.error('Erreur lors du chargement de l\'ordre:', error)
  } finally {
    loading.value = false
  }
}

// Charger les détails de l'ordre
const loadOrdreDetails = async (ordreId) => {
  loadingDetails.value = true
  try {
    const response = await fetch(`${API_BASE}/apresmultiple`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({
        acte: 'listeFilles',
        classe: 'fabrication.Of',
        classeFille: 'fabrication.OfFilleCpl',
        nomTableFille: 'OfFilleLibStock',
        colonneMere: 'idmere',
        id: ordreId
      })
    })

    const data = await response.json()
    console.log('Détails de l\'ordre chargés:', data)
    if (data.data && data.data.filles && Array.isArray(data.data.filles)) {
      ordreDetails.value = data.data.filles
    }
  } catch (error) {
    console.error('Erreur lors du chargement des détails:', error)
  } finally {
    loadingDetails.value = false
  }
}

// Actions
const validerOrdre = async () => {
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
        id: ordre.value.id
      })
    })

    const result = await response.json()
    if (result.success) {
      await loadOrdre() // Recharger les données
    } else {
      alert('Erreur lors de la validation: ' + (result.message || 'Erreur inconnue'))
    }
  } catch (error) {
    console.error('Erreur lors de la validation:', error)
    alert('Erreur lors de la validation')
  }
}

const modifierOrdre = () => {
  router.push(`/fabrication/modif/${ordre.value.id}`)
}

const supprimerOrdre = async () => {
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
        id: ordre.value.id
      })
    })

    const result = await response.json()
    if (result.success) {
      router.push('/fabrication/liste')
    } else {
      alert('Erreur lors de la suppression: ' + (result.message || 'Erreur inconnue'))
    }
  } catch (error) {
    console.error('Erreur lors de la suppression:', error)
    alert('Erreur lors de la suppression')
  }
}

const demandeTransfert = () => {
  router.push(`/demande/transfert/saisie?idOf=${ordre.value.id}`)
}

const situationGlobale = () => {
  router.push(`/fabrication/situation-globale/${ordre.value.id}`)
}

const fabriquer = () => {
  router.push(`/fabrication/saisie?idOffille=${ordre.value.id}&designation=Fabrication de ${ordre.value.libelleexacte || ordre.value.libelle}`)
}

const fabriquerUnParUn = () => {
  router.push(`/fabrication/saisie?idOffille=${ordre.value.id}&unParUn=true&designation=Fabrication de ${ordre.value.libelleexacte || ordre.value.libelle}`)
}

const fabriquerLigne = (idOffille, designation) => {
  router.push(`/fabrication/saisie?idOffille=${idOffille}&designation=Fabrication de ${designation}`)
}

const fabriquerUnParUnLigne = (idOffille, designation) => {
  router.push(`/fabrication/saisie?idOffille=${idOffille}&unParUn=true&designation=Fabrication de ${designation}`)
}

const imprimerPDF = () => {
  window.open(`${API_BASE}/ExportPDF?action=fiche_ordre_fabrication&id=${ordre.value.id}`, '_blank')
}

const goBack = () => {
  router.push('/ordre-fabrication/liste')
}

// Lifecycle
onMounted(() => {
  loadOrdre()
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

.header-info .subtitle {
  color: #6b7280;
  font-size: 15px;
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

.btn-success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
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

.btn-secondary {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%);
  color: white;
}

.btn-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.card {
  background: white;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  border: 1px solid #f3f4f6;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.info-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-field label {
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
}

.info-field span {
  color: #111827;
}

.tabs-card {
  padding: 0;
  overflow: hidden;
}

.tabs-header {
  display: flex;
  border-bottom: 2px solid #f3f4f6;
  background: #fafafa;
}

.tab-btn {
  padding: 16px 28px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-weight: 500;
  font-size: 15px;
  color: #6b7280;
  transition: all 0.2s ease;
  position: relative;
}

.tab-btn:hover {
  color: #374151;
  background: rgba(59, 130, 246, 0.05);
}

.tab-btn.active {
  color: #2563eb;
  background: white;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 3px 3px 0 0;
}

.tabs-content {
  padding: 24px;
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

  .info-grid {
    grid-template-columns: 1fr;
  }
}

.btn-action {
  padding: 4px 8px;
  font-size: 12px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.btn-action:hover {
  background-color: #2563eb;
}
</style>