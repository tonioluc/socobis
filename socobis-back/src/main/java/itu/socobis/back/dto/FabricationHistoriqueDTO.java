package itu.socobis.back.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour l'historique de fabrication.
 */
public class FabricationHistoriqueDTO {

    private String id;
    private String produitId;
    private String produitLibelle;
    private BigDecimal quantite;
    private String unite;
    private LocalDateTime dateFabrication;
    private String statut;
    private List<FabricationLigneDTO> lignes;

    public FabricationHistoriqueDTO() {}

    // Getters & Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProduitId() {
        return produitId;
    }

    public void setProduitId(String produitId) {
        this.produitId = produitId;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
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

    public LocalDateTime getDateFabrication() {
        return dateFabrication;
    }

    public void setDateFabrication(LocalDateTime dateFabrication) {
        this.dateFabrication = dateFabrication;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public List<FabricationLigneDTO> getLignes() {
        return lignes;
    }

    public void setLignes(List<FabricationLigneDTO> lignes) {
        this.lignes = lignes;
    }

    /**
     * DTO pour une ligne de fabrication.
     */
    public static class FabricationLigneDTO {
        private String ingredientId;
        private String ingredientLibelle;
        private String type;
        private BigDecimal quantiteUtilisee;
        private String unite;

        public FabricationLigneDTO() {}

        public String getIngredientId() {
            return ingredientId;
        }

        public void setIngredientId(String ingredientId) {
            this.ingredientId = ingredientId;
        }

        public String getIngredientLibelle() {
            return ingredientLibelle;
        }

        public void setIngredientLibelle(String ingredientLibelle) {
            this.ingredientLibelle = ingredientLibelle;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public BigDecimal getQuantiteUtilisee() {
            return quantiteUtilisee;
        }

        public void setQuantiteUtilisee(BigDecimal quantiteUtilisee) {
            this.quantiteUtilisee = quantiteUtilisee;
        }

        public String getUnite() {
            return unite;
        }

        public void setUnite(String unite) {
            this.unite = unite;
        }
    }
}
