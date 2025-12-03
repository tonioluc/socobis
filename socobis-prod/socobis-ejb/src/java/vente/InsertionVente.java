/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vente;

import bean.ClassMAPTable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *
 * @author Sahy
 */
public class InsertionVente extends Vente{
    String idDevise;
    String idVenteOrig;
    String changerEtPayer; // "1" si Changer et payer
    // Montant TTC calculé côté JS (newMontant) pour le cas Changer et payer
    double newMontant;
    double oldTotal;
    String idDetail; // ID de la ligne de détail à changer

    public String getIdDetail() {
        return idDetail;
    }

    public void setIdDetail(String idDetail) {
        this.idDetail = idDetail;
    }

    public String getIdVenteOrig() {
        return idVenteOrig;
    }

    public void setIdVenteOrig(String idVenteOrig) {
        this.idVenteOrig = idVenteOrig;
    }

    public String getIdDevise() {
        return idDevise;
    }

    public void setIdDevise(String idDevise) {
        this.idDevise = idDevise;
    }

    public String getChangerEtPayer() {
        return changerEtPayer;
    }

    public void setChangerEtPayer(String changerEtPayer) {
        this.changerEtPayer = changerEtPayer;
    }

    public double getNewMontant() {
        return newMontant;
    }

    public void setNewMontant(double newMontant) {
        this.newMontant = newMontant;
    }

    public double getOldTotal() {
        return oldTotal;
    }

    public void setOldTotal(double oldTotal) {
        this.oldTotal = oldTotal;
    }

    public InsertionVente() {
        this.setNomTable("INSERTION_VENTE");
    }
    
    @Override
    public ClassMAPTable createObject(String u, Connection c) throws Exception {
        super.setNomTable("VENTE");
        ClassMAPTable res = super.createObject(u, c);

        if(this.getIdVenteOrig() != null && !this.getIdVenteOrig().isEmpty()){
            Vente venteOrig = new Vente();
            venteOrig.setId(this.getIdVenteOrig());
            Vente venteOrigComplete = (Vente) venteOrig.getById(venteOrig.getId(), "VENTE", c);
            
            // if(venteOrigComplete.getEtat() <= 12){
            //     throw new Exception("Impossible de changer une facture non payée/livrée (état " + venteOrigComplete.getEtat() + "). La facture doit être au minimum livrée.");
            // }
            
            String idMagasinAnnulation = this.getIdMagasin();
            venteOrigComplete.annulerVenteCustom(u, c, idMagasinAnnulation);
            // insert into VENTE_LIEN_CHANGEMENT
            String sql = "INSERT INTO VENTE_LIEN_CHANGEMENT (ID_ANCIENNE_VENTE, ID_NOUVELLE_VENTE, DATE_CHANGEMENT) VALUES (?, ?, SYSDATE)";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, this.getIdVenteOrig());
            ps.setString(2, this.getId());
            ps.executeUpdate();
            ps.close();
            //eto miviser anazy
            this.validerObject(u, c);

            // Set idOrigine for payment logic
            this.setIdOrigine(this.getIdVenteOrig());

            // Validations - vérifier le changeable uniquement pour le produit de la ligne changée
            if(this.getIdDetail() != null && !this.getIdDetail().isEmpty()) {
                VenteDetails detailOriginal = new VenteDetails();
                detailOriginal.setId(this.getIdDetail());
                VenteDetails[] detailsOrig = (VenteDetails[]) bean.CGenUtil.rechercher(detailOriginal, null, null, c, "");
                
                if(detailsOrig != null && detailsOrig.length > 0) {
                    String idProduitAChanger = detailsOrig[0].getIdProduit();
                    
                    String sqlChangeable = "SELECT changeable FROM AS_INGREDIENTS WHERE id = ?";
                    PreparedStatement psChangeable = c.prepareStatement(sqlChangeable);
                    psChangeable.setString(1, idProduitAChanger);
                    ResultSet rs = psChangeable.executeQuery();
                    
                    if(rs.next()){
                        int changeable = rs.getInt("changeable");
                        if(changeable == 0){
                            throw new Exception("Le produit " + idProduitAChanger + " ne peut pas être changé.");
                        }
                        if(changeable == 1 && this.getNewMontant() <= this.getOldTotal()){
                            throw new Exception("Le montant de la nouvelle vente (" + this.getNewMontant() + " Ar) doit être supérieur à l'ancienne vente (" + this.getOldTotal() + " Ar) pour ce produit.");
                        }
                    }
                    rs.close();
                    psChangeable.close();
                }
            }
        }

        if ("1".equals(this.getChangerEtPayer())) {
            this.payerAvecTTC(u, c, this.getNewMontant(), this.getOldTotal());
        }
        return res;
    }
}
