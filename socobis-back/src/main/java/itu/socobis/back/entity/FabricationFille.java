package itu.socobis.back.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entité mappant la table FABRICATIONFILLE.
 * Représente une ligne de fabrication (composant consommé ou produit fabriqué).
 */
@Entity
@Table(name = "FABRICATIONFILLE")
public class FabricationFille {

    @Id
    @Column(name = "ID", length = 255)
    private String id;

    @Column(name = "IDMERE", length = 255, insertable = false, updatable = false)
    private String idMere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDMERE")
    private Fabrication fabrication;

    @Column(name = "IDINGREDIENTS", length = 255)
    private String idIngredients;

    @Column(name = "LIBELLE", length = 255)
    private String libelle;

    @Column(name = "REMARQUE", length = 255)
    private String remarque;

    @Column(name = "QTE", precision = 30, scale = 5)
    private BigDecimal qte;

    @Column(name = "PU", precision = 30, scale = 5)
    private BigDecimal pu;

    @Column(name = "IDUNITE", length = 100)
    private String idUnite;

    @Column(name = "DATYBESOIN")
    private LocalDate datyBesoin;

    @Column(name = "NIVEAU")
    private Integer niveau;

    @Column(name = "IDMACHINE", length = 255)
    private String idMachine;

    @Column(name = "OPERATEUR", length = 255)
    private String operateur;

    @Column(name = "IDBCFILLE", length = 1000)
    private String idBcFille;

    // Relation vers l'ingrédient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDINGREDIENTS", referencedColumnName = "ID", insertable = false, updatable = false)
    private Ingredient ingredient;

    // Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdMere() {
        return idMere;
    }

    public void setIdMere(String idMere) {
        this.idMere = idMere;
    }

    public Fabrication getFabrication() {
        return fabrication;
    }

    public void setFabrication(Fabrication fabrication) {
        this.fabrication = fabrication;
    }

    public String getIdIngredients() {
        return idIngredients;
    }

    public void setIdIngredients(String idIngredients) {
        this.idIngredients = idIngredients;
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

    public BigDecimal getQte() {
        return qte;
    }

    public void setQte(BigDecimal qte) {
        this.qte = qte;
    }

    public BigDecimal getPu() {
        return pu;
    }

    public void setPu(BigDecimal pu) {
        this.pu = pu;
    }

    public String getIdUnite() {
        return idUnite;
    }

    public void setIdUnite(String idUnite) {
        this.idUnite = idUnite;
    }

    public LocalDate getDatyBesoin() {
        return datyBesoin;
    }

    public void setDatyBesoin(LocalDate datyBesoin) {
        this.datyBesoin = datyBesoin;
    }

    public Integer getNiveau() {
        return niveau;
    }

    public void setNiveau(Integer niveau) {
        this.niveau = niveau;
    }

    public String getIdMachine() {
        return idMachine;
    }

    public void setIdMachine(String idMachine) {
        this.idMachine = idMachine;
    }

    public String getOperateur() {
        return operateur;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public String getIdBcFille() {
        return idBcFille;
    }

    public void setIdBcFille(String idBcFille) {
        this.idBcFille = idBcFille;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }
}
