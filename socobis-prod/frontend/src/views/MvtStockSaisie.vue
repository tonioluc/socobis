<template>
  <div class="container">
    <!-- Header -->
    <header class="header">
      <div class="header-info">
        <h1 class="title">{{ titre }}</h1>
        <p class="subtitle">{{ mvtStock.id || 'Nouveau mouvement de stock' }}</p>
      </div>
      <div class="header-actions">
        <button @click="sauvegarder" class="btn btn-primary">
          <Save class="w-5 h-5" />
          <span>Enregistrer</span>
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
        <div class="form-grid">
          <div class="form-field">
            <label>Date</label>
            <input v-model="mvtStock.daty" type="date" class="form-input">
          </div>
          <div class="form-field">
            <label>Désignation</label>
            <input v-model="mvtStock.designation" type="text" class="form-input" placeholder="Désignation du mouvement">
          </div>
          <div class="form-field">
            <label>Type de mouvement de stock</label>
            <select v-model="mvtStock.idTypeMvStock" class="form-input">
              <option value="">Sélectionner un type</option>
              <option v-for="type in typesMvtStock" :key="type.id" :value="type.id">
                {{ type.val }}
              </option>
            </select>
          </div>
          <div class="form-field">
            <label>Point</label>
            <select v-model="mvtStock.idPoint" class="form-input" @change="onPointChange">
              <option value="">Sélectionner un point</option>
              <option v-for="point in points" :key="point.id" :value="point.id">
                {{ point.val }}
              </option>
            </select>
          </div>
          <div class="form-field">
            <label>Magasin</label>
            <select v-model="mvtStock.idMagasin" class="form-input">
              <option value="">Sélectionner un magasin</option>
              <option v-for="magasin in magasins" :key="magasin.id" :value="magasin.id">
                {{ magasin.val }}
              </option>
            </select>
          </div>
          <div class="form-field" v-if="showFabPrecedent">
            <label>Fabrication précédente</label>
            <AutoComplete
              v-model="mvtStock.fabPrecedent"
              :search-function="searchFabrications"
              :all-items="allFabrications"
              display-field="libelle"
              value-field="id"
              placeholder="Rechercher une fabrication..."
              class="form-input"
            />
          </div>
        </div>
      </section>

      <!-- Détails des mouvements -->
      <section class="card">
        <div class="table-container">
          <table class="details-table">
            <thead>
              <tr>
                <th>Produit</th>
                <th>Désignation</th>
                <th>Entrée</th>
                <th>Sortie</th>
                <th>Prix unitaire</th>
                <th>Mouvement source</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(detail, index) in mvtStock.details" :key="index">
                <td>
                  <AutoComplete
                    v-model="detail.idProduit"
                    :search-function="searchProduits"
                    :all-items="allProduits"
                    display-field="display"
                    value-field="id"
                    placeholder="Rechercher un produit..."
                    class="form-input"
                    @select="onProduitSelect(detail, $event)"
                  />
                </td>
                <td>
                  <input v-model="detail.designation" type="text" class="form-input" readonly>
                </td>
                <td>
                  <input v-model.number="detail.entree" type="number" step="0.01" class="form-input" :disabled="mvtStock.idTypeMvStock === 'TPMVST000022'">
                </td>
                <td>
                  <input v-model.number="detail.sortie" type="number" step="0.01" class="form-input" :disabled="mvtStock.idTypeMvStock === 'TPMVST000001'">
                </td>
                <td>
                  <input v-model.number="detail.pu" type="number" step="0.01" class="form-input">
                </td>
                <td>
                  <AutoComplete
                    v-model="detail.mvtSrc"
                    :search-function="searchMvtSource"
                    :all-items="allMvtSource"
                    display-field="designation"
                    value-field="id"
                    placeholder="Mouvement source..."
                    class="form-input"
                    @select="onMvtSourceSelect(detail, $event)"
                  />
                </td>
                <td>
                  <button @click="removeDetail(index)" class="btn btn-danger btn-sm">
                    <Trash2 class="w-4 h-4" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <button @click="addDetail" class="btn btn-secondary mt-4">
            <Plus class="w-4 h-4" />
            Ajouter une ligne
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Save, CheckCircle, ArrowLeft, Plus, Trash2 } from 'lucide-vue-next'
import AutoComplete from '../components/AutoComplete.vue'

