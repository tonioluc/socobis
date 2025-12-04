/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vente;

import avoir.AvoirFC;
import avoir.AvoirFCFille;
import avoir.AvoirFCLib;
import bean.AdminGen;
import bean.CGenUtil;
import bean.ClassMere;
import bean.ResultatEtSomme;
import bean.ClassMAPTable;
import java.sql.Connection;
import java.sql.Date;
import caisse.Caisse;
import caisse.MvtCaisse;
import client.Client;
import constante.ConstanteEtat;
import encaissement.Encaissement;
import encaissement.EncaissementDetails;
import faturefournisseur.FactureFournisseurCpl;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import magasin.Magasin;
import mg.cnaps.compta.ComptaEcriture;
import mg.cnaps.compta.ComptaSousEcriture;
import paiement.LiaisonPaiement;
import prevision.Prevision;
import prevision.PrevisionComplet;
import produits.Ingredients;
import stock.MvtStock;
import stock.MvtStockFille;
import utilitaire.UtilDB;
import utilitaire.Utilitaire;
import utils.ConstanteEtatCustom;
import utils.ConstanteStation;

/**
 *
 * @author Angela
 */
public class Vente extends FactureCF {
    protected String designation;
    protected String idMagasin;
    protected String remarque;
    protected String idOrigine;
    protected String idClient, clientlib;
    protected String compte;
    protected double tauxdechange;
    protected VenteDetails[] venteDetails;
    int estPrevu;
    double montantRevient;
    double margeBrute;
    double echeancefacture;
    String modepaiement;
    double fraislivraison;
    int modelivraison;
    String referencefact;
    String numerofacture;
    String planpaiement;

    public String getPlanpaiement() {
        return planpaiement;
    }

    public void setPlanpaiement(String planpaiement) {
        this.planpaiement = planpaiement;
    }

    public String getNumerofacture() {
        return numerofacture;
    }

    public void setNumerofacture(String numerofacture) {
        this.numerofacture = numerofacture;
    }

    public int getModelivraison() {
        return modelivraison;
    }

    public void setModelivraison(int modelivraison) {
        this.modelivraison = modelivraison;
    }

    public double getFraislivraison() {
        return fraislivraison;
    }

    public void setFraislivraison(double fraislivraison) {
        this.fraislivraison = fraislivraison;
    }

    public String getModepaiement() {
        return modepaiement;
    }

    public void setModepaiement(String modepaiement) {
        this.modepaiement = modepaiement;
    }

    public void setMargeBrute(double margeBrute) {
        this.margeBrute = margeBrute;
    }

    public double getMontantRevient() {
        return montantRevient;
    }

    public void setMontantRevient(double montantRevient) {
        this.montantRevient = montantRevient;
    }

    public String getReferencefact() {
        return referencefact;
    }

    public void setReferencefact(String referencefact) {
        this.referencefact = referencefact;
    }

    public double getMargeBrute() {
        return this.getMontantttc() - this.getMontantRevient();
    }

    @Override
    public boolean isSynchro() {
        return true;
    }

    public void Vente() {
    }

    @Override
    public boolean getEstIndexable() {
        return true;
    }

    @Override
    public String getTiers() {
        return this.getIdClient();
    }

    @Override
    public String getSensPrev() {
        return "credit";
    }

    public Prevision genererPrevision(String u, Connection c) throws Exception {
        Prevision mere = new Prevision();
        Vente venteComplet = this.getVenteWithMontant(c);
        mere.setDaty(datyPrevu);
        mere.setCredit(venteComplet.getMontantttcAr());
        mere.setIdFacture(this.id);
        mere.setIdCaisse(ConstanteStation.idCaisse);
        mere.setIdDevise("AR");
        mere.setDesignation("Prevision rattachée au vente N : " + this.getId());
        mere.setIdTiers(this.getIdClient());
        return (Prevision) mere.createObject(u, c);
    }

    public Prevision[] genererPrevisions(String u, Connection c) throws Exception {
        Vente venteComplet = this.getVenteWithMontant(c);
        double montantTotal = venteComplet.getMontantttcAr();

        // 1. Vérifier si plan de paiement existe
        if (this.planpaiement == null || this.planpaiement.trim().isEmpty()) {
            // Mode ancien : une seule prévision
            return genererPrevisionUnique(montantTotal, u, c);
        }

        // 2. Parser le plan de paiement
        String[] elements = this.planpaiement.split(";");
        List<Prevision> previsions = new ArrayList<>();

        // Détecter le format
        boolean formatAvecPourcentages = false;
        boolean formatDatesSeules = false;
        double totalPourcentage = 0;

        for (String element : elements) {
            element = element.trim();
            if (element.isEmpty())
                continue;

            // Vérifier si contient ":" (format date:pourcentage)
            if (element.contains(":")) {
                formatAvecPourcentages = true;
                String[] parts = element.split(":");
                if (parts.length == 2) {
                    try {
                        totalPourcentage += Double.parseDouble(parts[1].trim());
                    } catch (NumberFormatException e) {
                        throw new Exception("Pourcentage invalide: " + parts[1]);
                    }
                }
            } else {
                formatDatesSeules = true;
            }
        }

        // 3. Valider qu'on a un seul format
        if (formatAvecPourcentages && formatDatesSeules) {
            throw new Exception("Format mixte non supporté. Utilisez soit 'date:pourcentage' soit 'date' uniquement");
        }

        // 4. Générer les prévisions selon le format
        if (formatAvecPourcentages) {
            // Format: date:pourcentage (07/12/2025:30)
            previsions = genererPrevisionsAvecPourcentages(elements, montantTotal, u, c);

            // Vérifier que le total fait 100%
            if (Math.abs(totalPourcentage - 100.0) > 0.01) {
                throw new Exception(
                        "Le total des pourcentages doit être égal à 100%. Total: " + totalPourcentage + "%");
            }
        } else {
            // Format: date uniquement (07/12/2025;10/12/2025;15/12/2025)
            previsions = genererPrevisionsDatesSeules(elements, montantTotal, u, c);
        }

        return previsions.toArray(new Prevision[0]);
    }

