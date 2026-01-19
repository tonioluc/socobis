package itu.socobis.back.service;

import itu.socobis.back.dto.*;
import itu.socobis.back.dto.FabricationHistoriqueDTO.FabricationLigneDTO;
import itu.socobis.back.entity.*;
import itu.socobis.back.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * Basé sur le workflow SOCOBIS : CRÉÉ (1) -> VALIDÉ (11) -> ENTAMÉ (21) -> TERMINÉ (41)
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
        fabrication.setDaty(LocalDate.now());
        fabrication.setEtat(Fabrication.ETAT_TERMINE); // Exécution directe = TERMINÉ (41)
        
        fabrication = fabricationRepository.save(fabrication);
        
        // Créer les lignes de fabrication et mettre à jour les stocks
        List<FabricationLigneDTO> lignes = new ArrayList<>();
        int ligneCount = 0;
        
        for (FormuleItemDTO besoin : simulation.getBesoins()) {
            // Créer la ligne de fabrication
            FabricationFille ligne = new FabricationFille();
            ligne.setId(generateFabricationFilleId());
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
        historique.setStatut(fabrication.chaineEtat());
        historique.setLignes(lignes);
        
        return historique;
    }

    /**
     * Créer une fabrication (état CRÉÉ = 1).
     * Basé sur la logique de fabrication-saisie.jsp
     */
    @Transactional
    public FabricationHistoriqueDTO creerFabrication(String produitId, BigDecimal quantite) {
        Optional<Ingredient> produitOpt = ingredientRepository.findById(produitId);
        if (produitOpt.isEmpty()) {
            throw new RuntimeException("Produit non trouvé");
        }
        
        Ingredient produit = produitOpt.get();
        
        // Créer l'enregistrement de fabrication
        Fabrication fabrication = new Fabrication();
        String newId = generateFabricationId();
        fabrication.setId(newId);
        fabrication.setCible(produitId);
        fabrication.setLibelle("Fabrication de " + produit.getLibelle() + " x " + quantite);
        fabrication.setDaty(LocalDate.now());
        fabrication.setEtat(Fabrication.ETAT_CREE); // État CRÉÉ (1)
        
        fabrication = fabricationRepository.save(fabrication);
        
        // Récupérer la recette pour créer les lignes
        List<Recette> recettes = recetteRepository.findByIdProduitsWithIngredient(produitId);
        List<FabricationLigneDTO> lignes = new ArrayList<>();
        
        for (Recette recette : recettes) {
            Ingredient ingredient = recette.getIngredient();
            if (ingredient == null) continue;
            
            BigDecimal qteParUnite = recette.getQuantite() != null ? recette.getQuantite() : BigDecimal.ONE;
            BigDecimal besoinTotal = qteParUnite.multiply(quantite);
            
            FabricationFille ligne = new FabricationFille();
            ligne.setId(generateFabricationFilleId());
            ligne.setIdMere(fabrication.getId());
            ligne.setIdIngredients(ingredient.getId());
            ligne.setLibelle(ingredient.getLibelle());
            ligne.setQte(besoinTotal);
            ligne.setIdUnite(ingredient.getUnite());
            
            fabricationFilleRepository.save(ligne);
            
            FabricationLigneDTO ligneDTO = new FabricationLigneDTO();
            ligneDTO.setIngredientId(ingredient.getId());
            ligneDTO.setIngredientLibelle(ingredient.getLibelle());
            ligneDTO.setType(determineType(ingredient.getTypeIngredient()));
            ligneDTO.setQuantiteUtilisee(besoinTotal);
            ligneDTO.setUnite(ingredient.getUnite());
            lignes.add(ligneDTO);
        }
        
        FabricationHistoriqueDTO dto = new FabricationHistoriqueDTO();
        dto.setId(fabrication.getId());
        dto.setProduitId(produitId);
        dto.setProduitLibelle(produit.getLibelle());
        dto.setQuantite(quantite);
        dto.setUnite(produit.getUnite());
        dto.setDateFabrication(fabrication.getDaty().atStartOfDay());
        dto.setStatut(fabrication.chaineEtat());
        dto.setLignes(lignes);
        
        return dto;
    }

    /**
     * Valider une fabrication (passer de CRÉÉ à VALIDÉ).
     * Basé sur valider() de fabrication-saisie.jsp
     */
    @Transactional
    public FabricationHistoriqueDTO validerFabrication(String fabricationId) {
        Fabrication fabrication = fabricationRepository.findById(fabricationId)
            .orElseThrow(() -> new RuntimeException("Fabrication non trouvée: " + fabricationId));
        
        if (fabrication.getEtat() != Fabrication.ETAT_CREE) {
            throw new RuntimeException("Seule une fabrication CRÉÉE peut être validée. État actuel: " + fabrication.chaineEtat());
        }
        
        fabrication.setEtat(Fabrication.ETAT_VALIDE);
        fabricationRepository.save(fabrication);
        
        return toHistoriqueDTO(fabrication);
    }

    /**
     * Entamer une fabrication (passer de VALIDÉ à ENTAMÉ).
     * Basé sur entamer() de fabrication-saisie.jsp
     */
    @Transactional
    public FabricationHistoriqueDTO entamerFabrication(String fabricationId) {
        Fabrication fabrication = fabricationRepository.findById(fabricationId)
            .orElseThrow(() -> new RuntimeException("Fabrication non trouvée: " + fabricationId));
        
        if (!fabrication.canEntamer()) {
            throw new RuntimeException("Seule une fabrication VALIDÉE peut être entamée. État actuel: " + fabrication.chaineEtat());
        }
        
        fabrication.setEtat(Fabrication.ETAT_ENTAME);
        fabricationRepository.save(fabrication);
        
        // Décrémenter les stocks des ingrédients
        List<FabricationFille> lignes = fabricationFilleRepository.findByIdMereWithIngredient(fabricationId);
        for (FabricationFille ligne : lignes) {
            if (ligne.getQte() != null && ligne.getIdIngredients() != null) {
                ingredientService.decrementerStock(ligne.getIdIngredients(), ligne.getQte());
            }
        }
        
        return toHistoriqueDTO(fabrication);
    }

    /**
     * Bloquer une fabrication (passer à état BLOQUÉ).
     */
    @Transactional
    public FabricationHistoriqueDTO bloquerFabrication(String fabricationId) {
        Fabrication fabrication = fabricationRepository.findById(fabricationId)
            .orElseThrow(() -> new RuntimeException("Fabrication non trouvée: " + fabricationId));
        
        if (fabrication.getEtat() != Fabrication.ETAT_ENTAME) {
            throw new RuntimeException("Seule une fabrication ENTAMÉE peut être bloquée. État actuel: " + fabrication.chaineEtat());
        }
        
        fabrication.setEtat(Fabrication.ETAT_BLOQUE);
        fabricationRepository.save(fabrication);
        
        return toHistoriqueDTO(fabrication);
    }

    /**
     * Débloquer une fabrication (retourner à état ENTAMÉ).
     */
    @Transactional
    public FabricationHistoriqueDTO debloquerFabrication(String fabricationId) {
        Fabrication fabrication = fabricationRepository.findById(fabricationId)
            .orElseThrow(() -> new RuntimeException("Fabrication non trouvée: " + fabricationId));
        
        if (fabrication.getEtat() != Fabrication.ETAT_BLOQUE) {
            throw new RuntimeException("Seule une fabrication BLOQUÉE peut être débloquée. État actuel: " + fabrication.chaineEtat());
        }
        
        fabrication.setEtat(Fabrication.ETAT_ENTAME);
        fabricationRepository.save(fabrication);
        
        return toHistoriqueDTO(fabrication);
    }

    /**
     * Terminer une fabrication (passer de ENTAMÉ à TERMINÉ).
     * Basé sur terminer() de fabrication-saisie.jsp
     */
    @Transactional
    public FabricationHistoriqueDTO terminerFabrication(String fabricationId) {
        Fabrication fabrication = fabricationRepository.findById(fabricationId)
            .orElseThrow(() -> new RuntimeException("Fabrication non trouvée: " + fabricationId));
        
        if (!fabrication.canTerminer()) {
            throw new RuntimeException("Seule une fabrication ENTAMÉE peut être terminée. État actuel: " + fabrication.chaineEtat());
        }
        
        fabrication.setEtat(Fabrication.ETAT_TERMINE);
        fabricationRepository.save(fabrication);
        
        // Incrémenter le stock du produit fini
        if (fabrication.getCible() != null) {
            // Calculer la quantité fabriquée
            List<FabricationFille> lignes = fabricationFilleRepository.findByIdMereWithIngredient(fabricationId);
            BigDecimal qteFabriquee = lignes.stream()
                .map(FabricationFille::getQte)
                .filter(q -> q != null)
                .findFirst()
                .orElse(BigDecimal.ONE);
            
            ingredientService.incrementerStock(fabrication.getCible(), qteFabriquee);
        }
        
        return toHistoriqueDTO(fabrication);
    }

    /**
     * Génère un nouvel ID pour une fabrication.
     * Format: FAB + 6 chiffres (ex: FAB002910)
     * Basé sur preparePk("FAB", "getSeqFab") de l'EJB SOCOBIS
     */
    private String generateFabricationId() {
        try {
            return fabricationRepository.generateNextFabId();
        } catch (Exception e) {
            // Fallback si la séquence n'existe pas
            long count = fabricationRepository.count();
            return String.format("FAB%06d", count + 1);
        }
    }

    /**
     * Génère un nouvel ID pour une ligne de fabrication.
     * Format: FABF + 6 chiffres (ex: FABF000123)
     */
    private String generateFabricationFilleId() {
        try {
            return fabricationRepository.generateNextFabFilleId();
        } catch (Exception e) {
            // Fallback si la séquence n'existe pas
            long count = fabricationFilleRepository.count();
            return String.format("FABF%06d", count + 1);
        }
    }

    /**
     * Récupère l'historique de toutes les fabrications.
     */
    public List<FabricationHistoriqueDTO> getHistoriqueFabrications() {
        List<Fabrication> fabrications = fabricationRepository.findAllOrderByDateDesc();
        return fabrications.stream().map(this::toHistoriqueDTO).collect(Collectors.toList());
    }

    /**
     * Récupère l'historique avec pagination (compatible Oracle 11g).
     * Utilise ROWNUM au lieu de FETCH FIRST.
     */
    public Page<FabricationHistoriqueDTO> getHistoriqueFabricationsPage(int page, int size) {
        int startRow = page * size;
        int endRow = startRow + size;
        
        List<Fabrication> fabrications = fabricationRepository.findAllOrderByDateDescPaginated(startRow, endRow);
        long total = fabricationRepository.countAll();
        
        List<FabricationHistoriqueDTO> dtos = fabrications.stream()
            .map(this::toHistoriqueDTO)
            .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(dtos, PageRequest.of(page, size), total);
    }

    /**
     * Récupère l'historique avec pagination et filtres de date (compatible Oracle 11g).
     */
    public Page<FabricationHistoriqueDTO> getHistoriqueFabricationsPage(LocalDate dateMin, LocalDate dateMax, int page, int size) {
        int startRow = page * size;
        int endRow = startRow + size;
        
        List<Fabrication> fabrications;
        long total;
        
        if (dateMin != null && dateMax != null) {
            fabrications = fabricationRepository.findByDateRangePaginated(dateMin, dateMax, startRow, endRow);
            total = fabricationRepository.countByDateRange(dateMin, dateMax);
        } else if (dateMin != null) {
            fabrications = fabricationRepository.findByDateAfterPaginated(dateMin, startRow, endRow);
            total = fabricationRepository.countByDateAfter(dateMin);
        } else if (dateMax != null) {
            fabrications = fabricationRepository.findByDateBeforePaginated(dateMax, startRow, endRow);
            total = fabricationRepository.countByDateBefore(dateMax);
        } else {
            fabrications = fabricationRepository.findAllOrderByDateDescPaginated(startRow, endRow);
            total = fabricationRepository.countAll();
        }
        
        List<FabricationHistoriqueDTO> dtos = fabrications.stream()
            .map(this::toHistoriqueDTO)
            .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(dtos, PageRequest.of(page, size), total);
    }

    /**
     * Convertit une entité Fabrication en DTO historique.
     */
    private FabricationHistoriqueDTO toHistoriqueDTO(Fabrication fab) {
        FabricationHistoriqueDTO dto = new FabricationHistoriqueDTO();
        dto.setId(fab.getId());
        dto.setProduitId(fab.getCible());
        dto.setStatut(fab.chaineEtat());
        dto.setEtat(fab.getEtat());
        
        if (fab.getDaty() != null) {
            dto.setDateFabrication(fab.getDaty().atStartOfDay());
        }
        
        if (fab.getCible() != null) {
            ingredientRepository.findById(fab.getCible()).ifPresent(ing -> {
                dto.setProduitLibelle(ing.getLibelle());
                dto.setUnite(ing.getUnite());
            });
        } else {
            dto.setProduitLibelle(fab.getLibelle());
        }
        
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
        
        BigDecimal qteTotale = filles.stream()
            .map(FabricationFille::getQte)
            .filter(q -> q != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setQuantite(qteTotale);
        
        return dto;
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
