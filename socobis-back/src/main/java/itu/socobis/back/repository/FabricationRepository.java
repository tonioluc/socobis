package itu.socobis.back.repository;

import itu.socobis.back.entity.Fabrication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FabricationRepository extends JpaRepository<Fabrication, String> {

    /**
     * Historique des fabrications par ordre décroissant de date (sans pagination).
     */
    @Query("SELECT f FROM Fabrication f ORDER BY f.daty DESC")
    List<Fabrication> findAllOrderByDateDesc();

    /**
     * Fabrications par état.
     */
    List<Fabrication> findByEtatOrderByDatyDesc(Integer etat);

    /**
     * Fabrications entre deux dates (sans pagination).
     */
    @Query("SELECT f FROM Fabrication f WHERE f.daty BETWEEN :debut AND :fin ORDER BY f.daty DESC")
    List<Fabrication> findByDateRange(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /**
     * Génère le prochain ID de fabrication au format FAB + numéro séquentiel (6 chiffres).
     * Exemple: FAB002909, FAB002910, etc.
     * Basé sur preparePk("FAB", "getSeqFab") du code SOCOBIS existant.
     */
    @Query(value = "SELECT 'FAB' || LPAD(NVL(MAX(TO_NUMBER(SUBSTR(ID, 4))), 0) + 1, 6, '0') FROM FABRICATION WHERE REGEXP_LIKE(ID, '^FAB[0-9]{6}$')", nativeQuery = true)
    String generateNextFabId();

    /**
     * Génère le prochain ID pour une ligne de fabrication (FABF + 6 chiffres).
     * Exemple: FABF000123, FABF000124, etc.
     */
    @Query(value = "SELECT 'FABF' || LPAD(NVL(MAX(TO_NUMBER(SUBSTR(ID, 5))), 0) + 1, 6, '0') FROM FABRICATIONFILLE WHERE REGEXP_LIKE(ID, '^FABF[0-9]{6}$')", nativeQuery = true)
    String generateNextFabFilleId();

    /**
     * Recherche par libellé (liste).
     */
    @Query("SELECT f FROM Fabrication f WHERE LOWER(f.libelle) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY f.daty DESC")
    List<Fabrication> searchByLibelle(@Param("search") String search);

    /**
     * Compte le nombre de fabrications.
     */
    @Query("SELECT COUNT(f) FROM Fabrication f")
    long countAll();

    /**
     * Compte les fabrications entre deux dates.
     */
    @Query("SELECT COUNT(f) FROM Fabrication f WHERE f.daty BETWEEN :debut AND :fin")
    long countByDateRange(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /**
     * Compte les fabrications après une date.
     */
    @Query("SELECT COUNT(f) FROM Fabrication f WHERE f.daty >= :date")
    long countByDateAfter(@Param("date") LocalDate date);

    /**
     * Compte les fabrications avant une date.
     */
    @Query("SELECT COUNT(f) FROM Fabrication f WHERE f.daty <= :date")
    long countByDateBefore(@Param("date") LocalDate date);

    /**
     * Historique paginé avec ROWNUM (compatible Oracle 11g).
     * Oracle 11g ne supporte pas FETCH FIRST ... ROWS ONLY.
     */
    @Query(value = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM FABRICATION ORDER BY DATY DESC NULLS LAST) a WHERE ROWNUM <= :endRow) WHERE rnum > :startRow", nativeQuery = true)
    List<Fabrication> findAllOrderByDateDescPaginated(@Param("startRow") int startRow, @Param("endRow") int endRow);

    /**
     * Historique paginé entre deux dates avec ROWNUM.
     */
    @Query(value = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM FABRICATION WHERE DATY BETWEEN :debut AND :fin ORDER BY DATY DESC NULLS LAST) a WHERE ROWNUM <= :endRow) WHERE rnum > :startRow", nativeQuery = true)
    List<Fabrication> findByDateRangePaginated(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin, @Param("startRow") int startRow, @Param("endRow") int endRow);

    /**
     * Historique paginé après une date avec ROWNUM.
     */
    @Query(value = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM FABRICATION WHERE DATY >= :date ORDER BY DATY DESC NULLS LAST) a WHERE ROWNUM <= :endRow) WHERE rnum > :startRow", nativeQuery = true)
    List<Fabrication> findByDateAfterPaginated(@Param("date") LocalDate date, @Param("startRow") int startRow, @Param("endRow") int endRow);

    /**
     * Historique paginé avant une date avec ROWNUM.
     */
    @Query(value = "SELECT * FROM (SELECT a.*, ROWNUM rnum FROM (SELECT * FROM FABRICATION WHERE DATY <= :date ORDER BY DATY DESC NULLS LAST) a WHERE ROWNUM <= :endRow) WHERE rnum > :startRow", nativeQuery = true)
    List<Fabrication> findByDateBeforePaginated(@Param("date") LocalDate date, @Param("startRow") int startRow, @Param("endRow") int endRow);
}
