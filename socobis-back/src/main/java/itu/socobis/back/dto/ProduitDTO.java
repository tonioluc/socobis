package itu.socobis.back.dto;

import java.math.BigDecimal;

/**
 * DTO représentant un produit (ingrédient/matière première/produit fini).
 */
public class ProduitDTO {

    private String id;
    private String libelle;
    private String unite;
    private BigDecimal stock;
    private String type; // MATIERE_PREMIERE, INTERMEDIAIRE, FINI, CHARGE
    private String stockStatus; // OK, BAS, RUPTURE
    private BigDecimal seuilMin;
    private BigDecimal seuilMax;
    private BigDecimal pu;
    private BigDecimal pv;
    private String categorie;

    public ProduitDTO() {}

    public ProduitDTO(String id, String libelle, String unite, BigDecimal stock, String type) {
        this.id = id;
        this.libelle = libelle;
        this.unite = unite;
        this.stock = stock;
        this.type = type;
    }

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

    public BigDecimal getStock() {
        return stock;
    }

    public void setStock(BigDecimal stock) {
        this.stock = stock;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
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

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }
}
