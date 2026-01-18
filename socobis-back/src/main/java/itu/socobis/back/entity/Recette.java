package itu.socobis.back.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entité mappant la table AS_RECETTE.
 * Représente la formule/nomenclature d'un produit (composition).
 */
@Entity
@Table(name = "AS_RECETTE")
public class Recette {

    @Id
    @Column(name = "ID", length = 50)
    private String id;

    /**
     * ID du produit parent (le produit composé/fabriqué).
     */
    @Column(name = "IDPRODUITS", length = 100)
    private String idProduits;

    /**
     * ID de l'ingrédient composant.
     */
    @Column(name = "IDINGREDIENTS", length = 100)
    private String idIngredients;

    /**
     * Quantité de l'ingrédient nécessaire pour fabriquer 1 unité du produit parent.
     */
    @Column(name = "QUANTITE", precision = 30, scale = 10)
    private BigDecimal quantite;

    @Column(name = "UNITE", length = 50)
    private String unite;

    // Relations (optionnelles pour enrichir les DTO)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDPRODUITS", referencedColumnName = "ID", insertable = false, updatable = false)
    private Ingredient produitParent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDINGREDIENTS", referencedColumnName = "ID", insertable = false, updatable = false)
    private Ingredient ingredientComposant;

    // Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdProduits() {
        return idProduits;
    }

    public void setIdProduits(String idProduits) {
        this.idProduits = idProduits;
    }

    public String getIdIngredients() {
        return idIngredients;
    }

    public void setIdIngredients(String idIngredients) {
        this.idIngredients = idIngredients;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public Ingredient getProduitParent() {
        return produitParent;
    }

    public void setProduitParent(Ingredient produitParent) {
        this.produitParent = produitParent;
    }

    public Ingredient getIngredientComposant() {
        return ingredientComposant;
    }

    public void setIngredientComposant(Ingredient ingredientComposant) {
        this.ingredientComposant = ingredientComposant;
    }

    /**
     * Alias pour getIngredientComposant().
     */
    public Ingredient getIngredient() {
        return ingredientComposant;
    }
}
