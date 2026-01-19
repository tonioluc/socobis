/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package faturefournisseur;

import bean.AdminGen;
import bean.CGenUtil;
import bean.ClassMAPTable;
import bean.ResultatEtSomme;
import caisse.Caisse;
import caisse.MvtCaisse;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import chatbot.FilleOcr;
import chatbot.MereOcr;
import magasin.Magasin;
import mg.cnaps.compta.ComptaEcriture;
import mg.cnaps.compta.ComptaSousEcriture;
import prevision.Prevision;
import utilitaire.ConstanteEtat;
import produits.Ingredients;
import utilitaire.UtilDB;
import utilitaire.Utilitaire;
import utils.ConstanteStation;
import vente.As_BondeLivraisonClient;
import vente.As_BondeLivraisonClientFille;
import vente.Vente;
import vente.VenteDetailsLib;

/**
 *
 * @author nouta
 */
public class FactureFournisseur extends vente.FactureCF {
    protected String idFournisseur, idModePaiement, reference, idBc, idMagasin, fournisseurlib;
    protected Date dateEcheancePaiement;
    protected String devise;
    protected double taux;
    protected String idDevise;
    protected String compte, compteauxiliaire;
    int estPrevu;
    private double montantPerteGain;
    String detailAchatMultiple;

    public String getDetailAchatMultiple() {
        return detailAchatMultiple;
    }

    public void setDetailAchatMultiple(String detailAchatMultiple) {
        this.detailAchatMultiple = detailAchatMultiple;
    }

    public String getCompteauxiliaire() {
        return compteauxiliaire;
    }

    public void setCompteauxiliaire(String compteauxiliaire) {
        this.compteauxiliaire = compteauxiliaire;
    }

    public static FactureFournisseur fromOcr(MereOcr mere) throws Exception {
        FactureFournisseur f = new FactureFournisseur();
        f.setDesignation(mere.getDesignation());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        LocalDate localDate = LocalDate.parse(mere.getDaty(), formatter);
        Date sqlDate = Date.valueOf(localDate);
        f.setDaty(sqlDate);

        Fournisseur fournisseur = new Fournisseur();
        Fournisseur[] fournisseurs = (Fournisseur[]) CGenUtil.rechercher(fournisseur, null, null,
                " and upper(nom) like '%" + mere.getIdOrigine().toUpperCase() + "%'");
        if (fournisseurs != null && fournisseurs.length > 0) {
            f.setIdFournisseur(fournisseurs[0].getId());
        }

        Magasin magasin = new Magasin();
        Magasin[] magasins = (Magasin[]) CGenUtil.rechercher(magasin, null, null,
                " and upper(val) like '%" + mere.getIdMagasin().toUpperCase() + "%'");
        if (magasins != null && magasins.length > 0) {
            f.setIdMagasin(magasins[0].getId());
        }

        FactureFournisseurDetails[] filles = fillesFromOcr(mere.getDetails().toArray(new FilleOcr[0]));
        f.setFille(filles);

        return f;
    }

    public static FactureFournisseurDetails[] fillesFromOcr(FilleOcr[] filles) throws Exception {
        FactureFournisseurDetails[] f = new FactureFournisseurDetails[filles.length];
        for (int i = 0; i < filles.length; i++) {
            f[i] = FactureFournisseurDetails.fromOcr(filles[i]);
        }
        return f;
    }

    @Override
    public String getLiaisonFille() {
        return "idFactureFournisseur";
    }

    @Override
    public String getNomClasseFille() {
        return "faturefournisseur.FactureFournisseurDetails";
    }

    @Override
    public String getSensPrev() {
        return "debit";
    }

    @Override
    public String getTiers() {
        return this.idFournisseur;
    }

    public int getEstPrevu() {
        return estPrevu;
    }

    public int isEstPrevu() {
        return estPrevu;
    }

    public void setEstPrevu(int estPrevu) {
        this.estPrevu = estPrevu;
    }

    public String getIdDevise() {
        return idDevise;
    }

    public void setIdDevise(String idDevise) {
        // Extraire le code devise si format composite "CODE|dateDebut|dateFin"
        if(idDevise != null && idDevise.contains("|")) {
            String[] parts = idDevise.split("\\|");
            this.idDevise = parts[0]; // Stocker uniquement le code
        } else {
            this.idDevise = idDevise;
        }
    }

