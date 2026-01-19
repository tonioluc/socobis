<template>
  <div class="fabrication-saisie-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">Saisie d'une fabrication</h1>
      </div>
      <div class="header-actions">
        <button @click="annuler" class="btn-outline">Annuler</button>
        <button @click="enregistrer" class="btn-primary">Enregistrer</button>
      </div>
    </div>

    <form @submit.prevent="enregistrer" class="fabrication-form">
      <!-- Informations générales -->
      <div class="form-section">
        <h3 class="section-title">Informations générales</h3>
        <div class="form-grid">
          <div class="form-group">
            <label class="form-label">Date *</label>
            <input
              v-model="fabrication.daty"
              type="date"
              class="form-input"
              required
            />
          </div>
          <div class="form-group">
            <label class="form-label">Lancée par *</label>
            <select v-model="fabrication.lancePar" class="form-input" required>
              <option v-for="mag in magasins" :key="mag.id" :value="mag.id">{{ mag.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Cible *</label>
            <select v-model="fabrication.cible" class="form-input" required>
              <option v-for="mag in magasins" :key="mag.id" :value="mag.id">{{ mag.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Désignation</label>
            <input
              v-model="fabrication.libelle"
              type="text"
              class="form-input"
              placeholder="Désignation de la fabrication"
            />
          </div>
          <div class="form-group">
            <label class="form-label">Remarque</label>
            <textarea
              v-model="fabrication.remarque"
              class="form-input"
              rows="3"
              placeholder="Remarques"
            ></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">Bon de commande</label>
            <AutoComplete
              v-model="fabrication.idBc"
              :search-function="rechercherBonCommande"
              value-field="id"
              display-field="label"
              placeholder="Rechercher un bon de commande"
              input-class="form-input"
            />
          </div>
          <div class="form-group">
            <label class="form-label">Ordre de fabrication associé</label>
            <AutoComplete
              v-model="fabrication.idOffille"
              :search-function="rechercherOrdreFabrication"
              value-field="id"
              display-field="label"
              placeholder="Rechercher un ordre de fabrication"
              input-class="form-input"
            />
          </div>
          <div class="form-group">
            <label class="form-label">Équipe</label>
            <input
              v-model="fabrication.equipe"
              type="text"
              class="form-input"
              placeholder="Équipe"
            />
          </div>
        </div>
      </div>

      <!-- Détails des composants -->
      <div class="form-section">
        <h3 class="section-title">Composants</h3>
        <div class="table-container">
          <table class="details-table">
            <thead>
              <tr>
                <th>Composants</th>
                <th>Remarque</th>
                <th>Quantité</th>
                <th>Unité</th>
                <th>Bon de commande fille</th>
                <th>Machine</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(composant, index) in fabrication.composants" :key="index">
                <td>
                  <AutoComplete
                    v-model="composant.idIngredients"
                    :search-function="rechercherIngredient"
                    :all-items="ingredients"
                    value-field="id"
                    display-field="label"
                    placeholder="Sélectionner un ingrédient"
                    input-class="table-input"
                    @input="onIngredientChange(index)"
                  />
                </td>
                <td>
                  <input
                    v-model="composant.remarque"
                    type="text"
                    class="table-input table-input-sm"
                    placeholder="Remarque"
                  />
                </td>
                <td>
                  <input
                    v-model.number="composant.qte"
                    type="number"
                    class="table-input table-input-xs"
                    step="0.01"
                    min="0"
                  />
                </td>
                <td>
                  <input
                    v-model="composant.idunite"
                    type="text"
                    class="table-input table-input-xs"
                    readonly
                  />
                </td>
                <td>
                  <input
                    v-model="composant.idBcFille"
                    type="text"
                    class="table-input table-input-sm"
                    readonly
                  />
                </td>
                <td>
                  <select v-model="composant.idMachine" class="table-input table-input-sm">
                    <option v-for="mach in machines" :key="mach.id" :value="mach.id">{{ mach.label }}</option>
                  </select>
                </td>
                <td>
                  <button @click="supprimerComposant(index)" class="btn-danger btn-sm">Supprimer</button>
                </td>
              </tr>
            </tbody>
          </table>
          <button @click="ajouterComposant" class="btn-secondary mt-4">Ajouter un composant</button>
        </div>
      </div>
    </form>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AutoComplete from '../components/AutoComplete.vue'

export default {
  name: 'FabricationSaisie',
  components: {
    AutoComplete
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8010/socobis/api'

    const fabrication = ref({
      daty: new Date().toISOString().split('T')[0],
      lancePar: '',
      cible: '',
      libelle: '',
      remarque: '',
      idBc: '',
      idOffille: '',
      equipe: '',
      composants: Array.from({ length: 10 }, () => ({
        idIngredients: '',
        remarque: '',
        qte: 0,
        idunite: '',
        idBcFille: '',
        idMachine: ''
      }))
    })

    const magasins = ref([])
    const bonCommandes = ref([])
    const ordreFabrications = ref([])
    const ingredients = ref([])
    const machines = ref([])

    const loadMagasins = async () => {
      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'magasin.Magasin',
            nomTable: 'MAGASINPOINT'
          })
        })
        if (response.ok) {
          const text = await response.text()
          if (text.trim()) {
            const data = JSON.parse(text)
            if (data.success && data.data && data.data.liste) {
              magasins.value = data.data.liste.map(item => ({ id: item.id, label: item.val }))
              // Définir des valeurs par défaut si disponibles
              if (magasins.value.length > 0 && !fabrication.value.lancePar) {
                fabrication.value.lancePar = magasins.value[0].id
              }
              if (magasins.value.length > 0 && !fabrication.value.cible) {
                fabrication.value.cible = magasins.value[0].id
              }
            }
          }
        }
      } catch (error) {
        console.error('Erreur chargement magasins:', error)
      }
    }

    const loadBonCommandes = async () => {
      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'vente.BonDeCommande',
            nomTable: 'BONDECOMMANDE_CLIENT'
          })
        })
        if (response.ok) {
          const text = await response.text()
          if (text.trim()) {
            const data = JSON.parse(text)
            if (data.success && data.data && data.data.liste) {
              bonCommandes.value = data.data.liste.map(item => ({ id: item.id, label: `Bon de commande - ${item.daty || item.date || item.val || item.id}` }))
            }
          }
        }
      } catch (error) {
        console.error('Erreur chargement bon de commandes:', error)
      }
    }

    const loadOrdreFabrications = async () => {
      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'fabrication.OfFilleCpl',
            nomTable: 'OFFILLELIB'
          })
        })
        if (response.ok) {
          const text = await response.text()
          if (text.trim()) {
            const data = JSON.parse(text)
            if (data.success && data.data && data.data.liste) {
              ordreFabrications.value = data.data.liste.map(item => ({ id: item.id, label: `${item.id}` }))
            }
          }
        }
      } catch (error) {
        console.error('Erreur chargement ordre fabrications:', error)
      }
    }

    const loadIngredients = async () => {
      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'produits.IngredientsLib',
            nomTable: 'as_ingredients_lib'
          })
        })
        console.log('Réponse reçue:', response.status, response.ok)
        if (response.ok) {
          const text = await response.text()
          if (text.trim()) {
            const data = JSON.parse(text)
            if (data.success && data.data && data.data.liste) {
              ingredients.value = data.data.liste.map(item => ({ id: item.id, label: item.designation || `${item.id} - ${item.libelle} - ${item.unite}`, unite: item.unite }))
            } else {
              console.log('Pas de liste dans data.data:', data.data)
            }
          } else {
            console.log('Réponse vide')
          }
        } else {
          console.log('Erreur HTTP:', response.status)
        }
      } catch (error) {
        console.error('Erreur chargement ingrédients:', error)
      }
    }

    const loadMachines = async () => {
      try {
        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            acte: 'liste',
            classe: 'machine.Machine',
            nomTable: 'MACHINE'
          })
        })
        if (response.ok) {
          const text = await response.text()
          if (text.trim()) {
            const data = JSON.parse(text)
            if (data.success && data.data && data.data.liste) {
              machines.value = data.data.liste.map(item => ({ id: item.id, label: item.val || `Machine ${item.id}` }))
            }
          }
        }
      } catch (error) {
        console.error('Erreur chargement machines:', error)
      }
    }

    const onBonCommandeChange = async (idBc) => {
      if (idBc) {
        try {
          const response = await fetch(`${API_BASE}/apresmultiple`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              acte: 'getFabFille',
              idBc: idBc
            })
          })
          const data = await response.json()
          if (data.success && data.data && data.data.liste) {
            fabrication.value.composants = data.data.liste.map(item => ({
              idIngredients: item.idIngredients,
              remarque: item.remarque || '',
              qte: item.qte || 0,
              idunite: item.idunite || '',
              idBcFille: item.idBcFille || '',
              idMachine: ''
            }))
            fabrication.value.libelle = `Fabrication du BC ${idBc}`
          }
        } catch (error) {
          console.error('Erreur chargement composants BC:', error)
        }
      }
    }

    const onOrdreFabricationChange = async (idOffille, unParUn = false) => {
      if (idOffille) {
        try {
          console.log('🔵 [DEBUG] onOrdreFabricationChange appelé avec idOffille:', idOffille, 'unParUn:', unParUn)
          const response = await fetch(`${API_BASE}/apresmultiple`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              acte: unParUn ? 'genererFabricationUnParUn' : 'genererFabrication',
              idOffille: idOffille
            })
          })
          const data = await response.json()
          console.log('🟢 [DEBUG] Données reçues de genererFabrication:', JSON.stringify(data, null, 2))
          if (data.success && data.data) {
            console.log('🟢 [DEBUG] data.data.fille:', data.data.fille)
            // Sauvegarder idOffille avant le merge pour éviter qu'il soit écrasé
            const currentIdOffille = idOffille
            fabrication.value = { ...fabrication.value, ...data.data }
            // Forcer idOffille à la bonne valeur
            fabrication.value.idOffille = currentIdOffille
            console.log('🟢 [DEBUG] idOffille après merge:', fabrication.value.idOffille)
            
            if (data.data.fille) {
              console.log('🟡 [DEBUG] Nombre d\'ingrédients avant ajout:', ingredients.value.length)
              fabrication.value.composants = data.data.fille.map((item, index) => {
                console.log(`🟡 [DEBUG] Composant ${index}:`, JSON.stringify(item, null, 2))
                // Ajouter l'ingrédient à la liste s'il n'existe pas déjà
                if (item.idIngredients) {
                  const existingIng = ingredients.value.find(ing => ing.id === item.idIngredients)
                  console.log(`🟡 [DEBUG] Ingrédient ${item.idIngredients} existe déjà?`, !!existingIng)
                  if (!existingIng) {
                    const newIng = {
                      id: item.idIngredients,
                      label: item.designation || item.libelle || `Ingredient ${item.idIngredients}`,
                      unite: item.idunite || ''
                    }
                    console.log(`🟢 [DEBUG] Ajout nouvel ingrédient:`, newIng)
                    ingredients.value.push(newIng)
                  }
                }
                return {
                  idIngredients: item.idIngredients,
                  remarque: item.remarque || '',
                  qte: item.qte || 0,
                  idunite: item.idunite || '',
                  idBcFille: item.idBcFille || '',
                  idMachine: ''
                }
              })
              console.log('🟢 [DEBUG] Nombre d\'ingrédients après ajout:', ingredients.value.length)
              console.log('🟢 [DEBUG] Composants assignés:', fabrication.value.composants)
            }
          }
        } catch (error) {
          console.error('🔴 [DEBUG] Erreur génération fabrication:', error)
        }
      }
    }

    const onIngredientChange = (index) => {
      const composant = fabrication.value.composants[index]
      if (composant.idIngredients) {
        const ingredient = ingredients.value.find(ing => ing.id === composant.idIngredients)
        console.log('Ingredient sélectionné:', composant.idIngredients)
        console.log('Ingredient trouvé:', ingredient)
        if (ingredient) {
          composant.idunite = ingredient.unite
          console.log('Unite assignée:', composant.idunite)
        } else {
          console.log('Ingredient non trouvé')
        }
      }
    }

    const rechercherBonCommande = async (query) => {
      if (query.length < 1) return bonCommandes.value.slice(0, 10)
      return bonCommandes.value.filter(item =>
        (item.label || '').toLowerCase().includes(query.toLowerCase())
      ).slice(0, 10)
    }

    const rechercherOrdreFabrication = async (query) => {
      if (query.length < 1) return ordreFabrications.value.slice(0, 10)
      return ordreFabrications.value.filter(item =>
        (item.label || '').toLowerCase().includes(query.toLowerCase())
      ).slice(0, 10)
    }

    const rechercherIngredient = async (query) => {
      if (query.length < 1) return ingredients.value.slice(0, 10)
      const result = ingredients.value.filter(item =>
        (item.label || '').toLowerCase().includes(query.toLowerCase())
      ).slice(0, 10)
      return result
    }

    const ajouterComposant = () => {
      fabrication.value.composants.push({
        idIngredients: '',
        remarque: '',
        qte: 0,
        idunite: '',
        idBcFille: '',
        idMachine: ''
      })
    }

    const supprimerComposant = (index) => {
      fabrication.value.composants.splice(index, 1)
    }

    const enregistrer = async () => {
      try {
        // Validation des champs obligatoires
        if (!fabrication.value.daty || !fabrication.value.lancePar || !fabrication.value.cible) {
          alert('Veuillez remplir les champs obligatoires (Date, Lancée par, Cible)')
          return
        }

        // Filtrer les composants qui ont au moins un ingrédient sélectionné
        const composantsValides = fabrication.value.composants.filter(comp => 
          comp.idIngredients && comp.idIngredients.trim() !== ''
        )

        if (composantsValides.length === 0) {
          alert('Veuillez ajouter au moins un composant')
          return
        }

        const payload = {
          acte: 'insert',
          classe: 'fabrication.Fabrication',
          classeFille: 'fabrication.FabricationFille',
          colonneMere: 'idMere',
          bute: 'fabrication/fabrication-fiche.jsp',
          mere: {
            daty: fabrication.value.daty,
            lancePar: fabrication.value.lancePar,
            cible: fabrication.value.cible,
            libelle: fabrication.value.libelle,
            remarque: fabrication.value.remarque,
            idBc: fabrication.value.idBc,
            idOffille: fabrication.value.idOffille,
            equipe: fabrication.value.equipe
          },
          filles: composantsValides.map(comp => ({
            idIngredients: comp.idIngredients,
            remarque: comp.remarque,
            qte: comp.qte,
            idunite: comp.idunite,
            idBcFille: comp.idBcFille,
            idMachine: comp.idMachine
          }))
        }

        const response = await fetch(`${API_BASE}/apresmultiple`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        })

        const result = await response.json()
        console.log('Résultat de l\'enregistrement:', result)
        if (result.success) {

          alert('Fabrication créée avec succès')
          router.push(`/fabrication/fiche/${result.data.id}`)
        } else {
          alert('Erreur lors de la création: ' + (result.message || 'Erreur inconnue'))
        }
      } catch (error) {
        console.error('Erreur:', error)
        alert('Erreur de connexion: ' + error.message)
      }
    }

    const annuler = () => {
      router.push('/fabrication/liste')
    }

    onMounted(async () => {
      console.log('onMounted: Chargement des données...')
      await Promise.all([
        loadMagasins(),
        loadBonCommandes(),
        loadOrdreFabrications(),
        loadIngredients(),
        loadMachines()
      ])
      console.log('Données chargées. Ingrédients:', ingredients.value.length)

      // Récupérer les paramètres URL et pré-remplir
      const idBC = route.query.idBC
      if (idBC) {
        fabrication.value.idBc = idBC
        await onBonCommandeChange(idBC)
      }

      const idOffille = route.query.idOffille
      if (idOffille) {
        console.log('🔵 [DEBUG] idOffille détecté dans URL:', idOffille)
        const unParUn = route.query.unParUn === 'true'
        
        // D'abord charger les détails de l'ordre de fabrication pour afficher le libellé dans l'AutoComplete
        try {
          console.log('🔵 [DEBUG] Chargement détails OF...')
          const response = await fetch(`${API_BASE}/apresmultiple`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              acte: 'consulte',
              classe: 'fabrication.OfFilleCpl',
              nomTable: 'OFFILLELIB',
              id: idOffille
            })
          })
          const data = await response.json()
          console.log('🟢 [DEBUG] Détails OF reçus:', JSON.stringify(data, null, 2))
          if (data.success && data.data) {
            // Ajouter l'ordre de fabrication aux options pour que l'AutoComplete puisse l'afficher
            const ofLabel = data.data.libelleexacte || data.data.libelle || `OF ${idOffille}`
            console.log('🟢 [DEBUG] Label OF à afficher:', ofLabel)
            // Vérifier si l'item existe déjà dans la liste
            const existingIndex = ordreFabrications.value.findIndex(of => of.id === idOffille)
            if (existingIndex >= 0) {
              ordreFabrications.value[existingIndex] = { id: idOffille, label: ofLabel }
              console.log('🟡 [DEBUG] OF mis à jour dans la liste à l\'index:', existingIndex)
            } else {
              ordreFabrications.value.unshift({ id: idOffille, label: ofLabel })
              console.log('🟢 [DEBUG] OF ajouté au début de la liste')
            }
            console.log('🟢 [DEBUG] Liste ordreFabrications:', ordreFabrications.value)
          }
        } catch (error) {
          console.error('🔴 [DEBUG] Erreur chargement détails OF:', error)
        }
        
        // Ensuite générer la fabrication (qui ajoutera automatiquement les ingrédients à la liste)
        console.log('🔵 [DEBUG] Appel onOrdreFabricationChange...')
        await onOrdreFabricationChange(idOffille, unParUn)
        
        // Assigner idOffille après avoir chargé les données
        fabrication.value.idOffille = idOffille
        console.log('🟢 [DEBUG] idOffille final assigné:', fabrication.value.idOffille)
        console.log('🟢 [DEBUG] Liste finale des ingrédients:', ingredients.value.slice(0, 5))
      }

      const designation = route.query.designation
      if (designation) {
        fabrication.value.libelle = designation
      }

      // Définir la machine par défaut (6ème élément) pour les composants non pré-remplis
      if (machines.value.length > 5) {
        fabrication.value.composants.forEach(comp => {
          if (!comp.idMachine) {
            comp.idMachine = machines.value[5].id
          }
        })
      }
    })

    // Watcher pour mettre à jour l'unité quand un ingrédient est sélectionné
    watch(() => fabrication.value.composants, (newComposants) => {
      newComposants.forEach((comp, index) => {
        if (comp.idIngredients && ingredients.value.length > 0) {
          const ingredient = ingredients.value.find(ing => ing.id === comp.idIngredients)
          if (ingredient && (!comp.idunite || comp.idunite !== ingredient.unite)) {
            comp.idunite = ingredient.unite
            console.log(`Unité mise à jour pour composant ${index}: ${comp.idunite}`)
          }
        }
      })
    }, { deep: true })

    return {
      fabrication,
      magasins,
      bonCommandes,
      ordreFabrications,
      ingredients,
      machines,
      rechercherBonCommande,
      rechercherOrdreFabrication,
      rechercherIngredient,
      onBonCommandeChange,
      onOrdreFabricationChange,
      onIngredientChange,
      ajouterComposant,
      supprimerComposant,
      enregistrer,
      annuler
    }
  }
}
</script>

