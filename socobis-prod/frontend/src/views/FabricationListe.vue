<template>
  <div class="fabrication-liste-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">Liste des Fabrications</h1>
      </div>
      <div class="header-actions">
        <button @click="nouvelleFabrication" class="btn-primary">Nouvelle Fabrication</button>
      </div>
    </div>

    <!-- Filtres -->
    <div class="filters-section">
      <form @submit.prevent="rechercher" class="filters-form">
        <div class="form-grid">
          <div class="form-group">
            <label class="form-label">Date min</label>
            <input
              v-model="filtres.daty1"
              type="date"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label class="form-label">Date max</label>
            <input
              v-model="filtres.daty2"
              type="date"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label class="form-label">ID Ordre de fabrication</label>
            <input
              v-model="filtres.idOf"
              type="text"
              class="form-input"
              placeholder="ID OF"
            />
          </div>
          <div class="form-group">
            <label class="form-label">ID Ordre de fabrication fille</label>
            <input
              v-model="filtres.idOffille"
              type="text"
              class="form-input"
              placeholder="ID OFFille"
            />
          </div>
          <div class="form-group">
            <label class="form-label">État</label>
            <select v-model="filtres.etat" class="form-input" @change="rechercher">
              <option value="FABRICATIONCPL">Tous</option>
              <option value="FABRICATIONCPLCREE">Créée(s)</option>
              <option value="FABRICATIONCPLVISEE">Validée(s)</option>
              <option value="FABRICATIONCPLANNULE">Annulée(s)</option>
              <option value="FABRICATIONCPLENTAMEE">Entamée(s)</option>
              <option value="FABRICATIONCPLBLOQUEE">Bloquée(s)</option>
              <option value="FABRICATIONCPLBTERMINEE">Terminée(s)</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Lancée par</label>
            <input
              v-model="filtres.lanceparLib"
              type="text"
              class="form-input"
              placeholder="Lancée par"
            />
          </div>
          <div class="form-group">
            <label class="form-label">Cible</label>
            <input
              v-model="filtres.cible"
              type="text"
              class="form-input"
              placeholder="Cible"
            />
          </div>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn-primary">Rechercher</button>
          <button type="button" @click="resetFiltres" class="btn-outline">Réinitialiser</button>
        </div>
      </form>
    </div>

    <!-- Résultats -->
    <div class="results-section">
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>Chargement...</p>
      </div>

      <div v-else-if="fabrications.length === 0" class="no-results">
        <p>Aucune fabrication trouvée</p>
      </div>

      <div v-else>
        <!-- Tableau -->
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Lancée par</th>
                <th>Cible</th>
                <th>Remarque</th>
                <th>Désignation</th>
                <th>Date</th>
                <th>ID OF</th>
                <th>ID OFFille</th>
                <th>État</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="fab in fabrications"
                :key="fab.id"
                @click="voirFiche(fab.id)"
                class="clickable-row"
              >
                <td>{{ fab.id }}</td>
                <td>{{ fab.lanceparLib }}</td>
                <td>{{ fab.cibleLib }}</td>
                <td>{{ fab.remarque }}</td>
                <td>{{ fab.libelle }}</td>
                <td>{{ formatDate(fab.daty) }}</td>
                <td>
                  <a
                    v-if="fab.idOf"
                    @click.stop="voirOrdreFabrication(fab.idOf)"
                    class="link"
                  >
                    {{ fab.idOf }}
                  </a>
                  <span v-else>-</span>
                </td>
                <td>
                  <a
                    v-if="fab.idOffille"
                    @click.stop="voirOrdreFabricationFille(fab.idOffille)"
                    class="link"
                  >
                    {{ fab.idOffille }}
                  </a>
                  <span v-else>-</span>
                </td>
                <td>{{ fab.etatLib }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="pagination">
          <button
            @click="pagePrecedente"
            :disabled="page === 1"
            class="btn-outline"
          >
            Précédent
          </button>
          <span>Page {{ page }} sur {{ totalPages }}</span>
          <button
            @click="pageSuivante"
            :disabled="page === totalPages"
            class="btn-outline"
          >
            Suivant
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'FabricationListe',
  setup() {
    const router = useRouter()
    const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010/socobis/api'

    const fabrications = ref([])
    const loading = ref(false)
    const page = ref(1)
    const totalPages = ref(1)

    const filtres = ref({
      daty1: '',
      daty2: '',
      idOf: '',
      idOffille: '',
      etat: 'FABRICATIONCPL',
      lanceparLib: '',
      cible: ''
    })

    const loadFabrications = async () => {
      loading.value = true
      try {
        // Construire le payload JSON
        const payload = {
          acte: 'liste',
          classe: 'fabrication.FabricationCpl',
          nomTable: filtres.value.etat
        }

        // Ajouter les filtres
        if (filtres.value.daty1) payload.daty1 = filtres.value.daty1
        if (filtres.value.daty2) payload.daty2 = filtres.value.daty2
        if (filtres.value.idOf) payload.idOf = filtres.value.idOf
        if (filtres.value.idOffille) payload.idOffille = filtres.value.idOffille
        if (filtres.value.lanceparLib) payload.lanceparLib = filtres.value.lanceparLib
        if (filtres.value.cible) payload.cible = filtres.value.cible

        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify(payload)
        })

        if (response.ok) {
          const data = await response.json()
          if (data.success && data.data && data.data.liste) {
            fabrications.value = data.data.liste
            totalPages.value = Math.ceil(data.data.total / 20) || 1 // Assuming 20 per page
          }
        }
      } catch (error) {
        console.error('Erreur chargement fabrications:', error)
      } finally {
        loading.value = false
      }
    }

    const rechercher = () => {
      page.value = 1
      loadFabrications()
    }

    const resetFiltres = () => {
      filtres.value = {
        daty1: '',
        daty2: '',
        idOf: '',
        idOffille: '',
        etat: 'FABRICATIONCPL',
        lanceparLib: '',
        cible: ''
      }
      rechercher()
    }

    const pagePrecedente = () => {
      if (page.value > 1) {
        page.value--
        loadFabrications()
      }
    }

    const pageSuivante = () => {
      if (page.value < totalPages.value) {
        page.value++
        loadFabrications()
      }
    }

    const voirFiche = (id) => {
      router.push(`/fabrication/fiche/${id}`)
    }

    const voirOrdreFabrication = (id) => {
      router.push(`/ordre-fabrication/fiche/${id}`)
    }

    const voirOrdreFabricationFille = (id) => {
      router.push(`/ordre-fabrication/details/${id}`)
    }

    const nouvelleFabrication = () => {
      router.push('/fabrication/saisie')
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return '-'
      return new Date(dateStr).toLocaleDateString('fr-FR')
    }

    onMounted(() => {
      // Set default dates (current week)
      const today = new Date()
      const monday = new Date(today)
      monday.setDate(today.getDate() - today.getDay() + 1)
      const sunday = new Date(today)
      sunday.setDate(today.getDate() - today.getDay() + 7)

      filtres.value.daty1 = monday.toISOString().split('T')[0]
      filtres.value.daty2 = sunday.toISOString().split('T')[0]

      loadFabrications()
    })

    return {
      fabrications,
      loading,
      page,
      totalPages,
      filtres,
      rechercher,
      resetFiltres,
      pagePrecedente,
      pageSuivante,
      voirFiche,
      voirOrdreFabrication,
      voirOrdreFabricationFille,
      nouvelleFabrication,
      formatDate
    }
  }
}
</script>