export default {
  name: 'MvtStockSaisie',
  components: {
    AutoComplete,
    Save,
    CheckCircle,
    ArrowLeft,
    Plus,
    Trash2
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010/socobis/api'

    const loading = ref(true)
    const titre = ref('Saisie de mouvement de stock')

    // Données principales
    const mvtStock = ref({
      id: '',
      daty: new Date().toISOString().split('T')[0],
      designation: '',
      idTypeMvStock: '',
      idPoint: '',
      idMagasin: '',
      fabPrecedent: '',
      details: []
    })

    // Données de référence
    const typesMvtStock = ref([])
    const points = ref([])
    const magasins = ref([])
    const allMagasins = ref([])
    const allFabrications = ref([])
    const allProduits = ref([])
    const allMvtSource = ref([])

    // États calculés
    const canValidate = computed(() => mvtStock.value.details.length > 0)
    const showFabPrecedent = computed(() => route.query.idOf || route.query.idFab)

    // Chargement des données de référence
    const loadReferenceData = async () => {
      try {
        // Types de mouvement
        const typesResponse = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'stock.TypeMvtStock',
            nomTable: 'TYPEMVTSTOCK'
          })
        })
        if (typesResponse.ok) {
          const typesData = await typesResponse.json()
          if (typesData.success) {
            typesMvtStock.value = typesData.data.liste || []
          }
        }

        // Points
        const pointsResponse = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'annexe.Point'
          })
        })
        if (pointsResponse.ok) {
          const pointsData = await pointsResponse.json()
          if (pointsData.success) {
            points.value = pointsData.data.liste || []
            // Sélectionner le premier point par défaut
            if (points.value.length > 0) {
              mvtStock.value.idPoint = points.value[0].id
            }
          }
        }

        // Magasins (tous au départ, filtrés par point après)
        const magasinsResponse = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'magasin.Magasin',
            nomTable: 'MAGASIN2'
          })
        })
        if (magasinsResponse.ok) {
          const magasinsData = await magasinsResponse.json()
          if (magasinsData.success) {
            allMagasins.value = magasinsData.data.liste || []
            magasins.value = [...allMagasins.value] // Copie pour le select
          }
        }

        // Fabrications pour autocomplete
        const fabResponse = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'fabrication.FabricationCpl',
            nomTable: 'FABRICATIONCPL'
          })
        })
        if (fabResponse.ok) {
          const fabData = await fabResponse.json()
          if (fabData.success) {
            allFabrications.value = fabData.data.liste || []
          }
        }

        // Produits pour autocomplete
        const prodResponse = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'produits.IngredientsLib',
            nomTable: 'ST_INGREDIENTSAUTO'
          })
        })
        if (prodResponse.ok) {
          const prodData = await prodResponse.json()
          if (prodData.success) {
            allProduits.value = (prodData.data.liste || []).map(item => ({
              ...item,
              display: `${item.id}-${item.libelle}`
            }))
          }
        }

        // Mouvements source pour autocomplete
        const mvtSourceResponse = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'stock.MvtStockEntreeAvecReste',
            nomTable: 'V_ETATSTOCK_ENTREE'
          })
        })
        if (mvtSourceResponse.ok) {
          const mvtSourceData = await mvtSourceResponse.json()
          if (mvtSourceData.success) {
            allMvtSource.value = mvtSourceData.data.liste || []
          }
        }
      } catch (error) {
        console.error('Erreur chargement données référence:', error)
      }

      // Forcer la remise à zéro des sélections pour éviter la sélection automatique du premier élément
      mvtStock.value.idTypeMvStock = ''
      // mvtStock.value.idPoint = '' // Garder la sélection par défaut du premier point
      mvtStock.value.idMagasin = ''
    }

    // Génération du mouvement de stock depuis la fabrication
    const generateFromFabrication = async () => {
      const idFab = route.query.idFab
      const idOf = route.query.idOf
      const idTypeMvStock = route.query.idTypeMvStock || 'TPMVST000001'
      const isResidu = route.query.isResidu

      if (!idFab) return

      try {
        console.log('Génération mouvement stock pour:', { idFab, idOf, idTypeMvStock, isResidu })
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'genererMvtStock',
            classe: 'fabrication.Fabrication',
            id: idFab,
            idOf: idOf,
            idTypeMvStock: idTypeMvStock,
            isResidu: isResidu
          })
        })

        if (response.ok) {
          const data = await response.json()
          console.log('Réponse genererMvtStock:', data)
          if (data.success && data.data) {
            mvtStock.value = { ...mvtStock.value, ...data.data }
            // Si le backend a déjà fourni les détails, les utiliser
            if (data.data.details && Array.isArray(data.data.details) && data.data.details.length > 0) {
              console.log('Détails trouvés dans la réponse:', data.data.details.length)
              // Mapper les détails pour ne garder que les propriétés métier
              mvtStock.value.details = data.data.details.map(detail => ({
                idProduit: detail.idProduit || '',
                designation: detail.designation || '',
                entree: detail.entree || 0,
                sortie: detail.sortie || 0,
                pu: detail.pu || 0,
                mvtSrc: detail.mvtSrc || '',
              }))
            } else {
              // Sinon, charger les détails depuis la fabrication
              console.log('Pas de détails dans la réponse, chargement depuis fabrication')
              await loadFabricationDetails(idFab)
            }
          }
        } else {
          console.error('Erreur HTTP genererMvtStock:', response.status, await response.text())
        }
      } catch (error) {
        console.error('Erreur génération mouvement:', error)
      }
    }

    // Chargement des détails depuis la fabrication
    const loadFabricationDetails = async (idFab) => {
      try {
        console.log('Chargement détails fabrication pour:', idFab)
        const requestBody = {
          acte: 'liste',
          classe: 'fabrication.FabricationFilleCpl',
          nomTable: 'FABRICATIONFILLECPL',
          idMere: idFab
        }
        console.log('Request body:', requestBody)
        
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        })

        console.log('Response status:', response.status)
        if (response.ok) {
          const data = await response.json()
          console.log('Response data:', data)
          if (data.success && data.data.liste) {
            // Convertir les détails de fabrication en détails de mouvement
            mvtStock.value.details = data.data.liste.map(fabDetail => ({
              idProduit: fabDetail.idIngredients,
              designation: fabDetail.idingredientsLib,
              entree: mvtStock.value.idTypeMvStock === 'TPMVST000001' ? fabDetail.qte : 0,
              sortie: mvtStock.value.idTypeMvStock === 'TPMVST000022' ? fabDetail.qte : 0,
              pu: fabDetail.pu || 0,
              mvtSrc: ''
            }))
            console.log('Détails chargés:', mvtStock.value.details.length)
          } else {
            // Si pas de liste, initialiser un tableau vide
            console.warn('Aucun détail trouvé pour la fabrication:', idFab)
            mvtStock.value.details = []
          }
        } else {
          // En cas d'erreur HTTP, initialiser un tableau vide
          const errorText = await response.text()
          console.warn('Erreur HTTP lors du chargement des détails:', response.status, errorText)
          mvtStock.value.details = []
        }
      } catch (error) {
        console.error('Erreur chargement détails fabrication:', error)
        // Initialiser un tableau vide en cas d'erreur
        mvtStock.value.details = []
      }
    }

    // Gestionnaires d'événements
    const onPointChange = () => {
      // Filtrer les magasins par point sélectionné
      if (mvtStock.value.idPoint) {
        // Pour l'instant, on garde tous les magasins (le filtrage côté serveur n'est pas implémenté)
        // magasins.value = allMagasins.value.filter(mag => mag.idPoint === mvtStock.value.idPoint)
      } else {
        // Remettre tous les magasins si aucun point sélectionné
        // magasins.value = allMagasins.value
      }
      // Remettre le magasin à vide quand le point change
      mvtStock.value.idMagasin = ''
    }

    // Chargement d'un mouvement existant pour modification
    const loadExistingMvtStock = async () => {
      const id = route.query.id
      const acte = route.query.acte

      if (!id || acte !== 'update') return

      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'consulte',
            classe: 'stock.MvtStock',
            nomTable: 'MVTSTOCK',
            id: id
          })
        })

        if (response.ok) {
          const result = await response.json()
          if (result.success && result.data) {
            mvtStock.value = result.data
          }
        }
      } catch (error) {
        console.error('Erreur chargement mouvement existant:', error)
      }
    }

    const addDetail = () => {
      mvtStock.value.details.push({
        idProduit: '',
        designation: '',
        entree: 0,
        sortie: 0,
        pu: 0,
        mvtSrc: ''
      })
    }

    const removeDetail = (index) => {
      mvtStock.value.details.splice(index, 1)
    }

    const onProduitSelect = (detail, produit) => {
      detail.designation = produit.libelle || produit.designation || ''
      detail.pu = produit.pu || 0
    }

    const onMvtSourceSelect = (detail, mvtSource) => {
      detail.pu = mvtSource.pu || detail.pu
    }

    // Fonctions de recherche pour autocomplete
    const searchFabrications = async (query) => {
      if (!query) return allFabrications.value.slice(0, 10)
      return allFabrications.value.filter(fab =>
        fab.libelle?.toLowerCase().includes(query.toLowerCase()) ||
        fab.id?.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 10)
    }

    const searchProduits = async (query) => {
      if (!query) return allProduits.value.slice(0, 10)
      return allProduits.value.filter(prod =>
        prod.libelle?.toLowerCase().includes(query.toLowerCase()) ||
        prod.id?.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 10)
    }

    const searchMvtSource = async (query) => {
      // Filtrer les mouvements source selon le magasin et la recherche
      let filtered = allMvtSource.value
      
      // Filtrer par magasin si sélectionné
      if (mvtStock.value.idMagasin) {
        filtered = filtered.filter(item => item.idmagasin === mvtStock.value.idMagasin)
      }
      
      // Filtrer par query si fourni
      if (query) {
        filtered = filtered.filter(item =>
          item.designation?.toLowerCase().includes(query.toLowerCase()) ||
          item.id?.toLowerCase().includes(query.toLowerCase())
        )
      }
      
      return filtered.slice(0, 10) // Limiter à 10 résultats
    }

    // Actions
    const sauvegarder = async () => {
      try {
        const acte = mvtStock.value.id ? 'update' : 'insert'
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: acte,
            classe: 'stock.MvtStock',
            classeFille: 'stock.MvtStockFille',
            nomTable: 'MVTSTOCK',
            nomTableFille: 'MVTSTOCKFILLE',
            colonneMere: 'idMvtStock',
            mere: mvtStock.value,
            filles: mvtStock.value.details
          })
        })

        const result = await response.json()
        if (result.success) {
          alert('Mouvement de stock enregistré avec succès')
          router.push('/stock/mvtstock/fiche/' + result.data.id)
        } else {
          alert('Erreur lors de la sauvegarde: ' + (result.message || 'Erreur inconnue'))
        }
      } catch (error) {
        console.error('Erreur sauvegarde:', error)
        alert('Erreur de connexion: ' + error.message)
      }
    }

    const goBack = () => {
      router.go(-1)
    }

    // Watch pour mettre à jour le titre
    watch(() => route.query.acte, (newActe) => {
      if (newActe === 'update') {
        titre.value = 'Modification de mouvement de stock'
      } else {
        titre.value = 'Saisie de mouvement de stock'
      }
    })

    // Watch pour le type de mouvement
    watch(() => mvtStock.value.idTypeMvStock, (newType) => {
      mvtStock.value.details.forEach(detail => {
        if (newType === 'TPMVST000001') { // Entrée
          detail.sortie = 0
        } else if (newType === 'TPMVST000022') { // Sortie
          detail.entree = 0
        }
      })
    })

    onMounted(async () => {
      await loadReferenceData()

      // Gérer les paramètres URL
      const query = route.query

      if (query.acte === 'update' && query.id) {
        await loadExistingMvtStock()
      } else if (query.idFab) {
        await generateFromFabrication()
      }

      if (query.idTypeMvStock) {
        mvtStock.value.idTypeMvStock = query.idTypeMvStock
      }

      if (query.idOf) {
        // Logique pour ordre de fabrication
      }

      loading.value = false
    })

    return {
      loading,
      titre,
      mvtStock,
      typesMvtStock,
      points,
      magasins,
      allFabrications,
      allProduits,
      allMvtSource,
      canValidate,
      showFabPrecedent,
      onPointChange,
      addDetail,
      removeDetail,
      onProduitSelect,
      onMvtSourceSelect,
      searchFabrications,
      searchProduits,
      searchMvtSource,
      sauvegarder,
      goBack
    }
  }
}
</script>

<style scoped>
.container {
  max-width: 1400px;
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

.btn-secondary {
  background-color: #6b7280;
  color: white;
}

.btn-secondary:hover {
  background-color: #4b5563;
}

.btn-danger {
  background-color: #dc2626;
  color: white;
}

.btn-danger:hover {
  background-color: #b91c1c;
}

.btn-outline {
  background-color: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-outline:hover {
  background-color: #f9fafb;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.form-field label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.25rem;
}

.form-input {
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  background-color: white !important;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-input:disabled {
  background-color: white !important;
  cursor: not-allowed;
}

.table-container {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border-radius: 0.375rem;
  border: 1px solid #e5e7eb;
}

.details-table {
  width: 100%;
  min-width: 800px;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.details-table th {
  padding: 0.75rem 1rem;
  background-color: #f9fafb;
  text-align: left;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}

.details-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  vertical-align: middle;
}

.details-table td .form-input {
  width: 100%;
  min-width: 80px;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .details-table th,
  .details-table td {
    padding: 0.5rem 0.75rem;
    font-size: 0.8rem;
  }

  .details-table td .form-input {
    font-size: 0.8rem;
    padding: 0.375rem 0.5rem;
  }
}

.mt-4 {
  margin-top: 1rem;
}
</style>