// Configuration de l'API
const API_BASE_URL = 'http://localhost:8090/api';

// Types
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
}

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

export interface FabricationLigne {
  ingredientId: string;
  ingredientLibelle: string;
  type: string;
  quantiteUtilisee: number;
  unite: string;
}

export interface FabricationHistorique {
  id: string;
  produitId: string;
  produitLibelle: string;
  quantite: number;
  unite: string;
  dateFabrication: string;
  statut: string;
  lignes: FabricationLigne[];
}

export interface FabricationRequest {
  produitId: string;
  quantite: number;
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
    return this.request<Produit[]>('/produits');
  }

  /**
   * Récupère un produit par ID.
   */
  async getProduitById(id: string): Promise<Produit> {
    return this.request<Produit>(`/produits/${id}`);
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
   * Exécute une fabrication.
   */
  async executerFabrication(request: FabricationRequest): Promise<FabricationHistorique> {
    return this.request<FabricationHistorique>('/fabrication/executer', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  /**
   * Récupère l'historique des fabrications.
   */
  async getHistoriqueFabrications(): Promise<FabricationHistorique[]> {
    return this.request<FabricationHistorique[]>('/fabrication/historique');
  }
}

// Export une instance unique du service
export const api = new ApiService(API_BASE_URL);

export default api;
