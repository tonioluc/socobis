<template>
  <div class="ordre-fabrication-saisie-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">Saisie d'un ordre de fabrication</h1>
      </div>
      <div class="header-actions">
        <button @click="annuler" class="btn-outline">Annuler</button>
        <button @click="enregistrer" class="btn-primary">Enregistrer</button>
      </div>
    </div>

    <form @submit.prevent="enregistrer" class="ordre-fabrication-form">
      <!-- Informations générales -->
      <div class="form-section">
        <h3 class="section-title">Informations générales</h3>
        <div class="form-grid">
          <div class="form-group">
            <label class="form-label">Date *</label>
            <input
              v-model="ordreFabrication.daty"
              type="date"
              class="form-input"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Cible *</label>
            <select v-model="ordreFabrication.cible" class="form-input" required>
              <option v-for="mag in magasins" :key="mag.id" :value="mag.id">{{ mag.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Désignation</label>
            <input
              v-model="ordreFabrication.libelle"
              type="text"
              class="form-input"
              placeholder="Désignation de l'ordre de fabrication"
            />
          </div>
          <div class="form-group">
            <label class="form-label">Date de besoin *</label>
            <input
              v-model="ordreFabrication.besoin"
              type="date"
              class="form-input"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Numéro de bon de commande</label>
            <AutoComplete
              v-model="ordreFabrication.idBc"
              :search-function="rechercherBonDeCommande"
              value-field="id"
              display-field="label"
              placeholder="Rechercher un bon de commande..."
              name="idBc"
            />
          </div>
          <div class="form-group col-span-2">
            <label class="form-label">Remarque</label>
            <textarea
              v-model="ordreFabrication.remarque"
              class="form-input"
              rows="3"
              placeholder="Remarques..."
            ></textarea>
          </div>
        </div>
      </div>

      <!-- Détails des produits -->
      <div class="form-section">
        <div class="section-header">
          <h3 class="section-title">Détails des produits</h3>
          <button @click="ajouterLigne" type="button" class="btn-secondary">
            <Plus class="w-4 h-4" />
            Ajouter un produit
          </button>
        </div>

        <div class="table-container">
          <table class="details-table">
            <thead>
              <tr>
                <th>Produits</th>
                <th>Libellé</th>
                <th>Remarque</th>
                <th>Unité</th>
                <th>Quantité</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(detail, index) in ordreFabrication.details" :key="index">
                <td>
                  <AutoComplete
                    v-model="detail.idIngredients"
                    :search-function="rechercherIngredients"
                    value-field="id"
                    display-field="label"
                    placeholder="ID ou nom..."
                    :show-selected-badge="false"
                    input-class="table-input-autocomplete"
                    @select="(item) => onIngredientSelect(item, index)"
                  />
                </td>
                <td>
                  <input
                    v-model="detail.libelle"
                    type="text"
                    class="table-input table-input-sm"
                    readonly
                  />
                </td>
                <td>
                  <input
                    v-model="detail.remarque"
                    type="text"
                    class="table-input table-input-sm"
                    placeholder="Remarque..."
                  />
                </td>
                <td>
                  <input
                    v-model="detail.idunite"
                    type="text"
                    class="table-input table-input-xs"
                    readonly
                  />
                </td>
                <td>
                  <input
                    v-model="detail.qte"
                    type="number"
                    min="1"
                    step="1"
                    class="table-input table-input-xs"
                  />
                </td>
                <td>
                  <button @click="supprimerLigne(index)" type="button" class="delete-btn">
                    <Trash2 class="w-4 h-4" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Bouton pour ajouter une ligne -->
          <div class="mt-4">
            <button @click="ajouterLigne" type="button" class="add-line-btn">
              <Plus class="w-4 h-4 mr-2" />
              Ajouter une ligne
            </button>
          </div>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Trash2 } from 'lucide-vue-next'
import AutoComplete from '@/components/AutoComplete.vue'
import {
  rechercherBonDeCommande,
  rechercherIngredients,
  getListeMagasins
} from '@/services/autocompleteService'

const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010/socobis/api'

// État de l'ordre de fabrication
const ordreFabrication = ref({
  daty: new Date().toISOString().split('T')[0],
  cible: null,
  libelle: '',
  besoin: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // +7 jours
  idBc: null,
  remarque: '',
  details: Array.from({ length: 10 }, () => ({
    idIngredients: null,
    libelle: '',
    remarque: '',
    idunite: '',
    qte: 1
  }))
})

// Liste des magasins
const magasins = ref([])

// Charger les magasins
const fetchMagasins = async () => {
  try {
    magasins.value = await getListeMagasins()

    if (magasins.value.length > 0) {
      ordreFabrication.value.cible = magasins.value[0].id
    }
  } catch (error) {
    console.error('Erreur lors du chargement des magasins:', error)
  }
}

// Handler pour la sélection d'ingrédient
// Le champ retour contient: pu;compte_vente;tva;unite;image;durre
const onIngredientSelect = (ingredient, index) => {
  if (ordreFabrication.value.details[index]) {
    ordreFabrication.value.details[index].idIngredients = ingredient.id || ingredient.value || null
    // La designation est le libelle de l'ingrédient (sans l'ID)
    ordreFabrication.value.details[index].libelle = ingredient.designation || ingredient.label || ''

    // Parser le champ retour: pu;compte_vente;tva;unite;image;durre
    if (ingredient.retour) {
      const parts = ingredient.retour.split(';')
      ordreFabrication.value.details[index].idunite = parts[3] || '' // unite
    }
  }
  console.log('Ingrédient sélectionné:', ingredient)
}

// Ajouter une ligne de détail
const ajouterLigne = () => {
  ordreFabrication.value.details.push({
    idIngredients: null,
    libelle: '',
    remarque: '',
    idunite: '',
    qte: 1
  })
}

// Supprimer une ligne
const supprimerLigne = (index) => {
  ordreFabrication.value.details.splice(index, 1)
}

// Actions
const enregistrer = async () => {
  // Validation
  if (!ordreFabrication.value.daty || !ordreFabrication.value.cible || !ordreFabrication.value.besoin) {
    alert('Veuillez remplir les champs obligatoires (Date, Cible, Date de besoin)')
    return
  }

  // Filtrer les détails qui ont un ingrédient sélectionné
  const detailsValides = ordreFabrication.value.details.filter(d => d.idIngredients && d.idIngredients.trim() !== '')

  if (detailsValides.length === 0) {
    alert('Veuillez ajouter au moins un produit')
    return
  }

  // Préparer les données pour le servlet ApresMultiple générique
  const payload = {
    acte: 'insert',
    classe: 'fabrication.Of',
    classeFille: 'fabrication.OfFille',
    colonneMere: 'idMere',
    bute: 'fabrication/ordre-fabrication-fiche.jsp',
    mere: {
      daty: ordreFabrication.value.daty,
      cible: ordreFabrication.value.cible,
      libelle: ordreFabrication.value.libelle || '',
      besoin: ordreFabrication.value.besoin,
      idBc: ordreFabrication.value.idBc || '',
      remarque: ordreFabrication.value.remarque || '',
      lancePar: '', // Sera défini côté serveur
      etat: 0 // Sera défini côté serveur
    },
    filles: detailsValides.map((detail, index) => ({
      idIngredients: detail.idIngredients,
      libelle: detail.libelle || '',
      remarque: detail.remarque || '',
      qte: detail.qte || 1,
      idunite: detail.idunite || '',
      idBcFille: '', // Sera défini côté serveur si idBc est fourni
      idmere: '', // Sera défini côté serveur
      datybesoin: ordreFabrication.value.besoin
    }))
  }

  console.log('Envoi des données au serveur:', payload)

  try {
    // Utiliser l'endpoint générique /api/apresmultiple
    const response = await fetch(`${API_BASE}/apresmultiple`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    const result = await response.json()
    console.log('Réponse du serveur:', result)

    if (result.success && result.data && result.data.id) {
      // Succès - afficher un message et rediriger
      alert('Ordre de fabrication créé avec succès! ID: ' + result.data.id)

      // Redirection vers la fiche de l'ordrep
      router.push(`/ordre-fabrication/fiche/${result.data.id}`)
    } else {
      alert('Erreur: ' + (result.error || 'Erreur inconnue'))
    }
  } catch (error) {
    console.error('Erreur lors de l\'enregistrement:', error)
    alert('Erreur de connexion au serveur: ' + error.message)
  }
}

const annuler = () => {
  router.push('/ordre-fabrication/liste')
}

// Initialisation
onMounted(() => {
  fetchMagasins()
  // Si un idBc est passé en paramètre, le pré-remplir
  const urlParams = new URLSearchParams(window.location.search)
  const idBc = urlParams.get('idBc')
  if (idBc) {
    ordreFabrication.value.idBc = idBc
    ordreFabrication.value.libelle = `Ordre de fabrication de ${idBc}`
  }
})
</script>

<style scoped>
.ordre-fabrication-saisie-container {
  @apply p-6 max-w-7xl mx-auto space-y-6 bg-white min-h-screen;
  background-color: white !important;
}

.page-header {
  @apply flex justify-between items-start mb-6;
}

.page-title {
  @apply text-3xl font-bold text-gray-900 mb-2;
}

.header-actions {
  @apply flex gap-3;
}

.btn-primary {
  @apply bg-blue-600 text-white px-6 py-2 rounded-lg font-medium hover:bg-blue-700 transition-colors;
}

.btn-outline {
  @apply border border-gray-300 text-gray-700 px-6 py-2 rounded-lg font-medium hover:bg-gray-50 transition-colors;
}

.btn-secondary {
  @apply bg-gray-100 text-gray-700 px-4 py-2 rounded-lg font-medium hover:bg-gray-200 transition-colors flex items-center gap-2;
}

.ordre-fabrication-form {
  @apply space-y-8;
}

.form-section {
  @apply bg-white p-6 rounded-xl shadow-sm border border-gray-200;
}

.section-title {
  @apply text-xl font-semibold text-gray-900 mb-4;
}

.section-header {
  @apply flex justify-between items-center mb-4;
}

.form-grid {
  @apply grid grid-cols-1 md:grid-cols-2 gap-6;
}

.form-group {
  @apply space-y-2;
}

.form-group.col-span-2 {
  /* col-span-2 already applied in template */
}

.form-label {
  @apply block text-sm font-medium text-gray-700;
}

.form-input {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent;
  background-color: white !important;
}

.form-input[readonly] {
  @apply bg-gray-50 cursor-not-allowed;
}

textarea.form-input {
  @apply resize-y;
}

.table-container {
  @apply overflow-x-auto;
}

.details-table {
  @apply w-full min-w-max;
}

.table-input {
  @apply w-full px-2 py-1 border border-gray-300 rounded focus:ring-1 focus:ring-blue-500 focus:border-transparent;
  font-size: 13px;
  background-color: white !important;
}

.table-input-sm {
  min-width: 120px;
  max-width: 180px;
}

.table-input-xs {
  width: 70px;
  min-width: 60px;
  text-align: right;
}

.table-input[type="number"] {
  -webkit-appearance: none;
  -moz-appearance: textfield;
}

.table-input[type="number"]::-webkit-outer-spin-button,
.table-input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
  opacity: 0.8;
  cursor: pointer;
}

.table-input[type="number"]:hover::-webkit-outer-spin-button,
.table-input[type="number"]:focus::-webkit-inner-spin-button {
  opacity: 1;
}

/* Style pour l'autocomplete dans le tableau */
.table-input-autocomplete {
  @apply w-full px-1 py-1 border border-gray-300 rounded focus:ring-1 focus:ring-blue-500 focus:border-transparent;
  font-size: 11px;
  min-width: 120px;
  background-color: white !important;
}

.details-table td {
  @apply px-1 py-2 border-b border-gray-200;
}

.details-table th {
  @apply px-1 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border-b;
  font-size: 10px;
}

.delete-btn {
  @apply p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors;
}

.add-line-btn {
  @apply bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700 transition-colors flex items-center;
}
</style>