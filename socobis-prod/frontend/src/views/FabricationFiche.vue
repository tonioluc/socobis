<template>
  <div class="container">
    <!-- Header -->
    <header class="header">
      <div class="header-info">
        <h1 class="title">Fiche de Fabrication</h1>
        <p class="subtitle">{{ fabrication.id || 'Nouvelle Fabrication' }}</p>
      </div>
      <div class="header-actions">
        <button v-if="canValidate" @click="validerFabrication" class="btn btn-primary">
          <CheckCircle class="w-5 h-5" />
          <span>Valider</span>
        </button>
        <button v-if="canEdit" @click="modifierFabrication" class="btn btn-primary">
          <Edit class="w-5 h-5" />
          <span>Modifier</span>
        </button>

        <button v-if="peutGenererMvtStock" @click="genererMvtEntree" class="btn btn-success">
          <Package class="w-5 h-5" />
          <span>Mouvement entrée</span>
        </button>

        <button v-if="peutGenererMvtStock" @click="genererMvtSortie" class="btn btn-warning">
          <Package class="w-5 h-5" />
          <span>Mouvement sortie</span>
        </button>

        <button v-if="peutGenererMvtStock" @click="genererResidu" class="btn btn-secondary">
          <Recycle class="w-5 h-5" />
          <span>Résidu</span>
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
            <span>{{ fabrication.id }}</span>
          </div>
          <div class="info-field">
            <label>Remarque</label>
            <span>{{ fabrication.remarque || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Désignation</label>
            <span>{{ fabrication.libelle }}</span>
          </div>
          <div class="info-field">
            <label>Date de besoin</label>
            <span>-</span>
          </div>
          <div class="info-field">
            <label>Date</label>
            <span>{{ formatDate(fabrication.daty) }}</span>
          </div>
          <div class="info-field">
            <label>Ordre de fabrication fille associé</label>
            <span>{{ fabrication.idOffille }}</span>
          </div>
          <div class="info-field">
            <label>Ordre de fabrication associé</label>
            <span>{{ fabrication.idOf }}</span>
          </div>
          <div class="info-field">
            <label>Lancée par</label>
            <span>{{ fabrication.lanceParLib }}</span>
          </div>
          <div class="info-field">
            <label>Cible</label>
            <span>{{ fabrication.cibleLib }}</span>
          </div>
          <div class="info-field">
            <label>ÉTAT</label>
            <span :class="getEtatClass(fabrication.etat)">{{ fabrication.etatLib }}</span>
          </div>
          <div class="info-field">
            <label>Fabrication précédente</label>
            <span>{{ fabrication.fabricationPrec }}</span>
          </div>
          <div class="info-field">
            <label>Fabrication Suivant</label>
            <span>{{ fabrication.fabricationSuiv || '-' }}</span>
          </div>
        </div>
      </section>

      <!-- Onglets -->
      <div class="tabs">
        <button class="tab active">Détails</button>
        <button class="tab">Mouvement de stock</button>
      </div>

      <!-- Détails des composants -->
      <section class="card">
        <div class="table-container">
          <table class="details-table">
            <thead>
              <tr>
                <th>id</th>
                <th>ID Ingrédient</th>
                <th>Composants</th>
                <th>Désignation</th>
                <th>Date de besoin</th>
                <th>Prix unitaire</th>
                <th>Quantité</th>
                <th>Montant</th>
                <th>Unité</th>
                <th>Machine</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="composant in composants" :key="composant.id">
                <td>{{ composant.id }}</td>
                <td>{{ composant.idIngredients }}</td>
                <td>{{ composant.idingredientsLib }}</td>
                <td>{{ fabrication.libelle }}</td>
                <td>-</td>
                <td>{{ composant.pu || 0 }}</td>
                <td>{{ composant.qte }}</td>
                <td>{{ (composant.pu || 0) * composant.qte }}</td>
                <td>{{ composant.idunitelib }}</td>
                <td>{{ composant.idMachineLib }}</td>
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
import { CheckCircle, Edit, ArrowLeft, Package, Recycle } from 'lucide-vue-next'

export default {
  name: 'FabricationFiche',
  components: {
    CheckCircle,
    Edit,
    ArrowLeft,
    Package,
    Recycle
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010/socobis/api'

    const loading = ref(true)
    const fabrication = ref({
      id: '',
      daty: '',
      lancePar: '',
      lanceParLib: '',
      cible: '',
      cibleLib: '',
      libelle: '',
      etat: 0,
      etatLib: '',
      idOffille: '',
      idBc: '',
      equipe: '',
      remarque: '',
      composants: []
    })

    const loadFabrication = async (id) => {
      try {
        loading.value = true
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'consulte',
            classe: 'fabrication.FabricationCpl',
            nomTable: 'FabricationCpl',
            id: id
          })
        })

        if (response.ok) {
          const data = await response.json()
          if (data.success && data.data) {
            fabrication.value = data.data
            // Charger les détails des composants si nécessaire
            await loadComposants(id)
          }
        }
      } catch (error) {
        console.error('Erreur chargement fabrication:', error)
      } finally {
        loading.value = false
      }
    }

    const loadComposants = async (id) => {
      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'fabrication.FabricationFilleCpl',
            nomTable: 'FabricationFilleCpl',
            idMere: id
          })
        })

        if (response.ok) {
          const data = await response.json()
          if (data.success && data.data && data.data.liste) {
            fabrication.value.composants = data.data.liste
          }
        }
      } catch (error) {
        console.error('Erreur chargement composants:', error)
      }
    }

    const validerFabrication = async () => {
      if (!confirm('Êtes-vous sûr de vouloir valider cette fabrication ?')) return

      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'valider',
            classe: 'fabrication.Fabrication',
            nomTable: 'fabrication',
            id: fabrication.value.id
          })
        })

        const result = await response.json()
        if (result.success) {
          alert('Fabrication validée avec succès')
          // Recharger les données
          await loadFabrication(fabrication.value.id)
        } else {
          alert('Erreur lors de la validation: ' + (result.message || 'Erreur inconnue'))
        }
      } catch (error) {
        console.error('Erreur validation:', error)
        alert('Erreur de connexion: ' + error.message)
      }
    }

    const modifierFabrication = () => {
      router.push(`/fabrication/saisie?id=${fabrication.value.id}&acte=update`)
    }

    const genererMvtEntree = () => {
      router.push(`/stock/mvtstock/saisie?idOf=${fabrication.value.idOf}&idFab=${fabrication.value.id}&idTypeMvStock=TPMVST000001`)
    }

    const genererMvtSortie = () => {
      router.push(`/stock/mvtstock/saisie?idOf=${fabrication.value.idOf}&idFab=${fabrication.value.id}&idTypeMvStock=TPMVST000022`)
    }

    const genererResidu = () => {
      router.push(`/stock/mvtstock/saisie?idFab=${fabrication.value.id}&isResidu=true`)
    }

    const goBack = () => {
      router.push('/fabrication/liste')
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      return new Date(dateStr).toLocaleDateString('fr-FR')
    }

    const getEtatClass = (etat) => {
      switch (etat) {
        case 1: return 'etat-brouillon'
        case 11: return 'etat-valide'
        default: return 'etat-autre'
      }
    }

    const canValidate = computed(() => fabrication.value.etat === 1)
    const canEdit = computed(() => fabrication.value.etat === 1)
    const composants = computed(() => fabrication.value.composants || [])
    const peutGenererMvtStock = computed(() => fabrication.value.etat >= 11)

    onMounted(async () => {
      const id = route.params.id || route.query.id
      if (id) {
        await loadFabrication(id)
      } else {
        loading.value = false
      }
    })

    return {
      loading,
      fabrication,
      composants,
      validerFabrication,
      modifierFabrication,
      genererMvtEntree,
      genererMvtSortie,
      genererResidu,
      goBack,
      formatDate,
      getEtatClass,
      canValidate,
      canEdit,
      peutGenererMvtStock
    }
  }
}
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
  gap: 0.75rem;
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s;
  border: none;
  cursor: pointer;
}