    public FactureFournisseur(String nomtable) {
        setNomTable(nomtable);
    }

    public String getIdMagasin() {
        return idMagasin;
    }

    public void setIdMagasin(String idMagasin) {
        this.idMagasin = idMagasin;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public double getTaux() {
        if (taux == 0)
            return 1;
        return taux;
    }

    public void setTaux(double taux) {
        if (taux == 0 & this.getMode().compareToIgnoreCase("modif") == 0)
            taux = 1;
        this.taux = taux;
    }

    public FactureFournisseur() {
        super.setNomTable("FACTUREFOURNISSEUR");
    }

    public void construirePK(Connection c) throws Exception {
        this.preparePk("FCF", "GETSEQFACTUREFOURNISSEUR");
        this.setId(makePK(c));
    }

    public String getIdFournisseur() {
        return idFournisseur;
    }

    public void setIdFournisseur(String idFournisseur) {
        this.idFournisseur = idFournisseur;
    }

    public String getIdModePaiement() {
        return idModePaiement;
    }

    public void setIdModePaiement(String idModePaiement) {
        this.idModePaiement = idModePaiement;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIdBc() {
        return idBc;
    }

    public void setIdBc(String idBc) {
        this.idBc = idBc;
    }

    public Date getDateEcheancePaiement() {
        return dateEcheancePaiement;
    }

    public void setDateEcheancePaiement(Date dateEcheancePaiement) {
        this.dateEcheancePaiement = dateEcheancePaiement;
    }

    public String getCompte() {
        return compte;
    }

    public void setCompte(String compte) {
        this.compte = compte;
    }

    public String getFournisseurlib() {
        return fournisseurlib;
    }

    public void setFournisseurlib(String fournisseurlib) {
        this.fournisseurlib = fournisseurlib;
    }

    public double getMontantPerteGain() {
        return montantPerteGain;
    }

    public void setMontantPerteGain(double montantPerteGain) {
        this.montantPerteGain = montantPerteGain;
    }

    @Override
    public String getTuppleID() {
        return id;
    }

    @Override
    public String getAttributIDName() {
        return "id";
    }

    public Prevision genererPrevision(String u, Connection c) throws Exception {
        Prevision mere = new Prevision();
        // Utiliser FactureFournisseurCpl pour avoir accès à tauxdechange
        FactureFournisseurCpl factureWithMontant = (FactureFournisseurCpl) new FactureFournisseurCpl().getById(this.getId(), "FACTUREFOURNISSEURCPL", c);
        mere.setDaty(getDatyPrevu());
        // Utiliser le montant brut (non converti) et la devise originale
        mere.setDebit(factureWithMontant.getMontantttc());
        mere.setIdFacture(this.id);
        mere.setIdCaisse(ConstanteStation.idCaisse);
        mere.setDesignation("Prevision rattachée au FF N"+this.getId());
        // Utiliser la devise de la facture (pas forcément AR)
        String deviseFacture = factureWithMontant.getIdDevise();
        if(deviseFacture == null || deviseFacture.isEmpty()) {
            deviseFacture = "AR";
        }
        mere.setIdDevise(deviseFacture);
        // Récupérer le taux depuis tauxdechange de la vue CPL
        double tauxFacture = factureWithMontant.getTauxdechange();
        mere.setTaux(tauxFacture > 0 ? tauxFacture : 1.0);
        System.out.println("=== genererPrevision: devise=" + deviseFacture + ", taux=" + tauxFacture + ", montant=" + factureWithMontant.getMontantttc() + " ===");
        mere.setIdTiers(this.getIdFournisseur());
        return (Prevision) mere.createObject(u, c);
    }
    
    /**
     * Génère plusieurs prévisions à partir d'une chaîne de plan de paiement
     * Format de la chaîne: "dd/MM/yyyy:pourcentage, dd/MM/yyyy:pourcentage, ..."
     * Exemple: "07/12/2025:30, 12/12/2025:40, 30/12/2025:30"
     * 
     * @param u - utilisateur
     * @param c - connexion
     * @param planPaiementStr - chaîne au format "date:pourcentage, date:pourcentage, ..."
     * @return tableau des prévisions créées
     * @throws Exception
     */
    public Prevision[] genererPrevisionsFromPlanPaiement(String u, Connection c, String planPaiementStr) throws Exception {
        if(planPaiementStr == null || planPaiementStr.trim().isEmpty()) {
            throw new Exception("Le plan de paiement ne peut pas être vide");
        }
        
        // Récupérer les infos de la facture
        FactureFournisseurCpl factureWithMontant = (FactureFournisseurCpl) new FactureFournisseurCpl().getById(this.getId(), "FACTUREFOURNISSEURCPL", c);
        double montantTotal = factureWithMontant.getMontantttc();
        String deviseFacture = factureWithMontant.getIdDevise();
        if(deviseFacture == null || deviseFacture.isEmpty()) {
            deviseFacture = "AR";
        }
        double tauxFacture = factureWithMontant.getTauxdechange();
        if(tauxFacture <= 0) {
            tauxFacture = 1.0;
        }
        
        // Parser la chaîne de plan de paiement
        String[] entries = planPaiementStr.split(",");
        java.util.List<Prevision> previsions = new java.util.ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        
        double totalPourcentage = 0;
        
        for(String entry : entries) {
            entry = entry.trim();
            if(entry.isEmpty()) continue;
            
            String[] parts = entry.split(":");
            if(parts.length != 2) {
                throw new Exception("Format invalide pour l'entrée: " + entry + ". Format attendu: dd/MM/yyyy:pourcentage");
            }
            
            String dateStr = parts[0].trim();
            String pourcentageStr = parts[1].trim();
            
            // Parser la date
            java.util.Date dateParsed = sdf.parse(dateStr);
            java.sql.Date dateSql = new java.sql.Date(dateParsed.getTime());
            
            // Parser le pourcentage
            double pourcentage = Double.parseDouble(pourcentageStr);
            totalPourcentage += pourcentage;
            
            // Calculer le montant pour cette prévision
            double montantPrevision = montantTotal * (pourcentage / 100.0);
            
            // Créer la prévision
            Prevision prev = new Prevision();
            prev.setDaty(dateSql);
            prev.setDebit(montantPrevision);
            prev.setIdFacture(this.id);
            prev.setIdCaisse(ConstanteStation.idCaisse);
            prev.setDesignation("Prevision " + (int)pourcentage + "% rattachée au FF N" + this.getId() + " - Echéance " + dateStr);
            prev.setIdDevise(deviseFacture);
            prev.setTaux(tauxFacture);
            prev.setIdTiers(this.getIdFournisseur());
            
            Prevision createdPrev = (Prevision) prev.createObject(u, c);
            previsions.add(createdPrev);
            
            System.out.println("=== genererPrevisionsFromPlanPaiement: créé prévision " + dateStr + " à " + pourcentage + "% = " + montantPrevision + " " + deviseFacture + " ===");
        }
        
        // Vérifier que le total des pourcentages est cohérent (optionnel: avertissement si != 100)
        if(Math.abs(totalPourcentage - 100.0) > 0.01) {
            System.out.println("=== ATTENTION: Le total des pourcentages (" + totalPourcentage + "%) n'est pas égal à 100% ===");
        }
        
        return previsions.toArray(new Prevision[0]);
    }

    public FactureFournisseur getFactureWithMontant(Connection c) throws Exception {
        return (FactureFournisseur) new FactureFournisseur().getById(this.getId(), "FACTUREFOURNISSEURCPL", c);
    }

    public void genererEcritureDecaissement(String u, Connection c) throws Exception {
        ComptaEcriture mere = new ComptaEcriture();
        Date dateDuJour = utilitaire.Utilitaire.dateDuJourSql();
        int exercice = utilitaire.Utilitaire.getAnnee(getDaty());
        mere.setDaty(dateDuJour);
        mere.setDesignation(this.getDesignation());
        mere.setExercice("" + exercice);
        mere.setDateComptable(this.getDaty());
        mere.setJournal(ConstanteStation.journalAchat);
        mere.setOrigine(this.getId());
        mere.setIdobjet(this.getId());
        mere.createObject(u, c);
        ComptaSousEcriture[] filles = this.genererSousEcritureDecaissement(c);
        for (int i = 0; i < filles.length; i++) {
            filles[i].setIdMere(mere.getId());
            filles[i].setExercice(exercice);
            filles[i].setDaty(this.getDaty());
            filles[i].setJournal(ConstanteStation.journalAchat);

            if (filles[i].getDebit() > 0 || filles[i].getCredit() > 0)
                filles[i].createObject(u, c);
        }
    }

    public ComptaSousEcriture[] genererSousEcritureDecaissement(Connection c) throws Exception {
        ComptaSousEcriture[] compta = {};
        boolean canClose = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                canClose = true;
            }
            FactureFournisseur[] facturefournisseurs = (FactureFournisseur[]) CGenUtil.rechercher(
                    new FactureFournisseur("FACTUREFOURNISSEUR_MERECMPT"), null, null, c,
                    " and id = '" + this.getId() + "'");
            if (facturefournisseurs.length < 1)
                throw new Exception("Facture mere Introuvable");
            this.setCompte(facturefournisseurs[0].getCompte());

            compta = new ComptaSousEcriture[2];

            compta[0] = new ComptaSousEcriture();
            compta[0].setLibellePiece(this.getDesignation());
            compta[0].setRemarque(this.getDesignation());
            compta[0].setCompte(getCaisse(c).getCompte());
            compta[0].setCredit(facturefournisseurs[0].getMontantttc());

            compta[1] = new ComptaSousEcriture();
            compta[1].setLibellePiece("Decaissement fournisseur " + facturefournisseurs[0].getFournisseurlib());
            compta[1].setRemarque("Decaissement fournisseur " + facturefournisseurs[0].getFournisseurlib());
            compta[1].setCompte(this.getCompte());
            // compta[i].setDebit((montantHT-retenue) * ((this.getTva()/100)));
            compta[1].setDebit(facturefournisseurs[0].getMontantttc());

        } catch (Exception e) {
            throw e;
        } finally {
            if (canClose) {
                c.close();
            }
        }
        return compta;
    }

