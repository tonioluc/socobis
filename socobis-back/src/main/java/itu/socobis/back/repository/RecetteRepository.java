package itu.socobis.back.repository;

import itu.socobis.back.entity.Recette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecetteRepository extends JpaRepository<Recette, String> {

    /**
     * Récupère la formule (nomenclature) d'un produit.
     */
    List<Recette> findByIdProduits(String idProduits);

    /**
     * Récupère la formule avec les détails des ingrédients (fetch join).
     */
    @Query("SELECT r FROM Recette r LEFT JOIN FETCH r.ingredientComposant WHERE r.idProduits = :idProduits")
    List<Recette> findByIdProduitsWithIngredient(@Param("idProduits") String idProduits);

    /**
     * Vérifie si un produit a une formule définie.
     */
    boolean existsByIdProduits(String idProduits);

    /**
     * Compte le nombre de composants d'un produit.
     */
    long countByIdProduits(String idProduits);
}