<style scoped>
.fabrication-saisie-container {
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

.btn-secondary {
  padding: 0.5rem 1rem;
  background-color: #4b5563;
  color: white;
  border-radius: 0.5rem;
  transition: background-color 0.2s;
}

.btn-secondary:hover {
  background-color: #374151;
}

.btn-danger {
  padding: 0.25rem 0.5rem;
  background-color: #dc2626;
  color: white;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  transition: background-color 0.2s;
}

.btn-danger:hover {
  background-color: #b91c1c;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
}

.fabrication-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-section {
  background-color: white;
  padding: 1.5rem;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
  margin-bottom: 1rem;
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

.form-input[readonly] {
  background-color: #f9fafb !important;
  cursor: not-allowed;
}

textarea.form-input {
  resize: vertical;
}

.table-container {
  overflow-x: auto;
}

.details-table {
  width: 100%;
  min-width: 800px;
}

.details-table th {
  padding: 0.5rem 1rem;
  background-color: #f9fafb;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.details-table td {
  padding: 0.5rem 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.table-input {
  width: 100%;
  padding: 0.25rem 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 0.25rem;
  font-size: 13px;
  transition: border-color 0.2s, box-shadow 0.2s;
  background-color: white !important;
}

.table-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
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
  appearance: textfield;
  -webkit-appearance: none;
  -moz-appearance: textfield;
}

.table-input[type="number"]::-webkit-outer-spin-button,
.table-input[type="number"]::-webkit-inner-spin-button {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  margin: 0;
}

.table-input[type="number"]:hover::-webkit-outer-spin-button,
.table-input[type="number"]:focus::-webkit-inner-spin-button {
  opacity: 1;
}
</style>