    // Méthode pour le format date uniquement
    private List<Prevision> genererPrevisionsDatesSeules(String[] dates, double montantTotal, String u, Connection c)
            throws Exception {
        List<Prevision> previsions = new ArrayList<>();
        int nombreEcheances = dates.length;

        // Calculer le montant par échéance (division égale)
        double montantParEcheance = montantTotal / nombreEcheances;

        for (int i = 0; i < nombreEcheances; i++) {
            String dateStr = dates[i].trim();
            if (dateStr.isEmpty())
                continue;

            // Convertir la date
            java.sql.Date dateEcheance = convertirDate(dateStr);

            // Créer la prévision
            Prevision prevision = new Prevision();
            prevision.setDaty(dateEcheance);
            prevision.setCredit(montantParEcheance);
            prevision.setIdFacture(this.id);
            prevision.setIdCaisse(ConstanteStation.idCaisse);
            prevision.setIdDevise("AR");
            prevision.setDesignation(String.format("Prévision vente %s - Échéance %d/%d (%.2f AR)",
                    this.getId(), i + 1, nombreEcheances, montantParEcheance));
            prevision.setIdTiers(this.getIdClient());

            // Enregistrer
            previsions.add((Prevision) prevision.createObject(u, c));
        }

        return previsions;
    }

    // Méthode pour le format date:pourcentage
    private List<Prevision> genererPrevisionsAvecPourcentages(String[] echeances, double montantTotal, String u,
            Connection c) throws Exception {
        List<Prevision> previsions = new ArrayList<>();
        int nombreEcheances = echeances.length;

        for (int i = 0; i < nombreEcheances; i++) {
            String echeance = echeances[i].trim();
            if (echeance.isEmpty())
                continue;

            // Séparer date et pourcentage
            String[] parts = echeance.split(":");
            if (parts.length != 2) {
                throw new Exception("Format invalide: " + echeance + ". Format attendu: DATE:PORCENTAGE");
            }

            String dateStr = parts[0].trim();
            String pourcentageStr = parts[1].trim();

            // Convertir la date
            java.sql.Date dateEcheance = convertirDate(dateStr);

            // Calculer le montant
            double pourcentage = Double.parseDouble(pourcentageStr);
            double montant = (montantTotal * pourcentage) / 100.0;

            // Créer la prévision
            Prevision prevision = new Prevision();
            prevision.setDaty(dateEcheance);
            prevision.setCredit(montant);
            prevision.setIdFacture(this.id);
            prevision.setIdCaisse(ConstanteStation.idCaisse);
            prevision.setIdDevise("AR");
            prevision.setDesignation(String.format("Prévision vente %s - Échéance %d/%d (%.0f%%)",
                    this.getId(), i + 1, nombreEcheances, pourcentage));
            prevision.setIdTiers(this.getIdClient());

            // Enregistrer
            previsions.add((Prevision) prevision.createObject(u, c));
        }

        return previsions;
    }

    // Méthode pour une seule prévision (ancien mode)
    private Prevision[] genererPrevisionUnique(double montantTotal, String u, Connection c) throws Exception {
        Prevision mere = new Prevision();
        mere.setDaty(datyPrevu);
        mere.setCredit(montantTotal);
        mere.setIdFacture(this.id);
        mere.setIdCaisse(ConstanteStation.idCaisse);
        mere.setIdDevise("AR");
        mere.setDesignation("Prévision rattachée à la vente N : " + this.getId());
        mere.setIdTiers(this.getIdClient());

        Prevision result = (Prevision) mere.createObject(u, c);
        return new Prevision[] { result };
    }

