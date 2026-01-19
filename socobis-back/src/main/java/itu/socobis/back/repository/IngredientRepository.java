package itu.socobis.back.repository;

import itu.socobis.back.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Recherche par libellé (LIKE) - pour autocomplete.
     */
    @Query("SELECT i FROM Ingredient i WHERE LOWER(i.libelle) LIKE LOWER(CONCAT('%', :search, '%')) AND (i.actif = 1 OR i.actif IS NULL) ORDER BY i.libelle")
    List<Ingredient> searchByLibelle(@Param("search") String search);

    /**
     * Recherche par libellé avec limite - pour autocomplete rapide.
     */
    @Query(value = "SELECT * FROM AS_INGREDIENTS WHERE LOWER(LIBELLE) LIKE LOWER('%' || :search || '%') AND (ACTIF = 1 OR ACTIF IS NULL) AND ROWNUM <= :limit ORDER BY LIBELLE", nativeQuery = true)
    List<Ingredient> searchByLibelleLimit(@Param("search") String search, @Param("limit") int limit);

    /**
     * Produits par catégorie.
     */
    List<Ingredient> findByCategorieIngredientAndActif(String categorieIngredient, Integer actif);

    /**
     * Tous les produits (paginé).
     */
    @Query("SELECT i FROM Ingredient i WHERE (i.actif = 1 OR i.actif IS NULL) ORDER BY i.libelle")
    Page<Ingredient> findAllActive(Pageable pageable);

    /**
     * Produits finis (paginé).
     */
    @Query("SELECT i FROM Ingredient i WHERE i.compose = 1 AND i.isVente = 1 AND (i.actif = 1 OR i.actif IS NULL) ORDER BY i.libelle")
    Page<Ingredient> findProduitsFinis(Pageable pageable);

    /**
     * Produits intermédiaires (paginé).
     */
    @Query("SELECT i FROM Ingredient i WHERE i.compose = 1 AND (i.isVente = 0 OR i.isVente IS NULL) AND (i.actif = 1 OR i.actif IS NULL) ORDER BY i.libelle")
    Page<Ingredient> findProduitsIntermediaires(Pageable pageable);

    /**
     * Matières premières (paginé).
     */
    @Query("SELECT i FROM Ingredient i WHERE (i.compose = 0 OR i.compose IS NULL) AND (i.actif = 1 OR i.actif IS NULL) ORDER BY i.libelle")
    Page<Ingredient> findMatierePremieres(Pageable pageable);

    /**
     * Recherche avec pagination et filtre type.
     */
    @Query("SELECT i FROM Ingredient i WHERE " +
           "(i.actif = 1 OR i.actif IS NULL) " +
           "AND (:search IS NULL OR LOWER(i.libelle) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:type IS NULL OR " +
           "     (:type = 'PRODUIT_FINI' AND i.compose = 1 AND i.isVente = 1) OR " +
           "     (:type = 'PRODUIT_INTERMEDIAIRE' AND i.compose = 1 AND (i.isVente = 0 OR i.isVente IS NULL)) OR " +
           "     (:type = 'MATIERE_PREMIERE' AND (i.compose = 0 OR i.compose IS NULL))) " +
           "ORDER BY i.libelle")
    Page<Ingredient> findWithFilters(@Param("search") String search, @Param("type") String type, Pageable pageable);
}