<style scoped>
.fabrication-liste-container {
  max-width: 80rem;
  margin: 0 auto;
  padding: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #111827;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-primary {
  padding: 0.5rem 1rem;
  background-color: #2563eb;
  color: white;
  border-radius: 0.5rem;
  transition: background-color 0.2s;
}

.btn-primary:hover {
  background-color: #1d4ed8;
}

.btn-outline {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 0.5rem;
  color: #374151;
  background-color: white;
  transition: background-color 0.2s;
}

.btn-outline:hover {
  background-color: #f9fafb;
}

.filters-section {
  background-color: white;
  padding: 1.5rem;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
  margin-bottom: 1.5rem;
}

.filters-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  margin-bottom: 0.25rem;
}

.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.5rem;
  transition: border-color 0.2s, box-shadow 0.2s;
  background-color: white !important;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  padding-top: 1rem;
}

.results-section {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.spinner {
  width: 2rem;
  height: 2rem;
  border: 4px solid #dbeafe;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.no-results {
  text-align: center;
  padding: 3rem;
  color: #6b7280;
}

.table-container {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  min-width: 800px;
}

.data-table th {
  padding: 0.75rem 1.5rem;
  background-color: #f9fafb;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #e5e7eb;
}

.data-table td {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #f3f4f6;
  font-size: 0.875rem;
  color: #111827;
}

.clickable-row {
  cursor: pointer;
  transition: background-color 0.2s;
}

.clickable-row:hover {
  background-color: #f9fafb;
}

.link {
  color: #2563eb;
  text-decoration: underline;
  transition: color 0.2s;
}

.link:hover {
  color: #1d4ed8;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  background-color: #f9fafb;
  border-top: 1px solid #e5e7eb;
}
</style>