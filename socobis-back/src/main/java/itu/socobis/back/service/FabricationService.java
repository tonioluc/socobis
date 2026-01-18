package itu.socobis.back.service;

import itu.socobis.back.dto.*;
import itu.socobis.back.dto.FabricationHistoriqueDTO.FabricationLigneDTO;
import itu.socobis.back.entity.*;
import itu.socobis.back.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FabricationService {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private RecetteRepository recetteRepository;

    @Autowired
    private FabricationRepository fabricationRepository;

    @Autowired
    private FabricationFilleRepository fabricationFilleRepository;

    @Autowired
    private IngredientService ingredientService;

    /**
     * Simule une fabrication : calcule les besoins et vérifie le stock.
     */
    public SimulationFabricationDTO simulerFabrication(String produitId, BigDecimal quantite) {
        SimulationFabricationDTO simulation = new SimulationFabricationDTO();
        
        // Récupérer le produit à fabriquer
        Optional<Ingredient> produitOpt = ingredientRepository.findById(produitId);
        if (produitOpt.isEmpty()) {
            simulation.setPeutFabriquer(false);
            simulation.setMessage("Produit non trouvé");
            return simulation;
        }
        
        Ingredient produit = produitOpt.get();
        simulation.setProduitId(produitId);
        simulation.setProduitLibelle(produit.getLibelle());
        simulation.setProduitUnite(produit.getUnite());
        simulation.setQuantiteAFabriquer(quantite);
        
        // Récupérer la recette/formule du produit
        List<Recette> recettes = recetteRepository.findByIdProduitsWithIngredient(produitId);
        
        if (recettes.isEmpty()) {
            simulation.setPeutFabriquer(false);
            simulation.setMessage("Aucune formule/recette définie pour ce produit");
            return simulation;
        }
        
        // Calculer les besoins
        List<FormuleItemDTO> besoins = new ArrayList<>();
        List<FormuleItemDTO> intermediairesManquants = new ArrayList<>();
        boolean toutSuffisant = true;
        
        for (Recette recette : recettes) {
            Ingredient ingredient = recette.getIngredient();
            if (ingredient == null) continue;
            
            FormuleItemDTO item = new FormuleItemDTO();
            item.setItemId(ingredient.getId());
            item.setLibelle(ingredient.getLibelle());
            item.setUnite(ingredient.getUnite());
            
            // Quantité par unité de produit fini
            BigDecimal qteParUnite = recette.getQuantite() != null ? recette.getQuantite() : BigDecimal.ONE;
            item.setQteParUnite(qteParUnite);
            
            // Besoin total = quantité par unité * quantité à fabriquer
            BigDecimal besoinTotal = qteParUnite.multiply(quantite);
            item.setBesoinTotal(besoinTotal);
            
            // Stock disponible
            BigDecimal stockDispo = ingredient.getStock() != null ? ingredient.getStock() : BigDecimal.ZERO;
            item.setStockDisponible(stockDispo);
            
            // Manquant = max(0, besoin - stock)
            BigDecimal manquant = besoinTotal.subtract(stockDispo);
            if (manquant.compareTo(BigDecimal.ZERO) < 0) {
                manquant = BigDecimal.ZERO;
            }
            item.setManquant(manquant);
            
            // Suffisant si stock >= besoin
            boolean suffisant = stockDispo.compareTo(besoinTotal) >= 0;
            item.setSuffisant(suffisant);
            
            // Déterminer le type
            String type = determineType(ingredient.getTypeIngredient());
            item.setType(type);
            
            besoins.add(item);
            
            if (!suffisant) {
                toutSuffisant = false;
                // Si c'est un produit intermédiaire, il peut être fabriqué
                if ("PRODUIT_INTERMEDIAIRE".equals(type)) {
                    intermediairesManquants.add(item);
                }
            }
        }
        
        simulation.setBesoins(besoins);
        simulation.setIntermediairesManquants(intermediairesManquants);
        simulation.setPeutFabriquer(toutSuffisant);
        
        if (toutSuffisant) {
            simulation.setMessage("Tous les ingrédients sont disponibles en quantité suffisante");
        } else if (!intermediairesManquants.isEmpty()) {
            simulation.setMessage("Des produits intermédiaires doivent être fabriqués en premier");
        } else {
            simulation.setMessage("Stock insuffisant pour certains ingrédients");
        }
        
        return simulation;
    }

    /**
     * Exécute une fabrication : décrémente les stocks des ingrédients et incrémente le stock du produit fini.
     */
    @Transactional
    public FabricationHistoriqueDTO executerFabrication(String produitId, BigDecimal quantite) {
        // Vérifier d'abord que la fabrication est possible
        SimulationFabricationDTO simulation = simulerFabrication(produitId, quantite);
        
        if (!simulation.isPeutFabriquer()) {
            throw new RuntimeException("Fabrication impossible : " + simulation.getMessage());
        }
        
        // Créer l'enregistrement de fabrication
        Fabrication fabrication = new Fabrication();
        String newId = generateFabricationId();
        fabrication.setId(newId);
        fabrication.setCible(produitId); // CIBLE contient l'ID du produit à fabriquer
        fabrication.setLibelle("Fabrication de " + simulation.getProduitLibelle() + " x " + quantite);
        fabrication.setDaty(java.time.LocalDate.now());
        fabrication.setEtat(15); // 15 = terminée
        
        fabrication = fabricationRepository.save(fabrication);
        
        // Créer les lignes de fabrication et mettre à jour les stocks
        List<FabricationLigneDTO> lignes = new ArrayList<>();
        int ligneCount = 0;
        
        for (FormuleItemDTO besoin : simulation.getBesoins()) {
            // Créer la ligne de fabrication
            FabricationFille ligne = new FabricationFille();
            ligne.setId(newId + "_" + (++ligneCount));
            ligne.setIdMere(fabrication.getId());
            ligne.setIdIngredients(besoin.getItemId());
            ligne.setLibelle(besoin.getLibelle());
            ligne.setQte(besoin.getBesoinTotal());
            ligne.setIdUnite(besoin.getUnite());
            
            fabricationFilleRepository.save(ligne);
            
            // Décrémenter le stock de l'ingrédient
            ingredientService.decrementerStock(besoin.getItemId(), besoin.getBesoinTotal());
            
            // Ajouter à la liste des lignes pour le retour
            FabricationLigneDTO ligneDTO = new FabricationLigneDTO();
            ligneDTO.setIngredientId(besoin.getItemId());
            ligneDTO.setIngredientLibelle(besoin.getLibelle());
            ligneDTO.setType(besoin.getType());
            ligneDTO.setQuantiteUtilisee(besoin.getBesoinTotal());
            ligneDTO.setUnite(besoin.getUnite());
            lignes.add(ligneDTO);
        }
        
        // Incrémenter le stock du produit fini
        ingredientService.incrementerStock(produitId, quantite);
        
        // Préparer le retour
        FabricationHistoriqueDTO historique = new FabricationHistoriqueDTO();
        historique.setId(fabrication.getId());
        historique.setProduitId(produitId);
        historique.setProduitLibelle(simulation.getProduitLibelle());
        historique.setQuantite(quantite);
        historique.setUnite(simulation.getProduitUnite());
        historique.setDateFabrication(fabrication.getDaty().atStartOfDay());
        historique.setStatut(getStatutLabel(fabrication.getEtat()));
        historique.setLignes(lignes);
        
        return historique;
    }

    /**
     * Génère un nouvel ID pour une fabrication.
     */
    private String generateFabricationId() {
        return "FAB-" + System.currentTimeMillis();
    }

    /**
     * Convertit le code état en libellé.
     */
    private String getStatutLabel(Integer etat) {
        if (etat == null) return "INCONNU";
        switch (etat) {
            case 1: return "BROUILLON";
            case 5: return "VALIDEE";
            case 10: return "EN_COURS";
            case 15: return "TERMINE";
            case -5: return "ANNULEE";
            default: return "INCONNU";
        }
    }

    /**
     * Récupère l'historique de toutes les fabrications.
     */
    public List<FabricationHistoriqueDTO> getHistoriqueFabrications() {
        List<Fabrication> fabrications = fabricationRepository.findAllOrderByDateDesc();
        
        return fabrications.stream().map(fab -> {
            FabricationHistoriqueDTO dto = new FabricationHistoriqueDTO();
            dto.setId(fab.getId());
            dto.setProduitId(fab.getCible()); // CIBLE contient l'ID du produit
            dto.setStatut(getStatutLabel(fab.getEtat()));
            
            // Date de fabrication
            if (fab.getDaty() != null) {
                dto.setDateFabrication(fab.getDaty().atStartOfDay());
            }
            
            // Récupérer le libellé du produit et calculer la quantité à partir des lignes
            if (fab.getCible() != null) {
                ingredientRepository.findById(fab.getCible()).ifPresent(ing -> {
                    dto.setProduitLibelle(ing.getLibelle());
                    dto.setUnite(ing.getUnite());
                });
            } else {
                dto.setProduitLibelle(fab.getLibelle());
            }
            
            // Récupérer les lignes de fabrication
            List<FabricationFille> filles = fabricationFilleRepository.findByIdMereWithIngredient(fab.getId());
            List<FabricationLigneDTO> lignes = filles.stream().map(fille -> {
                FabricationLigneDTO ligne = new FabricationLigneDTO();
                ligne.setIngredientId(fille.getIdIngredients());
                ligne.setQuantiteUtilisee(fille.getQte());
                ligne.setUnite(fille.getIdUnite());
                ligne.setIngredientLibelle(fille.getLibelle());
                
                if (fille.getIngredient() != null) {
                    ligne.setIngredientLibelle(fille.getIngredient().getLibelle());
                    ligne.setUnite(fille.getIngredient().getUnite());
                    ligne.setType(determineType(fille.getIngredient().getTypeIngredient()));
                }
                
                return ligne;
            }).collect(Collectors.toList());
            
            dto.setLignes(lignes);
            
            // Calculer la quantité totale (somme des QTE des lignes ou première ligne)
            BigDecimal qteTotale = filles.stream()
                .map(FabricationFille::getQte)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setQuantite(qteTotale);
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Récupère la formule/recette d'un produit.
     */
    public List<FormuleItemDTO> getFormuleProduit(String produitId) {
        List<Recette> recettes = recetteRepository.findByIdProduitsWithIngredient(produitId);
        
        return recettes.stream().map(recette -> {
            Ingredient ingredient = recette.getIngredient();
            
            FormuleItemDTO item = new FormuleItemDTO();
            item.setItemId(recette.getIdIngredients());
            item.setQteParUnite(recette.getQuantite());
            
            if (ingredient != null) {
                item.setLibelle(ingredient.getLibelle());
                item.setUnite(ingredient.getUnite());
                item.setStockDisponible(ingredient.getStock() != null ? ingredient.getStock() : BigDecimal.ZERO);
                item.setType(determineType(ingredient.getTypeIngredient()));
            }
            
            return item;
        }).collect(Collectors.toList());
    }

    private String determineType(String typeIngredient) {
        if (typeIngredient == null) return "MATIERE_PREMIERE";
        
        switch (typeIngredient.toUpperCase()) {
            case "PF":
            case "PRODUIT_FINI":
                return "PRODUIT_FINI";
            case "PI":
            case "PRODUIT_INTERMEDIAIRE":
                return "PRODUIT_INTERMEDIAIRE";
            case "MP":
            case "MATIERE_PREMIERE":
            default:
                return "MATIERE_PREMIERE";
        }
    }
}
