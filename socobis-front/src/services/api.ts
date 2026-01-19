// Configuration de l'API - Pointe vers les servlets SOCOBIS
// Le serveur GlassFish tourne généralement sur le port 8080
// Le contexte de l'application est 'socobis-war'
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/socobis/api';

// Types basés sur la structure réelle SOCOBIS

// Ingrédients/Produits (table AS_INGREDIENTS)
export interface Produit {
  id: string;
  libelle: string;
  unite: string;
  stock: number;
  type: 'PRODUIT_FINI' | 'PRODUIT_INTERMEDIAIRE' | 'MATIERE_PREMIERE';
  stockStatus: 'OK' | 'BAS' | 'CRITIQUE' | 'RUPTURE';
  seuilMin?: number;
  seuilMax?: number;
  pu?: number;
  pv?: number;
  categorie?: string;
  quantiteParPack?: number;
  actif?: number;
  compose?: number;
  parfums?: string;
}

// Item de formule/recette (table AS_RECETTE)
export interface FormuleItem {
  itemId: string;
  libelle: string;
  type: 'PRODUIT_FINI' | 'PRODUIT_INTERMEDIAIRE' | 'MATIERE_PREMIERE';
  unite: string;
  qteParUnite: number;
  besoinTotal: number;
  stockDisponible: number;
  manquant: number;
  suffisant: boolean;
}

export interface SimulationFabrication {
  produitId: string;
  produitLibelle: string;
  produitUnite: string;
  quantiteAFabriquer: number;
  besoins: FormuleItem[];
  intermediairesManquants: FormuleItem[];
  peutFabriquer: boolean;
  message: string;
}

// Ligne de fabrication (table FABRICATIONFILLE)
export interface FabricationFille {
  id: string;
  idIngredients: string;      // IDINGREDIENTS - ID de l'ingrédient/composant
  libelle: string;            // LIBELLE - Désignation
  remarque?: string;          // REMARQUE
  idMere: string;             // IDMERE - ID de la fabrication parente
  dateBesoinn?: string;       // DATYBESOIN - Date de besoin
  unite?: string;             // IDUNITE
  idBcFille?: string;         // IDBCFILLE - Bon De Commande Fille
  machine?: string;           // IDMACHINE
  pu?: number;                // PU - Prix Unitaire
  qte?: number;               // QTE - Quantité
}

// États de fabrication (basé sur SOCOBIS)
export const FABRICATION_ETATS = {
  CREE: 1,
  VALIDE: 11,
  ENTAME: 21,
  BLOQUE: 31,
  TERMINE: 41
} as const;

// Mappage des codes d'état vers libellés
export const FABRICATION_ETATS_LABELS: Record<number, string> = {
  1: 'CRÉÉ',
  11: 'VALIDÉE',
  21: 'ENTAMÉE',
  31: 'BLOQUÉE',
  41: 'TERMINÉE'
};

// Fabrication principale (table FABRICATION)
export interface Fabrication {
  id: string;                 // ID - ex: FAB002943
  lancePar?: string;          // LANCEPAR - Lancée Par (ID magasin/entité)
  lanceParLibelle?: string;   // Libellé de l'entité qui lance
  cible?: string;             // CIBLE - ID du magasin cible
  cibleLibelle?: string;      // Libellé du magasin cible
  remarque?: string;          // REMARQUE
  libelle?: string;           // LIBELLE - Désignation
  dateBesoinn?: string;        // BESOIN - Date De Besoin
  daty?: string;              // DATY - Date de création
  etat: number;               // ETAT - Code numérique (1, 11, 21, 31, 41)
  idBc?: string;              // IDBC - Bon de Commande Associé
  idOfFille?: string;         // IDOFFILLE - Id Ordre De Fabrication Fille
  idOf?: string;              // IDOF - Id Ordre De Fabrication (Associé)
  fabricationPrec?: string;   // FABRICATIONPREC - Fabrication Précédente
  fabricationSuiv?: string;   // FABRICATIONSUIV - Fabrication Suivant
  equipe?: string;            // EQUIPE
  // Propriétés calculées
  statut?: string;            // Libellé de l'état (CRÉÉ, VALIDÉ, etc.)
  lignes?: FabricationFille[]; // Détails des composants
}

