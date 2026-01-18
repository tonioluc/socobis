package itu.socobis.back.dto;

import java.math.BigDecimal;

/**
 * DTO représentant un élément de la formule (nomenclature) d'un produit.
 */
public class FormuleItemDTO {

    private String itemId;
    private String libelle;
    private String type; // MATIERE_PREMIERE, INTERMEDIAIRE, CHARGE
    private String unite;
    private BigDecimal qteParUnite; // quantité nécessaire pour 1 unité du produit parent
    private BigDecimal besoinTotal; // qteParUnite * quantité à fabriquer
    private BigDecimal stockDisponible;
    private BigDecimal manquant;
    private boolean suffisant;

    public FormuleItemDTO() {}

    // Getters & Setters

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public BigDecimal getQteParUnite() {
        return qteParUnite;
    }

    public void setQteParUnite(BigDecimal qteParUnite) {
        this.qteParUnite = qteParUnite;
    }

    public BigDecimal getBesoinTotal() {
        return besoinTotal;
    }

    public void setBesoinTotal(BigDecimal besoinTotal) {
        this.besoinTotal = besoinTotal;
    }

    public BigDecimal getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(BigDecimal stockDisponible) {
        this.stockDisponible = stockDisponible;
    }

    public BigDecimal getManquant() {
        return manquant;
    }

    public void setManquant(BigDecimal manquant) {
        this.manquant = manquant;
    }

    public boolean isSuffisant() {
        return suffisant;
    }

    public void setSuffisant(boolean suffisant) {
        this.suffisant = suffisant;
    }
}
