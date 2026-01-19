package vente;

import bean.ClassFille;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VenteRetourDetail extends ClassFille {

    String id;
    String idVenteRetour;
    String idProduit;
    String idOrigine;
    String designation;
    String idDevise;
    double qte;
    double pu;
    double tva;
    double tauxDeChange;

    public VenteRetourDetail() {
        super.setNomTable("vente_retour_detail");
        try {
            this.setNomClasseMere("vente.VenteRetourClient");
            this.setLiaisonMere("idVenteRetour");
        } catch (Exception ex) {
            Logger.getLogger(VenteDetails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getTuppleID() {
        return this.getId();
    }

    @Override
    public void construirePK(Connection c) throws Exception {
        this.preparePk("VRTCD", "SEQVENTERETOURDETAILS.NEXTVAL");
        this.setId(makePK(c));
    }

    @Override
    public void setLiaisonMere(String liaisonMere) {
        super.setLiaisonMere("idVenteRetour");
    }

    @Override
    public String getNomClasseMere() {
        return "vente.VenteRetourClient";
    }

    @Override
    public String getAttributIDName() {
        return "id";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdVenteRetour() {
        return idVenteRetour;
    }

    public void setIdVenteRetour(String idVenteRetour) {
        this.idVenteRetour = idVenteRetour;
    }

    public String getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(String idProduit) {
        this.idProduit = idProduit;
    }

    public String getIdOrigine() {
        return idOrigine;
    }

    public void setIdOrigine(String idOrigine) {
        this.idOrigine = idOrigine;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getIdDevise() {
        return idDevise;
    }

    public void setIdDevise(String idDevise) {
        this.idDevise = idDevise;
    }

    public double getQte() {
        return qte;
    }

    public void setQte(double qte) {
        this.qte = qte;
    }

    public double getPu() {
        return pu;
    }

    public void setPu(double pu) {
        this.pu = pu;
    }

    public double getTva() {
        return tva;
    }

    public void setTva(double tva) {
        this.tva = tva;
    }

    public double getTauxDeChange() {
        return tauxDeChange;
    }

    public void setTauxDeChange(double tauxDeChange) {
        this.tauxDeChange = tauxDeChange;
    }
}