    // Méthode utilitaire pour convertir la date
    private java.sql.Date convertirDate(String dateStr) throws Exception {
        try {
            // Nettoyer la date
            dateStr = dateStr.trim().replaceAll("\\s+", "");

            // Format: "07/12/2025" (dd/MM/yyyy)
            String[] parts = dateStr.split("/");
            if (parts.length != 3) {
                throw new Exception("Format de date invalide: " + dateStr);
            }

            int jour = Integer.parseInt(parts[0]);
            int mois = Integer.parseInt(parts[1]) - 1; // Calendar mois commence à 0
            int annee = Integer.parseInt(parts[2]);

            // Validation basique
            if (jour < 1 || jour > 31)
                throw new Exception("Jour invalide: " + jour);
            if (mois < 0 || mois > 11)
                throw new Exception("Mois invalide: " + (mois + 1));
            if (annee < 1900 || annee > 2100)
                throw new Exception("Année invalide: " + annee);

            // Créer la date
            Calendar cal = Calendar.getInstance();
            cal.set(annee, mois, jour, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            return new java.sql.Date(cal.getTimeInMillis());

        } catch (NumberFormatException e) {
            throw new Exception("Date invalide: '" + dateStr + "'. Format attendu: JJ/MM/AAAA");
        } catch (Exception e) {
            throw new Exception("Erreur conversion date '" + dateStr + "': " + e.getMessage());
        }
    }

    public Vente getVenteWithMontant(Connection c) throws Exception {
        return (Vente) new Vente().getById(this.getId(), "VENTE_CPL", c);
    }

    public String genererBonLivraison(String u) throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            Vente enBase = (Vente) this.getById(this.getId(), this.getNomTable(), c);
            VenteDetailsLib vLib = new VenteDetailsLib();
            vLib.setNomTable("VENTE_DETAILS_RESTE");
            VenteDetailsLib[] details = (VenteDetailsLib[]) CGenUtil.rechercher(vLib, null, null, c,
                    " AND idVente='" + this.getId() + "' AND reste > 0");
            if (details.length > 0) {
                As_BondeLivraisonClient client = new As_BondeLivraisonClient();
                client.setMode("modif");
                client.setIdvente(this.getId());
                client.setEtat(1);
                client.setIdclient(enBase.getIdClient());
                client.setRemarque("Livraison de la facture numero " + this.getId());
                client.setDaty(Utilitaire.dateDuJourSql());
                client.createObject(u, c);
                for (VenteDetailsLib detail : details) {
                    As_BondeLivraisonClientFille clientFille = new As_BondeLivraisonClientFille();
                    clientFille.setMode("modif");
                    clientFille.setProduit(detail.getIdProduit());
                    clientFille.setUnite(detail.getIdUnite());
                    clientFille.setQuantite(detail.getReste());
                    clientFille.setIdventedetail(detail.getId());
                    clientFille.setNumbl(client.getId());
                    clientFille.createObject(u, c);
                }
                c.commit();
                return client.getId();
            }
            throw new Exception("Plus aucun article à livrer");
        } catch (Exception e) {
            if (c != null)
                c.rollback();
            throw e;
        } finally {
            if (c != null)
                c.close();
        }
    }

    public As_BondeLivraisonClient genererBonLivraison() throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            Vente enBase = (Vente) this.getById(this.getId(), this.getNomTable(), c);
            VenteDetailsLib vLib = new VenteDetailsLib();
            vLib.setNomTable("VENTE_DETAILS_RESTE");
            VenteDetailsLib[] details = (VenteDetailsLib[]) CGenUtil.rechercher(vLib, null, null, c,
                    " AND idVente='" + this.getId() + "' AND reste > 0");
            if (details.length > 0) {
                As_BondeLivraisonClient client = new As_BondeLivraisonClient();
                client.setMode("modif");
                client.setIdvente(this.getId());
                client.setEtat(1);
                client.setIdclient(enBase.getIdClient());
                client.setRemarque("Livraison de la facture numero " + this.getId());
                client.setDaty(Utilitaire.dateDuJourSql());
                client.setIdorigine(this.getId());
                As_BondeLivraisonClientFille[] filles = new As_BondeLivraisonClientFille[details.length];
                int i = 0;
                for (VenteDetailsLib detail : details) {
                    As_BondeLivraisonClientFille_Cpl clientFille = new As_BondeLivraisonClientFille_Cpl();
                    clientFille.setMode("modif");
                    clientFille.setProduit(detail.getIdProduit());
                    Ingredients ing = (Ingredients) new Ingredients().getById(detail.getIdProduit(), "AS_INGREDIENTS",
                            c);
                    clientFille.setProduitlib(ing.getLibelle());
                    clientFille.setUnite(detail.getIdUnite());
                    clientFille.setUnitelib(detail.getUnitelib());
                    clientFille.setQuantite(detail.getReste());
                    clientFille.setIdventedetail(detail.getId());
                    clientFille.setNumbl(client.getId());
                    filles[i] = clientFille;
                    i++;
                }
                client.setFille(filles);
                return client;
            }
            throw new Exception("Plus aucun article à livrer");
        } catch (Exception e) {
            throw e;
        } finally {
            if (c != null)
                c.close();
        }
    }

    public String getCompte() {
        return compte;
    }

    public void setCompte(String compte) {
        this.compte = compte;
    }

    public String getIdClient() {
        return idClient;
    }

    public void setIdClient(String idClient) throws Exception {
        if (this.getMode().compareTo("modif") == 0) {
            if (idClient == null || idClient.compareToIgnoreCase("") == 0)
                throw new Exception("Client obligatoire");
        }
        this.idClient = idClient;
    }

    public Vente() {
        this.setNomTable("VENTE");
    }

    public Vente(String nomtable) {
        setNomTable(nomtable);
    }

    public int getEstPrevu() {
        return estPrevu;
    }

    public void setEstPrevu(int estPrevu) {
        this.estPrevu = estPrevu;
    }

    public String getIdMagasin() {
        return idMagasin;
    }

    public void setIdMagasin(String idMagasin) {
        this.idMagasin = idMagasin;
    }

    public boolean isPaye() {
        if (this.getEtat() == ConstanteEtatCustom.PAYE_LIVRE || this.getEtat() == ConstanteEtatCustom.PAYE_NON_LIVRE) {
            return true;
        }
        return false;
    }

    public boolean isLivre() {
        if (this.getEtat() == ConstanteEtatCustom.LIVRE_NON_PAYE || this.getEtat() == ConstanteEtatCustom.PAYE_LIVRE) {
            return true;
        }
        return false;
    }

    public String getClientlib() {
        return clientlib;
    }

    public void setClientlib(String clientlib) {
        this.clientlib = clientlib;
    }

    public double getTauxdechange() {
        return tauxdechange;
    }

    public void setTauxdechange(double tauxdechange) {
        this.tauxdechange = tauxdechange;
    }

    public void payer(String u, Connection c) throws Exception {
        if (this.getEtat() < ConstanteEtat.getEtatValider()) {
            throw new Exception("Impossible d encaisser une vente non validée");
        }
        if (isLivre()) {
            this.updateEtat(ConstanteEtatCustom.PAYE_LIVRE, this.getId(), c);
        } else {
            this.updateEtat(ConstanteEtatCustom.PAYE_NON_LIVRE, this.getId(), c);
        }
        genererEcritureEncaissement(u, c);
    }

    // Variante pour flux "Changer et payer" avec montant TTC explicite
    public void payerAvecTTC(String u, Connection c, double montantTtcForce, double totalReversed) throws Exception {
        if (this.getEtat() < ConstanteEtat.getEtatValider()) {
            throw new Exception("Impossible d encaisser une vente non validée");
        }
        if (isLivre()) {
            this.updateEtat(ConstanteEtatCustom.PAYE_LIVRE, this.getId(), c);
        } else {
            this.updateEtat(ConstanteEtatCustom.PAYE_NON_LIVRE, this.getId(), c);
        }
        if (totalReversed > 0) {
            MvtCaisse mvt = new MvtCaisse();
            mvt.setDebit(totalReversed);
            mvt.setIdCaisse(getCaisse(c).getId());
            mvt.setDaty(utilitaire.Utilitaire.dateDuJourSql());
            mvt.setDesignation("Annulation encaissement pour " + this.getDesignation());
            mvt.setIdOrigine(this.getId());
            mvt.createObject(u, c);
            mvt.validerObject(u, c);
        }
        if (totalReversed > 0) {
            genererEcritureAnnulationEncaissement(u, c, totalReversed);
        }
        genererEcritureEncaissementAvecTTC(u, c, montantTtcForce);
    }

    public void livrer(String u, Connection c) throws Exception {
        if (this.getEtat() < ConstanteEtat.getEtatValider()) {
            throw new Exception("Impossible de livrer une vente non validée");
        }
        if (isPaye()) {
            this.updateEtat(ConstanteEtatCustom.PAYE_LIVRE, this.getId(), c);
        } else {
            this.updateEtat(ConstanteEtatCustom.LIVRE_NON_PAYE, this.getId(), c);
        }
    }

    public void lierLivraisons(String u, String[] idLivraison) throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            VenteDetails[] venteDetails = getVenteDetails(c);
            As_BondeLivraisonClient[] blcs = As_BondeLivraisonClient.getAll(idLivraison, c);
            As_BondeLivraisonClient.controlerClient(blcs);
            for (As_BondeLivraisonClient blcTemp : blcs) {
                blcTemp.setIdvente(this.getId());
                blcTemp.updateToTableWithHisto(u, c);
                As_BondeLivraisonClientFille[] blcfs = (As_BondeLivraisonClientFille[]) CGenUtil.rechercher(
                        new As_BondeLivraisonClientFille(), null, null, c, " and NUMBL = '" + blcTemp.getId() + "'");
                for (int i = 0; i < venteDetails.length; i++) {
                    for (As_BondeLivraisonClientFille as_BondeLivraisonClientFilleTemp : blcfs) {
                        if (venteDetails[i].getIdProduit().equals(as_BondeLivraisonClientFilleTemp.getProduit())) {
                            as_BondeLivraisonClientFilleTemp.setIdventedetail(venteDetails[i].getId());
                            as_BondeLivraisonClientFilleTemp.updateToTableWithHisto(u, c);
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

    @Override
    public void changeState(String acte, String u, Connection con) throws Exception {
        if (acte.equals("livrer")) {
            this.livrer(u, con);
        } else if (acte.equals("payer")) {
            this.payer(u, con);
        }
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }

    @Override
    public String getTuppleID() {
        return this.getId();
    }

    @Override
    public String getAttributIDName() {
        return "id";
    }

    public String getIdOrigine() {
        return idOrigine;
    }

    public void setIdOrigine(String idOrigine) {
        this.idOrigine = idOrigine;
    }

    @Override
    public void construirePK(Connection c) throws Exception {
        this.preparePk("VNT", "getSeqVente");
        this.setId(makePK(c));
    }

    public Caisse getCaisse(Connection c) throws Exception {
        Caisse caisse = new Caisse();
        // Dans ce contexte, on prend n'importe quelle caisse disponible, sans filtrer
        // par magasin
        Caisse[] caisses = (Caisse[]) CGenUtil.rechercher(caisse, null, null, c, " ");
        if (caisses.length > 0) {
            return caisses[0];
        }
        return null;
    }

    public Magasin getMagasin(Connection c) throws Exception {
        Magasin magasin = new Magasin();
        magasin.setId(this.getIdMagasin());
        Magasin[] magasins = (Magasin[]) CGenUtil.rechercher(magasin, null, null, c, " ");
        if (magasins.length > 0) {
            return magasins[0];
        }
        return null;
    }

    public VenteDetails[] getVenteDetails() {
        return venteDetails;
    }

    public void setVenteDetails(VenteDetails[] venteDetails) {
        this.venteDetails = venteDetails;
    }

    @Override
    public void controler(Connection c) throws Exception {
        super.controler(c);
    }

    @Override
    public void controlerUpdate(Connection c) throws Exception {
        super.controlerUpdate(c);

    }

    public void createMvtCaisses(String u, Connection c) throws Exception {
        VenteDetailsCpl vdc = new VenteDetailsCpl();
        vdc.setIdVente(this.getId());
        VenteDetailsCpl[] vdcs = (VenteDetailsCpl[]) CGenUtil.rechercher(vdc, null, null, c, " ");
        for (int i = 0; i < vdcs.length; i++) {
            MvtCaisse mc = vdcs[i].createMvtCaisse();
            mc.createObject(u, c);
        }
    }

    public VenteDetailsLib[] getVenteDetails(Connection c) throws Exception {
        VenteDetailsLib obj = new VenteDetailsLib();
        obj.setNomTable("VENTE_DETAILS_CPL");
        obj.setIdVente(this.getId());
        VenteDetailsLib[] objs = (VenteDetailsLib[]) CGenUtil.rechercher(obj, null, null, c, " ");
        if (objs.length > 0) {
            return objs;
        }
        return null;
    }

    protected EncaissementDetails[] generateDetailsEncaissements(Connection c) throws Exception {
        VenteDetailsLib[] vd = this.getVenteDetails(c);
        EncaissementDetails[] ed = new EncaissementDetails[vd.length];
        for (int i = 0; i < ed.length; i++) {
            ed[i] = vd[i].generateEncaissementDetails();
        }
        return ed;
    }

    @Override
    public Object payerObject(String u, Connection con) throws Exception {
        super.payerObject(u, con);
        Encaissement enc = this.genererEncaissement();
        enc = (Encaissement) enc.createObject(u, con);
        EncaissementDetails[] ed = generateDetailsEncaissements(con);
        for (int i = 0; i < ed.length; i++) {
            ed[i].setIdEncaissement(enc.getId());
            ed[i].createObject(u, con);
        }
        return enc;
    }

    protected MvtStockFille[] createMvtStockFilles(Connection c) throws Exception {
        VenteDetails[] tsd = this.getVenteDetails(c);
        MvtStockFille[] mvtf = new MvtStockFille[tsd.length];
        for (int i = 0; i < tsd.length; i++) {
            mvtf[i] = tsd[i].createMvtStockFille();
        }
        return mvtf;
    }

    protected MvtStockFille[] createMvtStockFillesReversed(Connection c) throws Exception {
        VenteDetails[] tsd = this.getVenteDetailsNonGrp(c);
        if (tsd == null || tsd.length == 0) {
            return new MvtStockFille[0];
        }

        java.util.Map<String, Double> quantitesParProduit = new java.util.HashMap<String, Double>();

        for (int i = 0; i < tsd.length; i++) {
            String idProduit = tsd[i].getIdProduit();
            if (idProduit == null) {
                continue;
            }
            idProduit = idProduit.trim();
            if (idProduit.length() == 0) {
                continue;
            }

            double qte = tsd[i].getQte();
            Double existant = quantitesParProduit.get(idProduit);
            if (existant == null) {
                existant = 0d;
            }
            quantitesParProduit.put(idProduit, existant + qte);
        }

        if (quantitesParProduit.isEmpty()) {
            return new MvtStockFille[0];
        }

        MvtStockFille[] mvtf = new MvtStockFille[quantitesParProduit.size()];
        int index = 0;
        for (java.util.Map.Entry<String, Double> entry : quantitesParProduit.entrySet()) {
            MvtStockFille msf = new MvtStockFille();
            msf.setIdProduit(entry.getKey());
            msf.setEntree(entry.getValue());
            mvtf[index] = msf;
            index++;
        }
        System.out.println("DEBUG: taille du mvtf " + mvtf.length);
        return mvtf;
    }

    protected MvtStock createMvtStock() throws Exception {
        MvtStock md = new MvtStock();
        md.setDaty(this.getDaty());
        md.setDesignation("Vente lubrifiant : " + this.getDesignation());
        md.setIdTransfert(this.getId());
        md.setIdTypeMvStock(ConstanteStation.TYPEMVTSTOCKSORTIE);
        md.setIdMagasin(this.getIdMagasin());
        return md;
    }

    protected MvtStock createMvtStockAnnulation() throws Exception {
        MvtStock md = new MvtStock();
        md.setDaty(utilitaire.Utilitaire.dateDuJourSql());
        md.setDesignation("Annulation vente : " + this.getDesignation());
        md.setIdTransfert(this.getId());
        md.setIdTypeMvStock(ConstanteStation.TYPEMVTSTOCKENTREE);
        md.setIdMagasin(this.getIdMagasin());
        return md;
    }

    protected MvtStock createMvtStockSortie(String u, Connection c) throws Exception {
        MvtStock ms = this.createMvtStock();
        ms.setFille(this.createMvtStockFilles(c));
        ms.createObject(u, c);
        ms.saveMvtStockFille(u, c);
        ms.validerObject(u, c);
        return ms;
    }

    @Override
    public ClassMAPTable createObject(String u, Connection c) throws Exception {
        NumeroFacture[] numFact = (NumeroFacture[]) CGenUtil.rechercher(new NumeroFacture(), null, null, c, "");
        this.setNumerofacture(numFact[0].getProchain_num_format());

        VenteDetails[] listeFille = (VenteDetails[]) this.getFille();
        System.out.println("Taille liste fille ===>" + listeFille.length);
        Set<String> seen = new HashSet<>();
        System.out.println("Avant boucle");

        for (VenteDetails vd : listeFille) {
            String key = vd.getIdProduit();
            String designation = vd.getDesignation();

            if (key == null || key.trim().isEmpty())
                continue;

            if (!seen.add(key)) {
                throw new Exception("Doublon pour l'article : " + designation);
            }
        }
        System.out.println("Avant createObject mere vente");

        return super.createObject(u, c);
    }

    @Override
    public Object validerObject(String u, Connection c) throws Exception {
        boolean estOuvert = false;
        try {
            if (c == null) {
                estOuvert = true;
                c = new UtilDB().GetConn();
                c.setAutoCommit(false);
            }
            // CheckEtatStockVenteDetails(c);
            super.validerObject(u, c);
            VenteDetails[] listeFille = (VenteDetails[]) this.getFille(null, c, "");
            for (int i = 0; i < listeFille.length; i++) {
                listeFille[i].calculerRevient(c);
                listeFille[i].updateToTableWithHisto(u, c);
            }
            genererEcriture(u, c);
            // createMvtStockSortie(u, c);
            if (this.getEstPrevu() == 0 || this.getDatyPrevu() != null) {
                genererPrevisions(u, c);
            }

            return this;

        } catch (Exception e) {
            if (c != null) {
                c.rollback();
            }
            e.printStackTrace();
            throw e;
        } finally {
            if (c != null && estOuvert == true) {
                c.close();
            }
        }
    }

    public void CheckEtatStockVenteDetails(Connection c) throws Exception {
        VenteDetailsLib[] vds = getVenteDetails(c);
        if (vds != null) {
            for (VenteDetailsLib v : vds) {
                v.CheckEtatStock(c);
            }
        }

    }

    public void genererEcritureEncaissement(String u, Connection c) throws Exception {
        ComptaEcriture mere = new ComptaEcriture();
        Date dateDuJour = utilitaire.Utilitaire.dateDuJourSql();
        int exercice = utilitaire.Utilitaire.getAnnee(daty);
        mere.setDaty(dateDuJour);
        mere.setDesignation(this.getDesignation());
        mere.setExercice("" + exercice);
        mere.setDateComptable(this.getDaty());
        mere.setJournal(ConstanteStation.JOURNALVENTE);
        mere.setOrigine(this.getId());
        mere.setIdobjet(this.getId());
        mere.createObject(u, c);
        ComptaSousEcriture[] filles = this.genererSousEcritureEncaissement(u, c);
        for (int i = 0; i < filles.length; i++) {
            filles[i].setIdMere(mere.getId());
            filles[i].setExercice(exercice);
            filles[i].setDaty(this.getDaty());
            filles[i].setJournal(ConstanteStation.JOURNALVENTE);

            if (filles[i].getDebit() > 0 || filles[i].getCredit() > 0)
                filles[i].createObject(u, c);
        }
    }

    public ComptaSousEcriture[] genererSousEcritureEncaissement(String refUser, Connection c) throws Exception {
        ComptaSousEcriture[] compta = {};
        boolean canClose = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                canClose = true;
            }
            Vente[] ventes = (Vente[]) CGenUtil.rechercher(new Vente("VENTE_MERE_MONTANT"), null, null, c,
                    " and id = '" + this.getId() + "'");
            if (ventes.length < 1)
                throw new Exception("Facture mere Introuvable");
            this.setCompte(getClient(c).getCompte());

            compta = new ComptaSousEcriture[2];

            compta[0] = new ComptaSousEcriture();
            compta[0].setLibellePiece(this.getDesignation());
            compta[0].setRemarque(this.getDesignation());
            compta[0].setCompte(getCaisse(c).getCompte());
            compta[0].setDebit(ventes[0].getMontantttc());
            MvtCaisse mvt = new MvtCaisse();
            mvt.setCredit(ventes[0].getMontantttc());
            mvt.setIdCaisse(getCaisse(c).getId());
            mvt.setDaty(utilitaire.Utilitaire.dateDuJourSql());
            mvt.setDesignation("mvt pour" + this.getDesignation());
            mvt.setIdOrigine(this.getId());
            mvt.createObject(refUser, c);
            mvt.validerObject(refUser, c);

            compta[1] = new ComptaSousEcriture();
            compta[1].setLibellePiece("Encaissement Client " + ventes[0].getClientlib());
            compta[1].setRemarque("Encaissement Client " + ventes[0].getClientlib());
            compta[1].setCompte(this.getCompte());
            // compta[i].setDebit((montantHT-retenue) * ((this.getTva()/100)));
            compta[1].setCredit(ventes[0].getMontantttc());

        } catch (Exception e) {
            throw e;
        } finally {
            if (canClose) {
                c.close();
            }
        }
        return compta;
    }

    // Copie pour flux "Changer et payer" avec montant TTC imposé
    public void genererEcritureEncaissementAvecTTC(String u, Connection c, double montantTtcForce) throws Exception {
        ComptaEcriture mere = new ComptaEcriture();
        Date dateDuJour = utilitaire.Utilitaire.dateDuJourSql();
        int exercice = utilitaire.Utilitaire.getAnnee(daty);
        mere.setDaty(dateDuJour);
        mere.setDesignation(this.getDesignation());
        mere.setExercice("" + exercice);
        mere.setDateComptable(this.getDaty());
        mere.setJournal(ConstanteStation.JOURNALVENTE);
        mere.setOrigine(this.getId());
        mere.setIdobjet(this.getId());
        mere.createObject(u, c);
        ComptaSousEcriture[] filles = this.genererSousEcritureEncaissementAvecTTC(u, c, montantTtcForce);
        for (int i = 0; i < filles.length; i++) {
            filles[i].setIdMere(mere.getId());
            filles[i].setExercice(exercice);
            filles[i].setDaty(this.getDaty());
            filles[i].setJournal(ConstanteStation.JOURNALVENTE);

            if (filles[i].getDebit() > 0 || filles[i].getCredit() > 0)
                filles[i].createObject(u, c);
        }
    }

    // Copie pour flux "Changer et payer" avec montant TTC imposé
    public ComptaSousEcriture[] genererSousEcritureEncaissementAvecTTC(String refUser, Connection c,
            double montantTtcForce) throws Exception {
        ComptaSousEcriture[] compta = {};
        boolean canClose = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                canClose = true;
            }
            Vente[] ventes = (Vente[]) CGenUtil.rechercher(new Vente("VENTE_MERE_MONTANT"), null, null, c,
                    " and id = '" + this.getId() + "'");
            if (ventes.length < 1)
                throw new Exception("Facture mere Introuvable");
            this.setCompte(getClient(c).getCompte());

            double montant = montantTtcForce;

            compta = new ComptaSousEcriture[2];

            compta[0] = new ComptaSousEcriture();
            compta[0].setLibellePiece(this.getDesignation());
            compta[0].setRemarque(this.getDesignation());
            compta[0].setCompte(getCaisse(c).getCompte());
            compta[0].setDebit(montant);

            MvtCaisse mvt = new MvtCaisse();
            mvt.setCredit(montant);
            mvt.setIdCaisse(getCaisse(c).getId());
            mvt.setDaty(utilitaire.Utilitaire.dateDuJourSql());
            mvt.setDesignation("mvt pour" + this.getDesignation());
            mvt.setIdOrigine(this.getId());
            mvt.createObject(refUser, c);
            mvt.validerObject(refUser, c);

            compta[1] = new ComptaSousEcriture();
            compta[1].setLibellePiece("Encaissement Client " + ventes[0].getClientlib());
            compta[1].setRemarque("Encaissement Client " + ventes[0].getClientlib());
            compta[1].setCompte(this.getCompte());
            compta[1].setCredit(montant);

        } catch (Exception e) {
            throw e;
        } finally {
            if (canClose) {
                c.close();
            }
        }
        return compta;
    }

    // Pour l'annulation des encaissements dans "Changer et payer"
    public void genererEcritureAnnulationEncaissement(String u, Connection c, double montantReversed) throws Exception {
        ComptaEcriture mere = new ComptaEcriture();
        Date dateDuJour = utilitaire.Utilitaire.dateDuJourSql();
        int exercice = utilitaire.Utilitaire.getAnnee(daty);
        mere.setDaty(dateDuJour);
        mere.setDesignation("Annulation encaissement " + this.getDesignation());
        mere.setExercice("" + exercice);
        mere.setDateComptable(this.getDaty());
        mere.setJournal(ConstanteStation.JOURNALVENTE);
        mere.setOrigine(this.getId());
        mere.setIdobjet(this.getId());
        mere.createObject(u, c);
        ComptaSousEcriture[] filles = genererSousEcritureAnnulationEncaissement(u, c, montantReversed);
        for (int i = 0; i < filles.length; i++) {
            filles[i].setIdMere(mere.getId());
            filles[i].setExercice(exercice);
            filles[i].setDaty(this.getDaty());
            filles[i].setJournal(ConstanteStation.JOURNALVENTE);

            if (filles[i].getDebit() > 0 || filles[i].getCredit() > 0)
                filles[i].createObject(u, c);
        }
    }

    // Pour l'annulation des encaissements dans "Changer et payer"
    public ComptaSousEcriture[] genererSousEcritureAnnulationEncaissement(String refUser, Connection c,
            double montantReversed) throws Exception {
        ComptaSousEcriture[] compta = {};
        boolean canClose = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                canClose = true;
            }
            Vente[] ventes = (Vente[]) CGenUtil.rechercher(new Vente("VENTE_MERE_MONTANT"), null, null, c,
                    " and id = '" + this.getId() + "'");
            if (ventes.length < 1)
                throw new Exception("Facture mere Introuvable");
            this.setCompte(getClient(c).getCompte());

            compta = new ComptaSousEcriture[2];

            compta[0] = new ComptaSousEcriture();
            compta[0].setLibellePiece("Annulation encaissement " + this.getDesignation());
            compta[0].setRemarque("Annulation encaissement " + this.getDesignation());
            compta[0].setCompte(getCaisse(c).getCompte());
            compta[0].setCredit(montantReversed); // Crédit caisse pour annulation

            compta[1] = new ComptaSousEcriture();
            compta[1].setLibellePiece("Annulation encaissement Client " + ventes[0].getClientlib());
            compta[1].setRemarque("Annulation encaissement Client " + ventes[0].getClientlib());
            compta[1].setCompte(this.getCompte());
            compta[1].setDebit(montantReversed); // Débit client pour annulation

        } catch (Exception e) {
            throw e;
        } finally {
            if (canClose) {
                c.close();
            }
        }
        return compta;
    }

    public void genererEcriture(String u, Connection c) throws Exception {
        ComptaEcriture mere = new ComptaEcriture();
        Date dateDuJour = utilitaire.Utilitaire.dateDuJourSql();
        int exercice = utilitaire.Utilitaire.getAnnee(daty);
        mere.setDaty(dateDuJour);
        mere.setDesignation(this.getDesignation());
        mere.setExercice("" + exercice);
        mere.setDateComptable(this.getDaty());
        mere.setJournal(ConstanteStation.JOURNALVENTE);
        mere.setOrigine(this.getId());
        mere.setIdobjet(this.getId());
        mere.createObject(u, c);

        ComptaSousEcriture[] filles = this.genererSousEcriture(c);
        for (int i = 0; i < filles.length; i++) {
            filles[i].setIdMere(mere.getId());
            filles[i].setExercice(exercice);
            filles[i].setDaty(this.getDaty());
            filles[i].setJournal(ConstanteStation.JOURNALVENTE);

            if (filles[i].getDebit() > 0 || filles[i].getCredit() > 0)
                filles[i].createObject(u, c);
        }
    }

    public Client getClient(Connection c) throws Exception {
        Client client = new Client();
        Client[] clients = (Client[]) CGenUtil.rechercher(client, null, null, c,
                " and id = '" + this.getIdClient() + "'");
        if (clients.length > 0) {
            return clients[0];
        }
        throw new Exception("Le client n'existe pas");
    }

    public ComptaSousEcriture[] genererSousEcriture(Connection c) throws Exception {
        ComptaSousEcriture[] compta = {};
        boolean canClose = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                canClose = true;
            }
            Vente[] ventes = (Vente[]) CGenUtil.rechercher(new Vente("VENTE_MERE_MONTANT"), null, null, c,
                    " and id = '" + this.getId() + "'");
            if (ventes.length < 1)
                throw new Exception("Facture mere Introuvable");
            Client client = getClient(c);
            this.setCompte(client.getCompte());
            VenteDetails[] details = this.getDetails(c);
            double montantHT = AdminGen.calculSommeDouble(details, "montantHT") * details[0].getTauxDeChange();
            double montantTva = AdminGen.calculSommeDouble(details, "montantTva") * details[0].getTauxDeChange();
            double montantTTC = AdminGen.calculSommeDouble(details, "montantTTC") * details[0].getTauxDeChange();
            int taille = details.length;
            compta = new ComptaSousEcriture[taille + 2];
            int i = 0;
            for (i = i; i < taille; i++) {
                compta[i] = new ComptaSousEcriture();
                compta[i].setLibellePiece(this.getDesignation());
                compta[i].setRemarque(details[i].getLibelle());
                compta[i].setCompte(details[i].getCompte());
                compta[i].setCredit(details[i].getMontantHT() * details[i].getTauxDeChange());
            }

            compta[i] = new ComptaSousEcriture();
            compta[i].setLibellePiece("TVA Collectee");
            compta[i].setRemarque("TVA Collectee");
            compta[i].setCompte(ConstanteStation.compteTVACollecte);
            // compta[i].setDebit((montantHT-retenue) * ((this.getTva()/100)));
            compta[i].setCredit(montantTva);
            i++;

            compta[i] = new ComptaSousEcriture();
            compta[i].setLibellePiece("Vente Client " + ventes[0].getClientlib());
            compta[i].setRemarque("Vente Client " + ventes[0].getClientlib());
            compta[i].setCompte(this.getCompte());
            compta[i].setCompte_aux(client.getCompteauxiliaire());
            compta[i].setDebit(montantTTC);
        } catch (Exception e) {
            throw e;
        } finally {
            if (canClose) {
                c.close();
            }
        }
        return compta;
    }

    public VenteDetails[] getDetails(Connection c) throws Exception {
        VenteDetails[] venteDetails = null;
        try {
            String awhere = " and IDVENTE = '" + this.getId() + "'";
            venteDetails = (VenteDetails[]) CGenUtil.rechercher(new VenteDetails("VENTE_GRP_VISER"), null, null, c,
                    awhere);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return venteDetails;
    }

    public VenteDetails[] getDetailsLib(Connection c) throws Exception {
        VenteDetails[] venteDetails = null;
        try {
            String awhere = " and IDVENTE = '" + this.getId() + "'";
            venteDetails = (VenteDetails[]) CGenUtil.rechercher(new VenteDetails("Vente_Details"), null, null, c,
                    awhere);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return venteDetails;
    }

    public Encaissement genererEncaissement() {
        Encaissement enc = new Encaissement();
        enc.setIdOrigine(this.getId());
        enc.setDaty(utilitaire.Utilitaire.dateDuJourSql());
        enc.setDesignation("Encaissement vente du " + this.getDaty() + " de la facture numéro " + this.getId());
        enc.setIdTypeEncaissement(ConstanteStation.TYPE_ENCAISSEMENT_ENTREE);
        return enc;
    }

    public void genererAPartirLivraison(String[] ids, String u, Connection c) throws Exception {
        boolean canClose = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                canClose = true;
            }
            As_BondeLivraisonClient[] bls = As_BondeLivraisonClient.getAll(ids, c);
            As_BondeLivraisonClient.controlerClient(bls);

            this.setDesignation("Facturation de Bon de Livraison");
            this.setIdClient(bls[0].getIdclient());
            this.setIdMagasin(bls[0].getMagasin());
            this.setDaty(Utilitaire.dateDuJourSql());
            this.createObject(u, c);
            for (As_BondeLivraisonClient bl : bls) {
                bl.setIdvente(this.getId());
                bl.updateToTableWithHisto(u, c);
            }
            As_BondeLivraisonClientFille blf = new As_BondeLivraisonClientFille();
            String[] somGr = { "quantite" };
            String[] gr = { "produit" };
            String[] tabvide = {};
            ResultatEtSomme rs = CGenUtil.rechercherGroupe(blf, gr, somGr, null, null,
                    " and numbl in " + Utilitaire.tabToString(ids, "'", ","), tabvide, "", c);
            As_BondeLivraisonClientFille[] blfs = (As_BondeLivraisonClientFille[]) rs.getResultat();
            for (As_BondeLivraisonClientFille item : blfs) {
                VenteDetails vd = item.toVenteDetails();
                vd.setIdVente(this.getId());
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

    public static AvoirFC genererAvoir(String u, Connection c, String idVente) throws SQLException, Exception {
        AvoirFC avoirFC = null;
        boolean estOuvert = false;
        if (c == null) {
            c = new UtilDB().GetConn();
            estOuvert = true;
            c.setAutoCommit(false);
        }
        try {

            Vente vente = Vente.getById(c, idVente);
            vente.getVenteDetailsNonGrp(c);

            int tailleFilles = vente.venteDetails.length;
            avoirFC = Vente.transformerFactureToAvoir(vente);
            avoirFC.createObject(u, c);
            AvoirFCFille[] avoirFCFilles = new AvoirFCFille[tailleFilles];
            for (int i = 0; i < tailleFilles; i++) {
                avoirFCFilles[i] = Vente.transformerFactureToAvoirFille(vente.venteDetails[i]);
                avoirFCFilles[i].setIdAvoirFC(avoirFC.getId());
                avoirFCFilles[i].createObject(u, c);
            }
            avoirFC.setAvoirDetails(avoirFCFilles);
            c.commit();
        } catch (Exception e) {
            if (estOuvert)
                c.rollback();
            throw e;
        } finally {
            if (estOuvert)
                c.close();
        }

        return avoirFC;
    }

    public static AvoirFC transformerFactureToAvoir(Vente vente) {
        AvoirFC valeur = new AvoirFC();
        valeur.setDesignation(vente.getDesignation());
        valeur.setIdMagasin(vente.getIdMagasin());
        valeur.setRemarque(vente.getRemarque());
        valeur.setIdOrigine(vente.getIdOrigine());
        valeur.setIdClient(vente.getIdClient());
        valeur.setIdVente(vente.getId());
        valeur.setCompte(vente.getCompte());
        valeur.setDaty(vente.getDaty());
        valeur.setEtat(1);
        return valeur;
    }

    public static AvoirFCFille transformerFactureToAvoirFille(VenteDetails venteDetails) throws Exception {
        AvoirFCFille valeur = new AvoirFCFille();
        // valeur.setIdAvoirFC(venteDetails.get());
        valeur.setIdProduit(venteDetails.getIdProduit());
        // valeur.setIdOrigine(venteDetails.getIdOrigine());
        valeur.setQte(1);
        valeur.setPu(0);
        valeur.setTva(0);
        valeur.setPuAchat(0);
        valeur.setPuVente(0);
        valeur.setIdDevise(venteDetails.getIdDevise());
        valeur.setTauxDeChange(1);
        valeur.setDesignation(venteDetails.getIdProduit());
        // valeur.setIdVenteDetails(venteDetails.getId());
        valeur.setEtat(11);
        return valeur;
    }

    public static Vente getById(Connection c, String id) throws Exception {
        Vente vtn = new Vente();
        try {
            vtn.setId(id);
            vtn = ((Vente[]) CGenUtil.rechercher(vtn, null, null, c, "")).length > 0
                    ? (Vente) ((Vente[]) CGenUtil.rechercher(vtn, null, null, c, ""))[0]
                    : null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return vtn;
    }

    public VenteDetails[] getVenteDetailsNonGrp(Connection c) throws Exception {
        VenteDetails[] venteDetails = null;
        try {
            String awhere = " and IDVENTE = '" + this.getId() + "'";
            venteDetails = (VenteDetails[]) CGenUtil.rechercher(new VenteDetails(), null, null, c, awhere);
            this.setVenteDetails(venteDetails);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return venteDetails;
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

    public Vente[] controllerVentePaiementMultiple(String[] ids) throws Exception {
        String aWhere = Utilitaire.getAWhereIn(ids, "id");
        Vente[] ventes = (Vente[]) CGenUtil.rechercher(new Vente(), null, null, null, aWhere);
        String firstClientId = ventes[0].getIdClient();
        for (int i = 0; i < ventes.length; i++) {
            if (!ventes[i].getIdClient().equalsIgnoreCase(firstClientId))
                throw new Exception("Clients differents!");
            if (ventes[i].getEtat() < 11)
                throw new Exception("Presence de facture non visee!");
        }
        return ventes;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public static VenteLib genererVenteClient(String[] ids, Connection c) throws Exception {
        boolean estOuvert = false;
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                estOuvert = true;
            }
            VenteLib[] liste = (VenteLib[]) CGenUtil.rechercher(new VenteLib(), null, null, c,
                    " AND ID IN (" + Utilitaire.tabToString(ids, "'", ",") + ")");
            if (liste.length <= 0) {
                return null;
            }
            String idClient = liste[0].getIdClient();
            double montant = liste[0].getMontantreste();
            for (int i = 1; i < liste.length; i++) {
                if (liste[i].getIdClient().compareToIgnoreCase(idClient) != 0) {
                    throw new Exception("Client different pour ces ventes");
                }
                montant += liste[i].getMontantreste();
            }
            liste[0].setMontantreste(montant);
            return liste[0];
        } catch (Exception e) {
            throw e;
        } finally {
            if (estOuvert)
                c.close();
        }
    }

    public static LiaisonPaiement genererPaiementFactureParAvoir(String u, Connection c, String idVente)
            throws SQLException, Exception {
        LiaisonPaiement pFacture = null;
        boolean estOuvert = false;
        if (c == null) {
            c = new UtilDB().GetConn();
            estOuvert = true;
            c.setAutoCommit(false);
        }
        try {

            Vente vente = Vente.getById(c, idVente);
            AvoirFCLib t = new AvoirFCLib();
            t.setNomTable("AVOIRFCLIB_CPL");
            AvoirFCLib[] details = (AvoirFCLib[]) CGenUtil.rechercher(t, null, null, c,
                    " AND idClient='" + vente.getIdClient() + "' order by id asc");
            StringBuilder sb = new StringBuilder();
            if (details.length > 0) {
                for (int i = 0; i < details.length; i++) {
                    if (i > 0)
                        sb.append(";");
                    sb.append(details[i].getId());
                }
            }
            double montantTotalReste = AdminGen.calculSommeDouble(details, "resteapayerar");
            pFacture = new LiaisonPaiement();
            pFacture.setId2(idVente);
            // pFacture.setId1(sb.toString());
            // pFacture.setDaty(vente.getDaty());
            pFacture.setMontant(montantTotalReste);
            c.commit();
        } catch (Exception e) {
            if (estOuvert)
                c.rollback();
            throw e;
        } finally {
            if (estOuvert)
                c.close();
        }

        return pFacture;
    }

    public double getEcheancefacture() {
        return echeancefacture;
    }

    public void setEcheancefacture(double echeancefacture) {
        this.echeancefacture = echeancefacture;
    }

    public static MvtCaisse genererPaiementFacture(String[] ids, Connection c) throws Exception {
        System.out.println("ids = " + ids.length);
        String requete = null;
        boolean ifconnnull = false;
        MvtCaisse mvt = new MvtCaisse();
        try {
            if (c == null) {
                c = new UtilDB().GetConn();
                ifconnnull = true;
                VenteLib ff = new VenteLib();
                ff.setNomTable("VENTE_CPL");
                requete = "SELECT * FROM " + ff.getNomTable() + " WHERE id IN (";
                requete += Utilitaire.tabToString(ids, "'", ",") + ")";
                System.out.println(requete);
                VenteLib[] liste = (VenteLib[]) CGenUtil.rechercher(ff, requete, c);
                Set<String> clients = new HashSet<>();
                for (VenteLib v : liste) {
                    clients.add(v.getIdClient());
                }
                if (clients.size() > 1) {
                    throw new Exception("Client different pour ces ventes !");
                }

                double montantTotal = AdminGen.calculSommeDouble(liste, "montantreste");
                mvt.setIdOrigine(String.join(";;", ids));
                mvt.setCredit(montantTotal);
                mvt.setIdDevise("Ar");
                mvt.setIdTiers(liste[0].getIdClient());
                mvt.setDesignation("Paiement facture  par " + liste[0].getIdClientLib());
            }
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        } finally {
            if (ifconnnull && c != null) {
                c.close();
            }
        }
        return mvt;
    }

    public void annulerVenteCustom(String u, Connection c, String idMagasinAnnulation) throws Exception {
        inverserMouvementsStock(u, c, idMagasinAnnulation);
        annulerEncaissementsLies(u, c);
    }

    private void inverserMouvementsStock(String u, Connection c, String idMagasinAnnulation) throws Exception {
        MvtStock mvt = this.createMvtStockAnnulation();
        if (idMagasinAnnulation == null || idMagasinAnnulation.trim().isEmpty()) {
            throw new Exception("Champ magasin obligatoire pour l'annulation de la vente " + this.getId());
        }
        mvt.setIdMagasin(idMagasinAnnulation);
        MvtStockFille[] mvtf = this.createMvtStockFillesReversed(c);
        System.out.println("DEBUG dans inverser: mvtf : " + mvtf.length);
        mvt.setFille(mvtf);
        mvt.createObject(u, c);
        mvt.validerObject(u, c);
    }

    private void annulerEncaissementsLies(String u, Connection c) throws Exception {
        Encaissement enc = new Encaissement();
        Encaissement[] encs = (Encaissement[]) CGenUtil.rechercher(enc,
                "SELECT * FROM ENCAISSEMENT WHERE IDORIGINE='" + this.getId() + "'", c);
        for (Encaissement e : encs) {
            e.setEtat(0); // cancel
            e.updateToTable(u, c);
        }
    }

}