    public Caisse getCaisse(Connection c) throws Exception {
        if (c == null) {
            throw new Exception("Connection non etablie");
        }
        Caisse caisse = new Caisse();
        Caisse[] caisses = (Caisse[]) CGenUtil.rechercher(caisse, null, null, c,
                " and idMagasin = '" + this.getIdMagasin() + "'");
        if (caisses.length > 0) {
            return caisses[0];
        }
        return null;
    }

    public Fournisseur getFournisseur(Connection c) throws Exception {
        Fournisseur crt = new Fournisseur();
        crt.setId(this.getIdFournisseur());
        Fournisseur[] liste = (Fournisseur[]) CGenUtil.rechercher(crt, null, null, c, "");
        if (liste.length == 0)
            return null;
        return liste[0];
    }

    public void genererEcriture(String u, Connection c) throws Exception {
        Date dateDuJour = Utilitaire.dateDuJourSql();
        int exercice = this.getDaty().getYear() + 1900;
        ComptaEcriture mere = new ComptaEcriture();
        // System.out.println("Daty "+this.getDaty().getYear()+" designation
        // "+this.getDesignation()+" id "+this.getId());
        mere.setDaty(this.getDaty());
        mere.setDesignation("Ecriture liee a la facture " + this.getDesignation() + " ref : " + this.getId());
        mere.setExercice("" + exercice);
        mere.setDateComptable(this.getDaty());
        mere.setJournal(ConstanteStation.journalAchat);
        mere.setIdobjet(this.getId());
        mere.setOrigine(this.getId());
        mere.createObject(u, c);
        ComptaSousEcriture[] filles = this.genererSousEcriture(c);
        for (int i = 0; i < filles.length; i++) {
            filles[i].setIdMere(mere.getId());
            filles[i].setExercice(exercice);
            filles[i].setDaty(this.getDaty());
            filles[i].setJournal(ConstanteStation.journalAchat);
            filles[i].createObject(u, c);
        }
    }