// Alias pour compatibilité avec le code existant
export type FabricationHistorique = Fabrication;

// Requête de création de fabrication
export interface FabricationRequest {
  cible?: string;             // Magasin cible
  lancePar?: string;          // Entité qui lance
  libelle?: string;           // Désignation
  remarque?: string;          // Remarque
  dateBesoinn?: string;        // Date de besoin
  idOf?: string;              // Ordre de fabrication associé
  idBc?: string;              // Bon de commande associé
  equipe?: string;            // Équipe
  composants: FabricationFilleRequest[]; // Liste des composants
}

// Requête pour une ligne de fabrication
export interface FabricationFilleRequest {
  idIngredients: string;      // ID du composant/ingrédient
  qte: number;                // Quantité
  unite?: string;             // Unité
  remarque?: string;          // Remarque
  idBcFille?: string;         // Bon de commande fille
  machine?: string;           // Machine
}

// Types pour la pagination
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // page actuelle (0-indexed)
  first: boolean;
  last: boolean;
  empty: boolean;
}

// API Client
class ApiService {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private async request<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
      ...options,
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Erreur inconnue' }));
      throw new Error(error.message || `HTTP ${response.status}`);
    }

    return response.json();
  }

  // ===== PRODUITS =====

  /**
   * Récupère tous les produits.
   */
  async getAllProduits(): Promise<Produit[]> {
    return this.request<Produit[]>('/produits/liste');
  }

  /**
   * Récupère un produit par ID.
   */
  async getProduitById(id: string): Promise<Produit> {
    return this.request<Produit>(`/produits/fiche/${id}`);
  }

  /**
   * Récupère les produits finis.
   */
  async getProduitsFinis(): Promise<Produit[]> {
    return this.request<Produit[]>('/produits/finis');
  }

  /**
   * Récupère les produits intermédiaires.
   */
  async getProduitsIntermediaires(): Promise<Produit[]> {
    return this.request<Produit[]>('/produits/intermediaires');
  }

  /**
   * Récupère les matières premières.
   */
  async getMatierePremieres(): Promise<Produit[]> {
    return this.request<Produit[]>('/produits/matieres-premieres');
  }

  /**
   * Récupère les produits fabricables (composés).
   */
  async getProduitsFabricables(): Promise<Produit[]> {
    return this.request<Produit[]>('/produits/fabricables');
  }

  /**
   * Récupère les produits avec pagination.
   */
  async getProduitsPage(params: { search?: string; type?: string; page?: number; size?: number }): Promise<Page<Produit>> {
    const queryParams = new URLSearchParams();
    if (params.search) queryParams.set('search', params.search);
    if (params.type) queryParams.set('type', params.type);
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    
    return this.request<Page<Produit>>(`/produits/page?${queryParams.toString()}`);
  }

  /**
   * Autocomplete pour recherche de produits.
   */
  async autocomplete(query: string, type?: string): Promise<Produit[]> {
    if (!query || query.trim().length === 0) return [];
    let url = `/produits/autocomplete?q=${encodeURIComponent(query)}`;
    if (type) url += `&type=${encodeURIComponent(type)}`;
    return this.request<Produit[]>(url);
  }

  // ===== FABRICATION =====

  /**
   * Récupère la formule d'un produit.
   */
  async getFormuleProduit(produitId: string): Promise<FormuleItem[]> {
    return this.request<FormuleItem[]>(`/fabrication/formule/${produitId}`);
  }

  /**
   * Simule une fabrication (calcule les besoins).
   */
  async simulerFabrication(produitId: string, quantite: number): Promise<SimulationFabrication> {
    return this.request<SimulationFabrication>(
      `/fabrication/simuler?produitId=${encodeURIComponent(produitId)}&quantite=${quantite}`
    );
  }

  /**
   * Exécute une fabrication (création + validation + entamé + terminé).
   */
  async executerFabrication(data: { produitId: string; quantite: number; user?: string }): Promise<Fabrication> {
    return this.request<Fabrication>('/fabrication/executer', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  /**
   * Crée une fabrication (état CRÉÉ).
   */
  async creerFabrication(data: { produitId: string; quantite: number; user?: string }): Promise<Fabrication> {
    return this.request<Fabrication>('/fabrication/creer', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  /**
   * Valide une fabrication (CRÉÉ -> VALIDÉ).
   */
  async validerFabrication(id: string, user?: string): Promise<Fabrication> {
    const params = user ? `?user=${encodeURIComponent(user)}` : '';
    return this.request<Fabrication>(`/fabrication/valider/${id}${params}`, {
      method: 'PUT',
    });
  }

  /**
   * Entame une fabrication (VALIDÉ -> ENTAMÉ).
   */
  async entamerFabrication(id: string, user?: string): Promise<Fabrication> {
    const params = user ? `?user=${encodeURIComponent(user)}` : '';
    return this.request<Fabrication>(`/fabrication/entamer/${id}${params}`, {
      method: 'PUT',
    });
  }

  /**
   * Bloque une fabrication (ENTAMÉ -> BLOQUÉ).
   */
  async bloquerFabrication(id: string, user?: string): Promise<Fabrication> {
    const params = user ? `?user=${encodeURIComponent(user)}` : '';
    return this.request<Fabrication>(`/fabrication/bloquer/${id}${params}`, {
      method: 'PUT',
    });
  }

  /**
   * Débloque une fabrication (BLOQUÉ -> ENTAMÉ).
   */
  async debloquerFabrication(id: string, user?: string): Promise<Fabrication> {
    const params = user ? `?user=${encodeURIComponent(user)}` : '';
    return this.request<Fabrication>(`/fabrication/debloquer/${id}${params}`, {
      method: 'PUT',
    });
  }

  /**
   * Termine une fabrication (ENTAMÉ -> TERMINÉ).
   */
  async terminerFabrication(id: string, user?: string): Promise<Fabrication> {
    const params = user ? `?user=${encodeURIComponent(user)}` : '';
    return this.request<Fabrication>(`/fabrication/terminer/${id}${params}`, {
      method: 'PUT',
    });
  }

  /**
   * Récupère la liste des fabrications avec filtres.
   */
  async getListeFabrications(params?: { 
    etat?: string; 
    dateMin?: string; 
    dateMax?: string 
  }): Promise<Fabrication[]> {
    const queryParams = new URLSearchParams();
    if (params?.etat) queryParams.set('etat', params.etat);
    if (params?.dateMin) queryParams.set('dateMin', params.dateMin);
    if (params?.dateMax) queryParams.set('dateMax', params.dateMax);
    const query = queryParams.toString();
    return this.request<Fabrication[]>(`/fabrication/liste${query ? '?' + query : ''}`);
  }

  /**
   * Récupère l'historique des fabrications (alias pour compatibilité).
   */
  async getHistoriqueFabrications(): Promise<Fabrication[]> {
    return this.getListeFabrications();
  }

  /**
   * Récupère l'historique des fabrications avec pagination et filtres de date.
   */
  async getHistoriqueFabricationsPage(params: {
    etat?: string;
    dateMin?: string;
    dateMax?: string;
    page?: number;
    size?: number;
  }): Promise<Page<Fabrication>> {
    const queryParams = new URLSearchParams();
    if (params.etat) queryParams.set('etat', params.etat);
    if (params.dateMin) queryParams.set('dateMin', params.dateMin);
    if (params.dateMax) queryParams.set('dateMax', params.dateMax);
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    
    return this.request<Page<Fabrication>>(`/fabrication/historique?${queryParams.toString()}`);
  }

  /**
   * Récupère le détail d'une fabrication.
   */
  async getFabricationDetail(id: string): Promise<Fabrication> {
    return this.request<Fabrication>(`/fabrication/fiche/${id}`);
  }
}

// Export une instance unique du service
export const api = new ApiService(API_BASE_URL);

export default api;