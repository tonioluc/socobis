package itu.socobis.back.dto;

import java.math.BigDecimal;

/**
 * DTO pour la demande de fabrication.
 */
public class FabricationRequestDTO {

    private String produitId;
    private BigDecimal quantite;

    public FabricationRequestDTO() {}

    public FabricationRequestDTO(String produitId, BigDecimal quantite) {
        this.produitId = produitId;
        this.quantite = quantite;
    }

    public String getProduitId() {
        return produitId;
    }

    public void setProduitId(String produitId) {
        this.produitId = produitId;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }
}
