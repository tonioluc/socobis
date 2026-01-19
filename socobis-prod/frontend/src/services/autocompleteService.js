import apiClient from './api/api.js'

export const rechercherClients = async (query) => {
  try {
    const response = await apiClient.get('/autocomplete/client', {
      params: { q: query }
    })
    return response.data.data || []
  } catch (error) {
    console.error('Erreur lors de la recherche de clients:', error)
    return []
  }
}

export const rechercherProduits = async (query) => {
  try {
    const response = await apiClient.get('/autocomplete/produit', {
      params: { q: query }
    })
    return response.data.data || []
  } catch (error) {
    console.error('Erreur lors de la recherche de produits:', error)
    return []
  }
}

export const rechercherIngredients = async (query) => {
  try {
    const response = await apiClient.get('/autocomplete/produit', {
      params: { q: query }
    })
    return response.data.data || []
  } catch (error) {
    console.error('Erreur lors de la recherche d\'ingrédients:', error)
    return []
  }
}

export const rechercherMagasins = async (query) => {
  try {
    const response = await apiClient.get('/autocomplete', {
      params: {
        classe: 'magasin.Magasin',
        table: 'MAGASINPOINT',
        valeur: 'id',
        affiche: 'val',
        q: query,
        useMotCle: 'true'
      }
    })
    return response.data.data || []
  } catch (error) {
    console.error('Erreur lors de la recherche de magasins:', error)
    return []
  }
}

export const rechercherBonDeCommande = async (query) => {
  try {
    const response = await apiClient.get('/autocomplete', {
      params: {
        classe: 'vente.BonDeCommande',
        table: 'BONDECOMMANDE_CLIENT',
        valeur: 'id',
        affiche: 'designation',
        q: query,
        useMotCle: 'true'
      }
    })
    return response.data.data || []
  } catch (error) {
    console.error('Erreur lors de la recherche de bon de commande:', error)
    return []
  }
}

export const getListeMagasins = async () => {
  try {
    const response = await apiClient.get('/autocomplete', {
      params: {
        classe: 'bean.TypeObjet',
        table: 'MAGASIN2',
        valeur: 'id',
        affiche: 'val',
        q: '',
        useMotCle: 'false'
      }
    })
    console.log(response.data.data)
    return response.data.data || []
  } catch (error) {
    console.error('Erreur lors du chargement des magasins:', error)
    return []
  }
}