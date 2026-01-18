package itu.socobis.back.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour la simulation/calcul des besoins de fabrication.
 */
public class SimulationFabricationDTO {

    private String produitId;
    private String produitLibelle;
    private String produitUnite;
    private BigDecimal quantiteAFabriquer;
    private List<FormuleItemDTO> besoins;
    private List<FormuleItemDTO> intermediairesManquants;
    private boolean peutFabriquer;
    private String message;

    public SimulationFabricationDTO() {}

    // Getters & Setters

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

    public String getProduitUnite() {
        return produitUnite;
    }

    public void setProduitUnite(String produitUnite) {
        this.produitUnite = produitUnite;
    }

    public BigDecimal getQuantiteAFabriquer() {
        return quantiteAFabriquer;
    }

    public void setQuantiteAFabriquer(BigDecimal quantiteAFabriquer) {
        this.quantiteAFabriquer = quantiteAFabriquer;
    }

    public List<FormuleItemDTO> getBesoins() {
        return besoins;
    }

    public void setBesoins(List<FormuleItemDTO> besoins) {
        this.besoins = besoins;
    }

    public List<FormuleItemDTO> getIntermediairesManquants() {
        return intermediairesManquants;
    }

    public void setIntermediairesManquants(List<FormuleItemDTO> intermediairesManquants) {
        this.intermediairesManquants = intermediairesManquants;
    }

    public boolean isPeutFabriquer() {
        return peutFabriquer;
    }

    public void setPeutFabriquer(boolean peutFabriquer) {
        this.peutFabriquer = peutFabriquer;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
