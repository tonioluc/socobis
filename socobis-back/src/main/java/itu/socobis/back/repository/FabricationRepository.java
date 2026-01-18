package itu.socobis.back.repository;

import itu.socobis.back.entity.Fabrication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FabricationRepository extends JpaRepository<Fabrication, String> {

    /**
     * Historique des fabrications par ordre décroissant de date.
     */
    @Query("SELECT f FROM Fabrication f ORDER BY f.daty DESC")
    List<Fabrication> findAllOrderByDateDesc();

    /**
     * Fabrications par état.
     */
    List<Fabrication> findByEtatOrderByDatyDesc(Integer etat);

    /**
     * Fabrications entre deux dates.
     */
    @Query("SELECT f FROM Fabrication f WHERE f.daty BETWEEN :debut AND :fin ORDER BY f.daty DESC")
    List<Fabrication> findByDateRange(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /**
     * Génère le prochain ID de fabrication (séquence).
     */
    @Query(value = "SELECT 'FAB-' || TO_CHAR(SYSDATE, 'YYYYMMDD') || '-' || LPAD(NVL(MAX(SUBSTR(ID, -3)), 0) + 1, 3, '0') FROM FABRICATION WHERE ID LIKE 'FAB-' || TO_CHAR(SYSDATE, 'YYYYMMDD') || '%'", nativeQuery = true)
    String generateNextId();

    /**
     * Recherche par libellé.
     */
    @Query("SELECT f FROM Fabrication f WHERE LOWER(f.libelle) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY f.daty DESC")
    List<Fabrication> searchByLibelle(@Param("search") String search);
}