    public ComptaSousEcriture[] genererSousEcriture(Connection c) throws Exception {
        ComptaSousEcriture[] compta = {};
        boolean canClose = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                canClose = true;
            }
            // FactureFournisseur[] facturefournisseur = (FactureFournisseur[])
            // CGenUtil.rechercher(new FactureFournisseur("FACTUREFOURNISSEUR_MERECMPT"),
            // null, null, c, " and id = '"+this.getId()+"'");
            // if(facturefournisseur.length<1) throw new Exception("Facture Fournisseur mère
            // Introuvable");
            FactureFournisseurDetails details[] = this.getDetails(c);
            // this.setCompte(facturefournisseur[0].getCompte());
            double montantTva = AdminGen.calculSommeDouble(details, "montantTva") * details[0].getTauxDeChange();
            double montantHT = AdminGen.calculSommeDouble(details, "montantHT") * details[0].getTauxDeChange();
            double montantTTC = AdminGen.calculSommeDouble(details, "montantTTC") * details[0].getTauxDeChange();
            int taille = details.length;
            compta = new ComptaSousEcriture[taille + (montantTva > 0 ? 2 : 1)];
            int i = 0;
            for (i = i; i < taille; i++) {

                compta[i] = new ComptaSousEcriture();
                compta[i].setLibellePiece(this.getDesignation());
                compta[i].setRemarque(this.getDesignation());
                // System.out.println("COMPTE = "+details[i].getCompte()+" mo");
                compta[i].setCompte(details[i].getCompte());
                compta[i].setDebit(details[i].getMontantHT() * details[0].getTauxDeChange());
            }
            if (montantTva > 0) {
                compta[i] = new ComptaSousEcriture();
                compta[i].setLibellePiece("TVA Deductible");
                compta[i].setRemarque("TVA Deductible");
                compta[i].setCompte(ConstanteStation.compteTVADeductible);
                // compta[i].setDebit((montantHT-retenue) * ((this.getTva()/100)));
                compta[i].setDebit(montantTva * details[0].getTauxDeChange());
                i++;
            }

