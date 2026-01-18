package itu.socobis.back.service;

import itu.socobis.back.dto.ProduitDTO;
import itu.socobis.back.entity.Ingredient;
import itu.socobis.back.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IngredientService {

    @Autowired
    private IngredientRepository ingredientRepository;

    /**
     * Récupère tous les produits finis.
     */
    public List<ProduitDTO> getProduitsFinis() {
        return ingredientRepository.findProduitsFinis()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les produits intermédiaires.
     */
    public List<ProduitDTO> getProduitsIntermediaires() {
        return ingredientRepository.findProduitsIntermediaires()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les matières premières.
     */
    public List<ProduitDTO> getMatierePremieres() {
        return ingredientRepository.findMatierePremieres()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les produits (produits finis, intermédiaires, matières premières).
     */
    public List<ProduitDTO> getAllProduits() {
        return ingredientRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un produit par ID.
     */
    public Optional<ProduitDTO> getProduitById(String id) {
        return ingredientRepository.findById(id)
                .map(this::toDTO);
    }

    /**
     * Récupère un ingrédient par ID (entité).
     */
    public Optional<Ingredient> getIngredientById(String id) {
        return ingredientRepository.findById(id);
    }

    /**
     * Met à jour le stock d'un ingrédient.
     */
    public void updateStock(String id, BigDecimal nouveauStock) {
        ingredientRepository.findById(id).ifPresent(ing -> {
            ing.setStock(nouveauStock);
            ingredientRepository.save(ing);
        });
    }

    /**
     * Décrémente le stock d'un ingrédient.
     */
    public void decrementerStock(String id, BigDecimal quantite) {
        ingredientRepository.findById(id).ifPresent(ing -> {
            BigDecimal currentStock = ing.getStock() != null ? ing.getStock() : BigDecimal.ZERO;
            ing.setStock(currentStock.subtract(quantite));
            ingredientRepository.save(ing);
        });
    }

    /**
     * Incrémente le stock d'un ingrédient.
     */
    public void incrementerStock(String id, BigDecimal quantite) {
        ingredientRepository.findById(id).ifPresent(ing -> {
            BigDecimal currentStock = ing.getStock() != null ? ing.getStock() : BigDecimal.ZERO;
            ing.setStock(currentStock.add(quantite));
            ingredientRepository.save(ing);
        });
    }

    /**
     * Convertit une entité Ingredient en DTO.
     */
    private ProduitDTO toDTO(Ingredient ing) {
        ProduitDTO dto = new ProduitDTO();
        dto.setId(ing.getId());
        dto.setLibelle(ing.getLibelle());
        dto.setUnite(ing.getUnite());
        dto.setStock(ing.getStock() != null ? ing.getStock() : BigDecimal.ZERO);
        
        // Déterminer le type selon le typeIngredient
        String type = determineType(ing.getTypeIngredient());
        dto.setType(type);
        
        // Déterminer le statut du stock
        String stockStatus = determineStockStatus(ing);
        dto.setStockStatus(stockStatus);
        
        dto.setSeuilMin(ing.getSeuilMin());
        dto.setSeuilMax(ing.getSeuilMax());
        dto.setPu(ing.getPuAchat());
        dto.setPv(ing.getPuVente());
        dto.setCategorie(ing.getCategorie());
        
        return dto;
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

    private String determineStockStatus(Ingredient ing) {
        BigDecimal stock = ing.getStock() != null ? ing.getStock() : BigDecimal.ZERO;
        BigDecimal seuilMin = ing.getSeuilMin();
        BigDecimal seuilMax = ing.getSeuilMax();
        
        if (stock.compareTo(BigDecimal.ZERO) <= 0) {
            return "RUPTURE";
        }
        
        if (seuilMin != null && stock.compareTo(seuilMin) < 0) {
            return "CRITIQUE";
        }
        
        if (seuilMin != null && seuilMax != null) {
            BigDecimal difference = seuilMax.subtract(seuilMin);
            BigDecimal threshold = seuilMin.add(difference.multiply(new BigDecimal("0.3")));
            if (stock.compareTo(threshold) < 0) {
                return "BAS";
            }
        }
        
        return "OK";
    }
}
