package itu.socobis.back.repository;

import itu.socobis.back.entity.FabricationFille;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FabricationFilleRepository extends JpaRepository<FabricationFille, String> {

    /**
     * Lignes d'une fabrication.
     */
    List<FabricationFille> findByIdMere(String idMere);

    /**
     * Lignes avec détails ingrédient.
     */
    @Query("SELECT ff FROM FabricationFille ff LEFT JOIN FETCH ff.ingredient WHERE ff.idMere = :idMere")
    List<FabricationFille> findByIdMereWithIngredient(@Param("idMere") String idMere);

    /**
     * Supprime toutes les lignes d'une fabrication.
     */
    void deleteByIdMere(String idMere);
}