            Fournisseur fo = this.getFournisseur(c);
            compta[i] = new ComptaSousEcriture();
            compta[i].setLibellePiece("Achat Fournisseur " + fo.getNom());
            compta[i].setRemarque("Achat Fournisseur " + fo.getNom());
            compta[i].setCompte(fo.getCompte());
            compta[i].setCredit(montantTTC);
            compta[i].setCompte_aux(fo.getCompteauxiliaire());

        } catch (Exception e) {
            throw e;
        } finally {
            if (canClose) {
                c.close();
            }

        }
        return compta;
    }

    public FactureFournisseurDetails[] getDetails(Connection c) throws Exception {
        FactureFournisseurDetails[] ffdetails = null;
        try {
            FactureFournisseurDetails details = new FactureFournisseurDetails();
            details.setNomTable("FACTUREFOURNISSEUR_FILLE_VISE");
            String awhere = " and IDFACTUREFOURNISSEUR = '" + this.getId() + "'";
            ffdetails = (FactureFournisseurDetails[]) CGenUtil.rechercher(details, null, null, c, awhere);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ffdetails;
    }

    @Override
    public Object validerObject(String u, Connection c) throws Exception {
        super.validerObject(u, c);
        genererEcriture(u, c);
        if(this.getEstPrevu() == 1 && this.getDatyPrevu()!=null){
                FactureFournisseur factureWithMontant = getFactureWithMontant(c);
                String detailleVenteMultiple =this.getDetailAchatMultiple();
                System.out.println("...................................."+ detailleVenteMultiple);
                if(detailleVenteMultiple != null && !detailleVenteMultiple.trim().isEmpty()){
                    Map<Date, Double> dateTauxMap = FactureFournisseur.parseDateTauxList(detailleVenteMultiple, factureWithMontant.getMontantttcAr());
                    for (Map.Entry<Date, Double> entry : dateTauxMap.entrySet()) {
                        Date date = entry.getKey();
                        double montant = entry.getValue();
                        Prevision prevision = this.genererPrevisionMultiple(u, c, montant, date);
                        System.out.println("Prévision générée pour le " + date + " avec montant : " + montant);
                    }
                }
                else{
                    genererPrevision(u, c);
                }
            }
        FactureFournisseurDetails[] ffD=(FactureFournisseurDetails[]) this.getFille(null,c,"");
        for(int i=0;i<ffD.length;i++){
            ffD[i].modifPuIngredientsNew(u,c);
        }
        return this;
    }  

     public Prevision genererPrevisionMultiple(String u, Connection c, double montant, Date date) throws Exception{
        Prevision mere = new Prevision();
        mere.setDaty(date);
        mere.setDebit(montant);
        mere.setIdFacture(this.id);
        mere.setIdCaisse(ConstanteStation.idCaisse);
        mere.setDesignation("Prevision rattachée au FF N"+this.getId());
        mere.setIdDevise("AR");
        mere.setIdTiers(this.getIdFournisseur());
        return (Prevision) mere.createObject(u, c); 
    }

    // ANCIENNE VERSION - commentée
    // public static java.util.Map<java.sql.Date, Double> parseDateTauxList(String input, double montant) throws Exception {
    //         java.util.Map<java.sql.Date, Double> result = new java.util.HashMap<>();
    //         if (input == null || input.trim().isEmpty()) {
    //             return result;
    //         }
    //         java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
    //         sdf.setLenient(false);
    //         String[] entries = input.split(";");
    //         double totalPourcentage = 0.0;
    //         for (String raw : entries) {
    //             if (raw == null) continue;
    //             String entry = raw.trim();
    //             if (entry.isEmpty()) continue;
    //             String[] parts = entry.split(":");
    //             if (parts.length != 2) {
    //                 throw new Exception("Format invalide: '" + entry + "'. Attendu 'jj/mm/yyyy:taux'");
    //             }
    //             String dateStr = parts[0].trim();
    //             String tauxStr = parts[1].trim();
    //             java.util.Date utilDate = sdf.parse(dateStr);
    //             java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
    //             double taux;
    //             try {
    //                 taux = Double.parseDouble(tauxStr.replace(',', '.'));
    //             } catch (NumberFormatException nfe) {
    //                 throw new Exception("Taux invalide pour '" + entry + "': " + tauxStr);
    //             }
    //             totalPourcentage += taux;
    //             result.put(sqlDate, montant * taux/100);
    //         }
    //    
    //         if (Math.abs(totalPourcentage - 100.0) > 1e-9) {
    //             throw new Exception("La somme des pourcentages doit être exactement 100 (actuelle: " + totalPourcentage + ")");
    //         }
    //         return result;
    // }

    // NOUVELLE VERSION - répartition équitable entre dates
    public static java.util.Map<java.sql.Date, Double> parseDateTauxList(String input, double montant) throws Exception {
            java.util.Map<java.sql.Date, Double> result = new java.util.HashMap<>();
            if (input == null || input.trim().isEmpty()) {
                return result;
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            String[] entries = input.split(";");
            
            // Liste pour stocker les dates valides
            java.util.List<java.sql.Date> dates = new java.util.ArrayList<>();
            
            for (String raw : entries) {
                if (raw == null) continue;
                String dateStr = raw.trim();
                if (dateStr.isEmpty()) continue;
                
                try {
                    java.util.Date utilDate = sdf.parse(dateStr);
                    java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
                    dates.add(sqlDate);
                } catch (java.text.ParseException pe) {
                    throw new Exception("Format de date invalide: '" + dateStr + "'. Attendu 'dd/MM/yyyy'");
                }
            }
            
            if (dates.isEmpty()) {
                return result;
            }
            
            // Répartir équitablement le montant entre toutes les dates
            double montantParDate = montant / dates.size();
            
            for (java.sql.Date date : dates) {
                result.put(date, montantParDate);
            }
            
            return result;
    }


    public FactureFournisseurCpl getFactureFournisseurCpl(Connection c) throws Exception {
        if (c == null) {
            throw new Exception("Connection non etablie");
        }
        FactureFournisseurCpl fact = new FactureFournisseurCpl();
        fact.setNomTable("FACTUREFOURNISSEURCPL_MONTANT");
        fact.setId(this.getId());
        FactureFournisseurCpl[] epts = (FactureFournisseurCpl[]) CGenUtil.rechercher(fact, null, null, c, " ");
        if (epts.length > 0) {
            return epts[0];
        }
        return null;
    }

    public String genererLivraison(String u, Connection c) throws Exception {
        boolean estOuvert = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                estOuvert = true;
            }

            FactureFournisseur ff = (FactureFournisseur) this.getById(this.getId(), this.getNomTable(), c);

            // CGenUtil.rechercher(f,null,null,c,"");
            return ff.genererBL(u, c).getId();
        } catch (Exception e) {
            throw e;
        } finally {
            if (estOuvert)
                c.close();
        }
    }

    private As_BonDeLivraison genererBL(String u, Connection c) throws Exception {
        boolean estOuvert = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                estOuvert = true;
            }
            As_BonDeLivraison bl = createBL();
            bl = (As_BonDeLivraison) bl.createObject(u, c);
            generateBLFille(bl, c, u);
            return bl;
        } catch (Exception e) {
            throw e;
        } finally {
            if (estOuvert)
                c.close();
        }
    }

    @Override
    public ClassMAPTable createObject(String u, Connection c) throws Exception {
        // this.setTaux(((FactureFournisseurDetails[])this.getFille())[0].getTaux());
        return super.createObject(u, c);
    }

    private void generateBLFille(As_BonDeLivraison bl, Connection c, String u) throws Exception {
        FactureFournisseurDetailResteALivrerLib[] details = getDetailsResteALivrer(c, "FFFILLERESTEALIVRER");
        for (FactureFournisseurDetailResteALivrerLib d : details) {
            As_BonDeLivraison_Fille blf = d.createBLFille(bl.getId());
            blf.createObject(u, c);
        }
    }

    public FactureFournisseurDetailResteALivrerLib[] getDetailsResteALivrer(Connection c, String nomtable)
            throws Exception {
        FactureFournisseurDetailResteALivrerLib[] ffdetails = null;

        if (nomtable == null) {
            nomtable = "FFFILLERESTEALIVRERLIB";
        }

        FactureFournisseurDetailResteALivrerLib details = new FactureFournisseurDetailResteALivrerLib();
        details.setNomTable(nomtable);
        ffdetails = (FactureFournisseurDetailResteALivrerLib[]) CGenUtil.rechercher(details, null, null, c,
                " and idfacturefournisseur = '" + this.getId() + "' and qte > 0");
        if (ffdetails.length == 0) {
            throw new Exception("Aucune livraison possible");
        }
        for (int i = 0; i < ffdetails.length; i++) {
            ffdetails[i].calculerTva();
            ffdetails[i].calculerHT();
            ffdetails[i].calculerTTC();
        }

        return ffdetails;
    }

    public As_BonDeLivraison createBL() throws Exception {
        As_BonDeLivraison bl = new As_BonDeLivraison();
        bl.setDaty(Utilitaire.dateDuJourSql());
        // bl.setIdbc(this.getId());
        bl.setMagasin(this.getIdMagasin());
        bl.setIdFournisseur(this.getIdFournisseur());
        bl.setIdFactureFournisseur(this.getId());
        bl.setRemarque(this.getDesignation());
        bl.setEtat(ConstanteEtat.getEtatCreer());
        return bl;
    }

    public FactureFournisseurDetailsCpl[] getFactureFournisseurDetails(Connection c) throws Exception {
        FactureFournisseurDetailsCpl obj = new FactureFournisseurDetailsCpl();
        obj.setNomTable("FACTUREFOURNISSEURFILLECPL");
        obj.setIdFactureFournisseur(this.getId());
        FactureFournisseurDetailsCpl[] objs = (FactureFournisseurDetailsCpl[]) CGenUtil.rechercher(obj, null, null, c,
                " ");
        if (objs.length > 0) {
            return objs;
        }
        return null;
    }

    public void genererAPartirLivraison(String[] ids, String u, Connection c) throws Exception {
        boolean canClose = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                canClose = true;
            }
            As_BonDeLivraison[] bls = As_BonDeLivraison.getAll(ids, c);
            As_BonDeLivraison.controlerFournisseur(bls);

            this.setDesignation("Facturation de Bon de Livraison");
            this.setIdFournisseur(bls[0].getIdFournisseur());
            this.setIdMagasin(bls[0].getMagasin());
            this.setDaty(Utilitaire.dateDuJourSql());
            this.setIdDevise("AR");
            this.createObject(u, c);
            for (As_BonDeLivraison bl : bls) {
                bl.setIdFactureFournisseur(this.getId());
                bl.setEtat(1);
                bl.updateToTableWithHisto(u, c);
            }
            As_BonDeLivraison_Fille blf = new As_BonDeLivraison_Fille();
            String[] somGr = { "quantite" };
            String[] gr = { "produit" };
            String[] tabvide = {};
            ResultatEtSomme rs = CGenUtil.rechercherGroupe(blf, gr, somGr, null, null,
                    " and numbl in " + Utilitaire.tabToString(ids, "'", ","), tabvide, "", c);
            As_BonDeLivraison_Fille[] blfs = (As_BonDeLivraison_Fille[]) rs.getResultat();
            for (As_BonDeLivraison_Fille item : blfs) {
                FactureFournisseurDetails vd = item.toFactureFournisseurDetails();
                vd.setIdFactureFournisseur(this.getId());
                vd.setIdDevise("AR");
                vd.createObject(u, c);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (canClose) {
                c.close();
            }
        }
    }

    public Prevision[] getPrevisions(Connection c) throws Exception {
        Boolean estOuvert = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                estOuvert = true;
            }
            Prevision prevision = new Prevision();
            prevision.setIdFacture(this.getId());
            Prevision[] prev = (Prevision[]) CGenUtil.rechercher(prevision, null, null, c, " ");
            return prev;
        } catch (Exception e) {
            throw e;
        } finally {
            if (estOuvert)
                c.close();
        }
    }

    public void lierLivraisons(String u, String[] idLivraison) throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            FactureFournisseurDetails[] factDetails = getFactureFournisseurDetails(c);
            As_BonDeLivraison[] blcs = As_BonDeLivraison.getAll(idLivraison, c);
            As_BonDeLivraison.controlerFournisseur(blcs);
            for (As_BonDeLivraison blcTemp : blcs) {
                blcTemp.setIdFactureFournisseur(this.getId());
                blcTemp.updateToTableWithHisto(u, c);
                As_BonDeLivraison_Fille[] blcfs = (As_BonDeLivraison_Fille[]) CGenUtil.rechercher(
                        new As_BonDeLivraison_Fille(), null, null, c, " and NUMBL = '" + blcTemp.getId() + "'");
                for (int i = 0; i < factDetails.length; i++) {
                    for (As_BonDeLivraison_Fille as_BonDeLivraisonFilleTemp : blcfs) {
                        if (factDetails[i].getIdProduit().equals(as_BonDeLivraisonFilleTemp.getProduit())) {
                            as_BonDeLivraisonFilleTemp.setIdFactureFournisseur(factDetails[i].getId());
                            as_BonDeLivraisonFilleTemp.updateToTableWithHisto(u, c);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw e;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public As_BonDeLivraison createBLN() throws Exception {
        FactureFournisseur ff = (FactureFournisseur) this.getById(this.getId(), this.getNomTable(), null);
        As_BonDeLivraison bl = ff.createBL();
        // bl =(As_BonDeLivraison) bl.createObject(u,c);
        // generateBLFille(bl,c,u);

        return bl;
    }

    public As_BonDeLivraison_Fille_Cpl[] generateBLFilles(As_BonDeLivraison bl, Connection c) throws Exception {
        boolean estOuvert = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                estOuvert = true;
            }

            FactureFournisseurDetailResteALivrerLib[] details = getDetailsResteALivrer(c, "FFFILLERESTEALIVRER");
            As_BonDeLivraison_Fille_Cpl[] values = new As_BonDeLivraison_Fille_Cpl[details.length];
            int indice = 0;
            for (FactureFournisseurDetailResteALivrerLib d : details) {
                values[indice] = new As_BonDeLivraison_Fille_Cpl();
                values[indice] = d.createBLFille(bl.getId());
                // blf.createObject(u,c);
                indice++;
            }
            return values;
        } catch (Exception e) {
            throw e;
        } finally {
            if (estOuvert)
                c.close();
        }
    }

}
