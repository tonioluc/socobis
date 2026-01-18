package itu.socobis.back.repository;

import itu.socobis.back.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    /**
     * Produits finis (composés et vendables).
     */
    @Query("SELECT i FROM Ingredient i WHERE i.compose = 1 AND i.isVente = 1 AND (i.actif = 1 OR i.actif IS NULL)")
    List<Ingredient> findProduitsFinis();

    /**
     * Produits intermédiaires (composés mais pas vendables).
     */
    @Query("SELECT i FROM Ingredient i WHERE i.compose = 1 AND (i.isVente = 0 OR i.isVente IS NULL) AND (i.actif = 1 OR i.actif IS NULL)")
    List<Ingredient> findProduitsIntermediaires();

    /**
     * Matières premières (non composées).
     */
    @Query("SELECT i FROM Ingredient i WHERE (i.compose = 0 OR i.compose IS NULL) AND (i.actif = 1 OR i.actif IS NULL)")
    List<Ingredient> findMatierePremieres();

    /**
     * Tous les produits fabriquables (composés).
     */
    @Query("SELECT i FROM Ingredient i WHERE i.compose = 1 AND (i.actif = 1 OR i.actif IS NULL)")
    List<Ingredient> findProduitsFabricables();

    /**
     * Recherche par libellé (LIKE).
     */
    @Query("SELECT i FROM Ingredient i WHERE LOWER(i.libelle) LIKE LOWER(CONCAT('%', :search, '%')) AND (i.actif = 1 OR i.actif IS NULL)")
    List<Ingredient> searchByLibelle(@Param("search") String search);

    /**
     * Produits par catégorie.
     */
    List<Ingredient> findByCategorieIngredientAndActif(String categorieIngredient, Integer actif);
}
