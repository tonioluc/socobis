package itu.socobis.back.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité mappant la table FABRICATION.
 * Représente un ordre de fabrication (header).
 */
@Entity
@Table(name = "FABRICATION")
public class Fabrication {

    @Id
    @Column(name = "ID", length = 255)
    private String id;

    @Column(name = "LIBELLE", length = 255)
    private String libelle;

    @Column(name = "REMARQUE", length = 255)
    private String remarque;

    @Column(name = "DATY")
    private LocalDate daty;

    @Column(name = "BESOIN")
    private LocalDate besoin;

    /**
     * État de la fabrication:
     * 1 = créée (brouillon)
     * 5 = validée
     * 10 = en cours
     * 15 = terminée
     * -5 = annulée
     */
    @Column(name = "ETAT", nullable = false)
    private Integer etat = 1;

    @Column(name = "LANCEPAR", length = 255)
    private String lancePar;

    @Column(name = "CIBLE", length = 255)
    private String cible;

    @Column(name = "IDBC", length = 500)
    private String idBc;

    @Column(name = "IDOFFILLE", length = 255)
    private String idOfFille;

    @Column(name = "IDOF", length = 255)
    private String idOf;

    @Column(name = "FABRICATIONPREC", length = 100)
    private String fabricationPrec;

    @Column(name = "FABRICATIONSUIV", length = 50)
    private String fabricationSuiv;

    @Column(name = "EQUIPE", length = 255)
    private String equipe;

    @OneToMany(mappedBy = "fabrication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FabricationFille> lignes = new ArrayList<>();

    // Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }

    public LocalDate getDaty() {
        return daty;
    }

    public void setDaty(LocalDate daty) {
        this.daty = daty;
    }

    public LocalDate getBesoin() {
        return besoin;
    }

    public void setBesoin(LocalDate besoin) {
        this.besoin = besoin;
    }

    public Integer getEtat() {
        return etat;
    }

    public void setEtat(Integer etat) {
        this.etat = etat;
    }

    public String getLancePar() {
        return lancePar;
    }

    public void setLancePar(String lancePar) {
        this.lancePar = lancePar;
    }

    public String getCible() {
        return cible;
    }

    public void setCible(String cible) {
        this.cible = cible;
    }

    public String getIdBc() {
        return idBc;
    }

    public void setIdBc(String idBc) {
        this.idBc = idBc;
    }

    public String getIdOfFille() {
        return idOfFille;
    }

    public void setIdOfFille(String idOfFille) {
        this.idOfFille = idOfFille;
    }

    public String getIdOf() {
        return idOf;
    }

    public void setIdOf(String idOf) {
        this.idOf = idOf;
    }

    public String getFabricationPrec() {
        return fabricationPrec;
    }

    public void setFabricationPrec(String fabricationPrec) {
        this.fabricationPrec = fabricationPrec;
    }

    public String getFabricationSuiv() {
        return fabricationSuiv;
    }

    public void setFabricationSuiv(String fabricationSuiv) {
        this.fabricationSuiv = fabricationSuiv;
    }

    public String getEquipe() {
        return equipe;
    }

    public void setEquipe(String equipe) {
        this.equipe = equipe;
    }

    public List<FabricationFille> getLignes() {
        return lignes;
    }

    public void setLignes(List<FabricationFille> lignes) {
        this.lignes = lignes;
    }

    public void addLigne(FabricationFille ligne) {
        lignes.add(ligne);
        ligne.setFabrication(this);
    }

    public void removeLigne(FabricationFille ligne) {
        lignes.remove(ligne);
        ligne.setFabrication(null);
    }
}
