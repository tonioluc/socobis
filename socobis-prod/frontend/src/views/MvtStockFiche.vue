<template>
  <div class="container">
    <!-- Header -->
    <header class="header">
      <div class="header-info">
        <h1 class="title">Fiche de Mouvement de Stock</h1>
        <p class="subtitle">{{ mvtStock.id || 'Nouveau mouvement' }}</p>
      </div>
      <div class="header-actions">
        <button v-if="canValidate" @click="validerMvtStock" class="btn btn-primary">
          <CheckCircle class="w-5 h-5" />
          <span>Valider</span>
        </button>
        <button @click="modifierMvtStock" class="btn btn-primary">
          <Edit class="w-5 h-5" />
          <span>Modifier</span>
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
          <div class="info-item">
            <label>Date</label>
            <span>{{ formatDate(mvtStock.daty) }}</span>
          </div>
          <div class="info-item">
            <label>Désignation</label>
            <span>{{ mvtStock.designation }}</span>
          </div>
          <div class="info-item">
            <label>Type de mouvement</label>
            <span>{{ mvtStock.idTypeMvStocklib }}</span>
          </div>
          <div class="info-item">
            <label>Magasin</label>
            <span>{{ mvtStock.idMagasinlib }}</span>
          </div>
          <div class="info-item" v-if="mvtStock.fabPrecedent">
            <label>Fabrication associée</label>
            <span>{{ mvtStock.fabPrecedent }}</span>
          </div>
          <div class="info-item">
            <label>Vente</label>
            <span>{{ mvtStock.idVentelib || '-' }}</span>
          </div>
          <div class="info-item">
            <label>Transfert</label>
            <span>{{ mvtStock.idTransfertlib || '-' }}</span>
          </div>
          <div class="info-item">
            <label>État</label>
            <span>{{ mvtStock.etatlib }}</span>
          </div>
          <div class="info-item">
            <label>Montant d'entrée</label>
            <span>{{ formatNumber(mvtStock.montantEntree) }}</span>
          </div>
          <div class="info-item">
            <label>Montant de sortie</label>
            <span>{{ formatNumber(mvtStock.montantSortie) }}</span>
          </div>
        </div>
      </section>

      <!-- Détails des mouvements -->
      <section class="card">
        <h3 class="section-title">Détails du mouvement</h3>
        <div class="total-amount">
          <strong>Montant Total : {{ formatNumber(totalMontant) }} Ar</strong>
        </div>
        <div class="table-container">
          <table class="details-table">
            <thead>
              <tr>
                <th>Id</th>
                <th>Ingrédients</th>
                <th>Entrée</th>
                <th>Sortie</th>
                <th>Prix unitaire</th>
                <th>Montant</th>
                <th>Mouvement source</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="detail in mvtStock.details" :key="detail.id">
                <td>{{ detail.id }}</td>
                <td>{{ detail.libelleexacte }}</td>
                <td>{{ detail.entree }}</td>
                <td>{{ detail.sortie }}</td>
                <td>{{ formatNumber(detail.pu) }}</td>
                <td>{{ formatNumber(detail.montant) }}</td>
                <td>{{ detail.mvtsrc }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { CheckCircle, Edit, ArrowLeft } from 'lucide-vue-next'

export default {
  name: 'MvtStockFiche',
  components: {
    CheckCircle,
    Edit,
    ArrowLeft
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010/socobis/api'

    const loading = ref(true)
    const mvtStock = ref({
      id: '',
      daty: '',
      designation: '',
      idTypeMvStock: '',
      idPoint: '',
      idMagasin: '',
      fabPrecedent: '',
      idVente: '',
      idTransfert: '',
      etat: '',
      montantEntree: 0,
      montantSortie: 0,
      details: []
    })

    const totalMontant = computed(() => {
      return mvtStock.value.details.reduce((sum, detail) => sum + (parseFloat(detail.montant) || 0), 0)
    })
    const canValidate = computed(() => {
      const etat = parseInt(mvtStock.value.etat)
      return etat === 1
    })

    // Chargement des données
    const loadMvtStock = async () => {
      const id = route.params.id
      if (!id) {
        router.push('/stock/mvtstock/liste')
        return
      }

      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'consulte',
            classe: 'stock.MvtStockLib',
            nomTable: 'mvtstocklib',
            id: id
          })
        })

        if (response.ok) {
          const result = await response.json()
          console.log('Données mouvement de stock:', result)
          if (result.success && result.data) {
            mvtStock.value = { ...result.data, details: [] } // Ne pas prendre details du mouvement principal
          }
        }
      } catch (error) {
        console.error('Erreur chargement mouvement:', error)
      }
    }

    const loadDetails = async () => {
      const id = route.params.id
      if (!id) return

      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'stock.MvtStockFilleLib',
            nomTable: 'MVTSTOCKFILLELIB',
            idMere: id,
            colonneMereFiltre: 'IDMVTSTOCK'
          })
        })

        if (response.ok) {
          const result = await response.json()
          if (result.success && result.data && result.data.liste) {
            mvtStock.value.details = result.data.liste
          }
        }
      } catch (error) {
        console.error('Erreur chargement détails:', error)
      }
    }

    // Utilitaires
    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      return new Date(dateStr).toLocaleDateString('fr-FR')
    }

    const formatNumber = (num) => {
      if (num === null || num === undefined) return '0'
      return Number(num).toLocaleString('fr-FR')
    }

    // Actions
    const validerMvtStock = async () => {
      if (!confirm('Êtes-vous sûr de vouloir valider ce mouvement de stock ?')) return

      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'valider',
            classe: 'stock.MvtStock',
            nomTable: 'mvtstock',
            id: mvtStock.value.id
          })
        })

        const result = await response.json()
        if (result.success) {
          alert('Mouvement de stock validé avec succès')
          // Recharger les données
          await loadMvtStock()
          await loadDetails()
        } else {
          alert('Erreur lors de la validation: ' + (result.message || 'Erreur inconnue'))
        }
      } catch (error) {
        console.error('Erreur validation:', error)
        alert('Erreur de connexion: ' + error.message)
      }
    }

    const modifierMvtStock = () => {
      router.push({
        name: 'MvtStockSaisie',
        query: { id: mvtStock.value.id, acte: 'update' }
      })
    }

    const goBack = () => {
      router.go(-1)
    }

    onMounted(async () => {
      await loadMvtStock()
      await loadDetails()
      loading.value = false
    })

    return {
      loading,
      mvtStock,
      totalMontant,
      canValidate,
      formatDate,
      formatNumber,
      validerMvtStock,
      modifierMvtStock,
      goBack
    }
  }
}
</script>

<style scoped>
/* Styles similaires à FabricationFiche */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.header-info h1 {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  color: #111827;
}

.header-info p {
  margin: 0.5rem 0 0 0;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background-color: #2563eb;
}

.btn-outline {
  background-color: white;
  color: #6b7280;
  border: 1px solid #d1d5db;
}

.btn-outline:hover {
  background-color: #f9fafb;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.loading-spinner {
  width: 2rem;
  height: 2rem;
  border: 2px solid #e5e7eb;
  border-top: 2px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.card {
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  margin-bottom: 2rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.info-item {
  display: flex;
  flex-direction: column;
}

.info-item label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.25rem;
}

.info-item span {
  font-size: 1rem;
  color: #111827;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 1rem;
}

.total-amount {
  text-align: right;
  margin-bottom: 1rem;
  font-size: 1rem;
  color: #374151;
}

.table-container {
  overflow-x: auto;
}

.details-table {
  width: 100%;
  border-collapse: collapse;
}

.details-table th,
.details-table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.details-table th {
  background-color: #f9fafb;
  font-weight: 600;
  color: #374151;
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background-color: white;
  font-size: 0.875rem;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
</style>