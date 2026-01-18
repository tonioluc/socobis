package itu.socobis.back.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entité mappant la table AS_INGREDIENTS.
 * Représente les matières premières, produits intermédiaires et produits finis.
 */
@Entity
@Table(name = "AS_INGREDIENTS")
public class Ingredient {

    @Id
    @Column(name = "ID", length = 50)
    private String id;

    @Column(name = "LIBELLE", length = 250)
    private String libelle;

    @Column(name = "UNITE", length = 100)
    private String unite;

    @Column(name = "PU", precision = 10, scale = 2)
    private BigDecimal pu;

    @Column(name = "PV", precision = 30, scale = 2)
    private BigDecimal pv;

    @Column(name = "SEUIL", precision = 10, scale = 2)
    private BigDecimal seuil;

    @Column(name = "SEUILMIN", precision = 30, scale = 2)
    private BigDecimal seuilMin;

    @Column(name = "SEUILMAX", precision = 30, scale = 2)
    private BigDecimal seuilMax;

    @Column(name = "RESTE", precision = 20, scale = 2)
    private BigDecimal reste;

    /**
     * 0 = matière première simple
     * 1 = produit composé (intermédiaire ou fini)
     */
    @Column(name = "COMPOSE")
    private Integer compose;

    @Column(name = "ACTIF")
    private Integer actif;

    @Column(name = "CATEGORIEINGREDIENT", length = 100)
    private String categorieIngredient;

    @Column(name = "IDFAMILLE", length = 100)
    private String idFamille;

    @Column(name = "IDFOURNISSEUR", length = 100)
    private String idFournisseur;

    @Column(name = "DATY")
    private LocalDate daty;

    @Column(name = "ISVENTE")
    private Integer isVente;

    @Column(name = "ISACHAT")
    private Integer isAchat;

    @Column(name = "TVA", precision = 10, scale = 2)
    private BigDecimal tva;

    @Column(name = "TYPESTOCK", length = 4)
    private String typeStock;

    @Column(name = "IDMAGASIN", length = 50)
    private String idMagasin;

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

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public BigDecimal getPu() {
        return pu;
    }

    public void setPu(BigDecimal pu) {
        this.pu = pu;
    }

    public BigDecimal getPv() {
        return pv;
    }

    public void setPv(BigDecimal pv) {
        this.pv = pv;
    }

    public BigDecimal getSeuil() {
        return seuil;
    }

    public void setSeuil(BigDecimal seuil) {
        this.seuil = seuil;
    }

    public BigDecimal getSeuilMin() {
        return seuilMin;
    }

    public void setSeuilMin(BigDecimal seuilMin) {
        this.seuilMin = seuilMin;
    }

    public BigDecimal getSeuilMax() {
        return seuilMax;
    }

    public void setSeuilMax(BigDecimal seuilMax) {
        this.seuilMax = seuilMax;
    }

    public BigDecimal getReste() {
        return reste;
    }

    public void setReste(BigDecimal reste) {
        this.reste = reste;
    }

    public Integer getCompose() {
        return compose;
    }

    public void setCompose(Integer compose) {
        this.compose = compose;
    }

    public Integer getActif() {
        return actif;
    }

    public void setActif(Integer actif) {
        this.actif = actif;
    }

    public String getCategorieIngredient() {
        return categorieIngredient;
    }

    public void setCategorieIngredient(String categorieIngredient) {
        this.categorieIngredient = categorieIngredient;
    }

    public String getIdFamille() {
        return idFamille;
    }

    public void setIdFamille(String idFamille) {
        this.idFamille = idFamille;
    }

    public String getIdFournisseur() {
        return idFournisseur;
    }

    public void setIdFournisseur(String idFournisseur) {
        this.idFournisseur = idFournisseur;
    }

    public LocalDate getDaty() {
        return daty;
    }

    public void setDaty(LocalDate daty) {
        this.daty = daty;
    }

    public Integer getIsVente() {
        return isVente;
    }

    public void setIsVente(Integer isVente) {
        this.isVente = isVente;
    }

    public Integer getIsAchat() {
        return isAchat;
    }

    public void setIsAchat(Integer isAchat) {
        this.isAchat = isAchat;
    }

    public BigDecimal getTva() {
        return tva;
    }

    public void setTva(BigDecimal tva) {
        this.tva = tva;
    }

    public String getTypeStock() {
        return typeStock;
    }

    public void setTypeStock(String typeStock) {
        this.typeStock = typeStock;
    }

    public String getIdMagasin() {
        return idMagasin;
    }

    public void setIdMagasin(String idMagasin) {
        this.idMagasin = idMagasin;
    }

    // ===== Méthodes de commodité =====

    /**
     * Alias pour getReste() - représente le stock disponible.
     */
    public BigDecimal getStock() {
        return reste;
    }

    /**
     * Alias pour setReste() - met à jour le stock.
     */
    public void setStock(BigDecimal stock) {
        this.reste = stock;
    }

    /**
     * Détermine le type d'ingrédient basé sur les champs compose et isVente.
     * PF = Produit Fini (composé + vendable)
     * PI = Produit Intermédiaire (composé + non vendable)
     * MP = Matière Première (non composé)
     */
    public String getTypeIngredient() {
        if (compose != null && compose == 1) {
            if (isVente != null && isVente == 1) {
                return "PF"; // Produit Fini
            }
            return "PI"; // Produit Intermédiaire
        }
        return "MP"; // Matière Première
    }

    /**
     * Prix d'achat (alias pour getPu()).
     */
    public BigDecimal getPuAchat() {
        return pu;
    }

    /**
     * Prix de vente (alias pour getPv()).
     */
    public BigDecimal getPuVente() {
        return pv;
    }

    /**
     * Catégorie (alias pour getCategorieIngredient()).
     */
    public String getCategorie() {
        return categorieIngredient;
    }
}