.btn-primary {
  background-color: #2563eb;
  color: white;
}

.btn-primary:hover {
  background-color: #1d4ed8;
}

.btn-success {
  background-color: #059669;
  color: white;
}

.btn-success:hover {
  background-color: #047857;
}

.btn-danger {
  background-color: #dc2626;
  color: white;
}

.btn-danger:hover {
  background-color: #b91c1c;
}

.btn-secondary {
  background-color: #6b7280;
  color: white;
}

.btn-secondary:hover {
  background-color: #4b5563;
}

.btn-outline {
  background-color: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-outline:hover {
  background-color: #f9fafb;
}

.loading-overlay {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e5e7eb;
  border-top: 4px solid #2563eb;
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
  margin-bottom: 1.5rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.info-field {
  display: flex;
  flex-direction: column;
}

.info-field label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.25rem;
}

.info-field span {
  font-size: 1rem;
  color: #111827;
}

.etat-brouillon {
  color: #d97706;
  font-weight: 500;
}

.etat-valide {
  color: #059669;
  font-weight: 500;
}

.etat-autre {
  color: #6b7280;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 1rem;
}

.table-container {
  overflow-x: auto;
}

.details-table {
  width: 100%;
  min-width: 600px;
  border-collapse: collapse;
}

.details-table th {
  padding: 0.75rem 1rem;
  background-color: #f9fafb;
  text-align: left;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
}

.tabs {
  display: flex;
  gap: 2rem;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
}

.tab {
  padding: 0.75rem 2rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: #6b7280;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 0.375rem;
}

.tab.active {
  color: #2563eb;
  border-bottom-color: #2563eb;
  background-color: rgba(37, 99, 235, 0.05);
}

.tab:hover {
  color: #374151;
  background-color: rgba(0, 0, 0, 0.02);
}
</style>