package vente;

import bean.CGenUtil;
import bean.ClassMere;
import produits.Ingredients;
import stock.MvtStock;
import stock.MvtStockFille;
import utilitaire.UtilDB;
import utils.ConstanteStation;
import caisse.MvtCaisse;

import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VenteRetourClient extends ClassMere {

    private String id;
    private String idVente;
    private String idVenteDetail;
    private String idClient;
    private String idTypeMotif;
    private String idMagasin;
    private String remarque;
    private String designation;
    private double qte;
    private double pu;
    private Date dateRetour;
    private Date daty;

    VenteRetourDetail[] venteRetourDetail;

    public Vente generateVenteClient(Connection c) throws Exception {
        boolean estOuvert = false;
        try {
            if (c == null) {
                c = (new UtilDB()).GetConn();
                estOuvert = true;
            }

            VenteRetourClient venteRetourClient = (VenteRetourClient) this.getById(this.getId(), this.getNomTable(), c);
            VenteRetourClient[] vrts = (VenteRetourClient[]) CGenUtil.rechercher(new VenteRetourClient(), null, null, c,
                    " AND ID='" + this.getId() + "' ");
            if (vrts == null || vrts.length == 0)
                throw new Exception("aucune retour trouver");
            venteRetourClient = vrts[0];

            VenteRetourDetail[] venteRetourDetails = (VenteRetourDetail[]) CGenUtil.rechercher(new VenteRetourDetail(),
                    null, null, c, "and IDVENTERETOUR='" + venteRetourClient.getId() + "'");

            InsertionVente insertionVente = new InsertionVente();
            insertionVente.setIdClient(venteRetourClient.getIdClient());
            insertionVente.setIdMagasin(venteRetourClient.getIdMagasin());
            insertionVente.setDesignation(venteRetourClient.getDesignation());
            insertionVente.setReferencefact(venteRetourClient.getIdVente());
            List<VenteDetails> venteDetails = new ArrayList<>();

            for (VenteRetourDetail retourDetail : venteRetourDetails) {
                venteDetails.add(generateVenteDetail(retourDetail));
            }
            if (!venteDetails.isEmpty()) {
                insertionVente.setFille(venteDetails.toArray(new VenteDetails[0]));
            }
            return insertionVente;
        } catch (Exception ex) {
            Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } finally {
            if (estOuvert) {
                c.close();
            }
        }
    }

    public MvtStock genererMvtStockEntreeSortie(Connection c, String typeMvtStock, boolean estEntrant)
            throws Exception {
        boolean estOuvert = false;
        MvtStock mvt;
        try {
            if (c == null) {
                c = (new UtilDB()).GetConn();
                estOuvert = true;
            }
            VenteRetourClient venteRetourClient = (VenteRetourClient) this.getById(this.getId(), this.getNomTable(), c);
            VenteRetourClient[] vrts = (VenteRetourClient[]) CGenUtil.rechercher(new VenteRetourClient(), null, null, c,
                    " AND ID='" + this.getId() + "' ");
            System.out.println(vrts.length + " longueur oo");
            if (vrts == null || vrts.length == 0)
                throw new Exception("aucune retour trouver");
            venteRetourClient = vrts[0];

            VenteDetails venteDetail = new VenteDetails();
            venteDetail.setId(venteRetourClient.getIdVenteDetail());
            VenteDetails[] venteDetails = (VenteDetails[]) CGenUtil.rechercher(venteDetail, null, null, c, "");

            MvtStock mvtStock = new MvtStock();
            mvtStock.setIdobjet(venteRetourClient.getId());
            mvtStock.setIdVente(venteRetourClient.getIdVente());
            mvtStock.setDesignation(venteRetourClient.getDesignation());
            mvtStock.setIdMagasin(venteRetourClient.getIdMagasin());
            mvtStock.setDaty(venteRetourClient.getDaty());
            mvtStock.setIdTypeMvStock(typeMvtStock);
            MvtStockFille[] mvtStockFilles = new MvtStockFille[venteDetails.length];
            for (int i = 0; i < venteDetails.length; i++) {
                mvtStockFilles[i] = this.generateStockFille(venteDetails[i], venteRetourClient.getQte(), estEntrant);
            }
            mvtStock.setFille(mvtStockFilles);
            mvt = mvtStock;
        } catch (Exception ex) {
            Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } finally {
            if (estOuvert) {
                c.close();
            }
        }
        return mvt;
    }

    public void controllerEtatVente(Connection c) throws Exception {
        boolean estOuvert = false;
        try {
            if (c == null) {
                c = (new UtilDB()).GetConn();
                estOuvert = true;
            }
            Vente[] vts = (Vente[]) CGenUtil.rechercher(new Vente(), null, null, c,
                    " AND ID='" + this.getIdVente() + "' ");
            if (vts == null || vts.length == 0)
                throw new Exception("Aucune vente trouve pour le retour");
            Vente vente = vts[0];
            if (vente.getEtat() < 7 && vente.getEtat() > 0)
                throw new Exception("La vente doit etre validee avant de faire un retour");
            if (vente.getEtat() <= 0)
                throw new Exception("La vente est annulée, impossible de faire un retour");

        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        } finally {
            if (estOuvert && c != null)
                c.close();
        }
    }

    private void genererEntreeSortieStockRetour(String user, Connection c, VenteRetourClient retour,
            InsertionVente facture, boolean estEntrant, String typeMouvement)
            throws Exception {
        if (retour == null || retour.getId() == null)
            return;
        try {
            MvtStock crit = new MvtStock();
            crit.setIdobjet(retour.getId());
            MvtStock[] existants = (MvtStock[]) CGenUtil.rechercher(crit, null, null, c,
                    " AND IDOBJET='" + retour.getId() + "' AND IDTYPEMVSTOCK='" + typeMouvement + "'");
            if (existants != null && existants.length > 0) {
                Logger.getLogger(VenteRetourClient.class.getName()).log(Level.INFO,
                        "Mvt stock entrée déjà présent pour retour=" + retour.getId());
                return;
            }
        } catch (Exception ignore) {
        }
        MvtStock mvt = this.genererMvtStockEntreeSortie(c, typeMouvement, estEntrant);
        System.out.println("mvt: " + mvt + " type: " + estEntrant);
        if (mvt == null)
            return;
        mvt.setIdVente(facture != null ? facture.getId() : retour.getIdVente());
        if (mvt.getDaty() == null) {
            Date d = facture != null ? facture.getDaty() : retour.getDaty();
            if (d == null)
                d = new Date(System.currentTimeMillis());
            mvt.setDaty(d);
        }
        if (mvt.getDesignation() == null || mvt.getDesignation().trim().isEmpty()) {
            if (estEntrant)
                mvt.setDesignation("Retour client entrée stock " + retour.getId());
            else
                mvt.setDesignation("Retour client sortie stock " + retour.getId());

        }
        mvt.setIdTypeMvStock(typeMouvement);
        mvt.construirePK(c);
        mvt.createObjectMultiple(user, c);
        mvt.validerObject(user, c);
        if (estEntrant)
            Logger.getLogger(VenteRetourClient.class.getName()).log(Level.INFO,
                    "Mvt stock ENTREE généré id=" + mvt.getId() + " pour retour=" + retour.getId());
        else
            Logger.getLogger(VenteRetourClient.class.getName()).log(Level.INFO,
                    "Mvt stock SORTIE généré id=" + mvt.getId() + " pour retour=" + retour.getId());

    }

    public InsertionVente echanger(String user, Connection c) throws Exception {
        boolean estOuvert = false;
        try {
            if (c == null) {
                c = (new UtilDB()).GetConn();
                estOuvert = true;
            }

            VenteRetourClient venteRetourClient = (VenteRetourClient) this.getById(this.getId(), this.getNomTable(), c);
            VenteRetourClient[] vrts = (VenteRetourClient[]) CGenUtil.rechercher(new VenteRetourClient(), null, null, c,
                    " AND ID='" + this.getId() + "' ");
            if (vrts == null || vrts.length == 0)
                throw new Exception("Aucune retour trouver");
            venteRetourClient = vrts[0];

            VenteRetourDetail[] venteRetourDetails = (VenteRetourDetail[]) CGenUtil.rechercher(
                    new VenteRetourDetail(), null, null, c, "and IDVENTERETOUR='" + venteRetourClient.getId() + "'");
            double montantTotalDetails = 0d;
            for (VenteRetourDetail d : venteRetourDetails)
                montantTotalDetails += d.getQte() * d.getPu();
            if (montantTotalDetails <= 0)
                throw new Exception("Montant non valide");

            try {
                Vente v = this.generateVenteClient(c);
                if (v instanceof InsertionVente) {
                    InsertionVente facture = (InsertionVente) v;

                    // Date obligatoire
                    Date datySource = facture.getDaty();
                    if (datySource == null)
                        datySource = this.getDaty();
                    if (datySource == null)
                        datySource = this.getDateRetour();
                    if (datySource == null)
                        datySource = new Date(System.currentTimeMillis());
                    facture.setDaty(datySource);
                    // DatyPrevu nul pour empêcher la génération de Prevision
                    try {
                        facture.getClass().getMethod("setDatyPrevu", Date.class).invoke(facture, (Date) null);
                    } catch (Exception ignore) {
                    }

                    double totalHT = 0d, totalTVA = 0d, totalTTC = 0d;

                    if (facture.getFille() != null && facture.getFille().length > 0) {
                        facture.construirePK(c);
                        for (VenteDetails vd : (VenteDetails[]) facture.getFille()) {
                            if (vd.getId() == null || vd.getId().isEmpty())
                                vd.construirePK(c);
                            vd.setIdVente(facture.getId());
                            if (vd.getIdDevise() == null || vd.getIdDevise().trim().isEmpty())
                                vd.setIdDevise("AR");

                            double pu = vd.getPu();
                            double qte = vd.getQte();
                            double tvaLigne = vd.getTva();
                            if (Double.isNaN(tvaLigne))
                                tvaLigne = 0d;

                            double montantHTLigne = pu * qte;
                            double montantTVALigne = montantHTLigne * tvaLigne / 100d;
                            double montantTTCLigne = montantHTLigne + montantTVALigne;
                            try {
                                vd.setMontantHT(montantHTLigne);
                            } catch (Exception ignore) {
                            }
                            try {
                                vd.setMontantTva(montantTVALigne);
                            } catch (Exception ignore) {
                            }
                            try {
                                vd.setMontantTTC(montantTTCLigne);
                            } catch (Exception ignore) {
                            }
                            try {
                                vd.setMontant(montantTTCLigne);
                            } catch (Exception ignore) {
                            }

                            totalHT += montantHTLigne;
                            totalTVA += montantTVALigne;
                            totalTTC += montantTTCLigne;
                        }

                        // Totaux parent
                        try {
                            facture.getClass().getMethod("setMontantHT", double.class).invoke(facture, totalHT);
                        } catch (Exception ignore) {
                        }
                        try {
                            facture.getClass().getMethod("setMontantTva", double.class).invoke(facture, totalTVA);
                        } catch (Exception ignore) {
                        }
                        try {
                            facture.getClass().getMethod("setMontantTTC", double.class).invoke(facture, totalTTC);
                        } catch (Exception ignore) {
                        }
                        try {
                            facture.getClass().getMethod("setMontant", double.class).invoke(facture, totalTTC);
                        } catch (Exception ignore) {
                        }

                        try {
                            facture.getClass().getMethod("setEstPrevu", int.class).invoke(facture, 2);
                        } catch (Exception ignore) {

                        }

                        if (totalTTC <= 0)
                            throw new Exception("Total TTC nul pour facture retour");

                        facture = (InsertionVente) facture.createObject(user, c);
                        System.out.println("Tsy aiko hoe tonga ve sa aona!: " + facture.getId());

                        // Mise à jour des quantités sur les lignes de la vente d'origine : diminuer qté
                        // par la qté retournée
                        // try {
                        // VenteDetails[] origs = (VenteDetails[]) CGenUtil.rechercher(new
                        // VenteDetails(), null, null, c,
                        // " AND ID='" + this.getIdVenteDetail() + "'");
                        // if (origs != null && origs.length > 0) {
                        // VenteDetails orig = origs[0];
                        // double nouvelleQte = orig.getQte() - this.getQte();
                        // if (Double.isNaN(nouvelleQte) || nouvelleQte < 0d)
                        // nouvelleQte = 0d;
                        // orig.setMode("modif");
                        // try {
                        // orig.setQte(nouvelleQte);
                        // } catch (Exception ignore) {
                        // }
                        // try {
                        // orig.updateToTableWithHisto(user, c);
                        // Logger.getLogger(VenteRetourClient.class.getName()).log(Level.INFO,
                        // "Mise a jour qte VenteDetails id=" + orig.getId() + " -> " + nouvelleQte);
                        // } catch (Exception updErr) {
                        // Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                        // "Impossible de mettre a jour la qte pour VenteDetails id=" +
                        // this.getIdVenteDetail(), updErr);
                        // }
                        // } else {
                        // Logger.getLogger(VenteRetourClient.class.getName()).log(Level.WARNING,
                        // "Ligne vente origine introuvable id=" + this.getIdVenteDetail());
                        // }
                        // } catch (Exception inner) {
                        // Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                        // "Erreur traitement ligne retour pour idVenteDetail", inner);
                        // }

                        // Génération automatique mouvement de stock ENTREE (produits retournés)
                        try {
                            genererEntreeSortieStockRetour(user, c, venteRetourClient, facture, true, "TPMVST000001");
                        } catch (Exception mvtErr) {
                            Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                                    "Erreur génération mvt stock entrée pour retour=" + venteRetourClient.getId(),
                                    mvtErr);
                        }
                        return facture;
                    }
                }
            } catch (Exception e) {
                Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                        "Erreur création facture auto (retour=" + this.getId() + ")", e);
            }
        } catch (Exception e) {
            Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                    "Erreur création facture auto (retour=" + this.getId() + ")", e);
        } finally {
            if (estOuvert && c != null)
                c.close();
        }
        return null;
    }

    public InsertionVente echangerEtPayer(String user, Connection c) throws Exception {
        boolean estOuvert = false;
        InsertionVente facture = null;
        try {
            if (c == null) {
                c = (new UtilDB()).GetConn();
                estOuvert = true;
            }

            facture = this.echanger(user, c);
            if (facture == null)
                throw new Exception("Facture non générée pour le retour " + this.getId());

            System.out.println("Facture retour générée ID=" + facture.getId());

            // 2. Validation facture
            facture = (InsertionVente) facture.validerObject(user, c);
            System.out.println("Facture validée ID=" + facture.getId());

            double montantTTCEnc = 0d;
            VenteDetails critere = new VenteDetails();
            critere.setIdVente(facture.getId());
            VenteDetails[] lignes = null;
            try {
                lignes = (VenteDetails[]) CGenUtil.rechercher(critere, null, null, c,
                        " AND IDVENTE='" + facture.getId() + "'");
            } catch (Exception rechercheErr) {
                Logger.getLogger(VenteRetourClient.class.getName()).log(Level.WARNING,
                        "Impossible de récupérer les lignes de facture=" + facture.getId() + " : "
                                + rechercheErr.getMessage());
            }
            if (lignes != null) {
                for (VenteDetails vd : lignes) {
                    double ht = vd.getPu() * vd.getQte();
                    double tvaPct = vd.getTva();
                    if (Double.isNaN(tvaPct))
                        tvaPct = 0d;
                    montantTTCEnc += ht + (ht * tvaPct / 100d);
                }
                if (montantTTCEnc <= 0d) {
                    montantTTCEnc = 0d;
                    for (VenteDetails vd : lignes) {
                        montantTTCEnc += vd.getPu() * vd.getQte();
                    }
                }
            }

            if (montantTTCEnc <= 0d) {
                Logger.getLogger(VenteRetourClient.class.getName()).log(Level.INFO,
                        "Montant encaissement nul après recalcul (facture=" + facture.getId()
                                + ") - création mouvement de caisse avec 0");
            }

            // Détermination date de caisse (fallback chaine similaire à echanger)
            Date datySource = facture.getDaty();
            if (datySource == null)
                datySource = this.getDaty();
            if (datySource == null)
                datySource = this.getDateRetour();
            if (datySource == null)
                datySource = new Date(System.currentTimeMillis());
            Date datyCaisse = facture.getDaty() != null ? facture.getDaty() : datySource;

            // Devise & taux
            String devise = "AR";
            try {
                if (facture.getIdDevise() != null && !facture.getIdDevise().trim().isEmpty()) {
                    devise = facture.getIdDevise();
                }
            } catch (Exception ignore) {
            }
            double taux = 1d;
            try {
                taux = (double) facture.getClass().getMethod("getTauxdechange").invoke(facture);
                if (Double.isNaN(taux) || taux <= 0)
                    taux = 1d;
            } catch (Exception ignore) {
            }

            MvtCaisse enc = new MvtCaisse();
            enc.setDaty(datyCaisse);
            enc.setDesignation("Paiement de la facture : " + facture.getId());
            enc.setIdOrigine(facture.getId());
            enc.setIdTiers(this.getIdClient());
            enc.setIdDevise(devise);
            enc.setTaux(taux);
            enc.setCredit(montantTTCEnc);
            enc.setDebit(0);
            enc.setIdCaisse(ConstanteStation.idCaisse);

            try {
                enc.construirePK(c);
                enc.createObject(user, c);
                enc.validerObject(user, c);
                System.out.println("Tonga eto le izy - Encaissement OK facture=" + facture.getId());
            } catch (Exception encErr) {
                Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                        "Erreur encaissement automatique (facture=" + facture.getId() + ")", encErr);
            }

            MvtCaisse retour = new MvtCaisse();
            retour.setDaty(datyCaisse);
            retour.setDesignation("Contre de remboursement en credit : " + facture.getId());
            retour.setIdOrigine(facture.getId());
            retour.setIdTiers(this.getIdClient());
            retour.setIdDevise(devise);
            retour.setTaux(taux);
            retour.setCredit(0);
            retour.setDebit(this.getPu() * this.getQte());
            retour.setIdCaisse(ConstanteStation.idCaisse);
            try {
                retour.construirePK(c);
                retour.createObject(user, c);
                retour.validerObject(user, c);
                System.out.println("Tonga eto le izy - Encaissement OK facture=" + facture.getId());
            } catch (Exception encErr) {
                Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                        "Erreur encaissement automatique (facture=" + facture.getId() + ")", encErr);
            }

            return facture;
        } catch (Exception e) {
            Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                    "Erreur echangerEtPayer (retour=" + this.getId() + ")", e);
            throw e;
        } finally {
            if (estOuvert && c != null)
                c.close();
        }
    }

    public InsertionVente echangerEtPayerEtLivrer(String user, Connection c) throws Exception {
        boolean estOuvert = false;
        Connection cx = c;
        InsertionVente facture = null;
        try {
            if (cx == null) {
                cx = (new UtilDB()).GetConn();
                estOuvert = true;
            }

            // 1. Échange + paiement (doit retourner la facture)
            facture = this.echangerEtPayer(user, cx);
            if (facture == null) {
                Logger.getLogger(VenteRetourClient.class.getName()).log(Level.INFO,
                        "Aucune facture générée pour retour " + this.getId());
                return null;
            }

            // 2. Génération mouvement de stock sortie
            VenteRetourClient[] vrts = (VenteRetourClient[]) CGenUtil.rechercher(new VenteRetourClient(), null, null, c,
                    " AND ID='" + this.getId() + "' ");
            if (vrts == null || vrts.length == 0)
                throw new Exception("Aucune retour trouver");

            VenteRetourClient venteRetourClient = vrts[0];
            System.out.println(this.getId() + " retour oo");
            System.out.println("Manomboka mamoaka stock");
            try {
                genererEntreeSortieStockRetour(user, cx, venteRetourClient, facture, false, "TPMVST000022");
                System.out.println("Tafavoaka le stock");
            } catch (Exception mvtErr) {
                Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                        "Erreur génération mvt stock sortie pour retour=" + venteRetourClient.getId(),
                        mvtErr);
            }
            System.out.println("Egnaie ka mety");
            return facture;
        } catch (Exception ex) {
            Logger.getLogger(VenteRetourClient.class.getName()).log(Level.SEVERE,
                    "Erreur echangerPayerEtGenererStockSortie retour=" + this.getId(), ex);
        } finally {
            if (estOuvert && cx != null && !cx.isClosed()) {
                cx.close();
            }
        }
        return facture;
    }

    private VenteDetails generateVenteDetail(VenteRetourDetail venteRetourDetail) throws Exception {
        VenteDetails venteDetail = new VenteDetails();
        venteDetail.setIdProduit(venteRetourDetail.getIdProduit());
        venteDetail.setDesignation(venteRetourDetail.getDesignation());
        venteDetail.setQte(venteRetourDetail.getQte());
        venteDetail.setPu(venteRetourDetail.getPu());
        return venteDetail;
    }

    private MvtStockFille generateStockFille(VenteDetails venteDetail, double qte, boolean estEntrant)
            throws Exception {
        MvtStockFille fille = new MvtStockFille();
        fille.setIdVenteDetail(venteDetail.getId());
        fille.setDesignation(venteDetail.getDesignation());
        if (estEntrant)
            fille.setEntree(qte);

        else
            fille.setSortie(venteDetail.getQte());

        fille.setPu(venteDetail.getPu());
        fille.setIdProduit(venteDetail.getIdProduit());
        return fille;
    }

    public VenteRetourClient() {
        super.setNomTable("VENTE_RETOUR_CLIENT");
    }

    @Override
    public void construirePK(Connection c) throws Exception {
        this.preparePk("VRT", "SEQVENTERETOURCLIENT.NEXTVAL");
        this.setId(makePK(c));
    }

    private void verifierChangeRetourPrix(Connection c) throws Exception {
        VenteDetails[] venteDetails = (VenteDetails[]) CGenUtil.rechercher(new VenteDetails(), null, null, c,
                " AND ID='" + this.getIdVenteDetail() + "' ");
        if (venteDetails.length == 0 || venteDetails == null)
            throw new Exception("Aucun vente detail trouver");

        VenteDetails venteDetail = venteDetails[0];
        System.out.println("Debug alea 1: " + venteDetail.getId() + " , length" + venteDetails.length);
        Ingredients[] ingredients = (Ingredients[]) CGenUtil.rechercher(new Ingredients(), null, null, c,
                " AND ID='" + venteDetail.getIdProduit() + "' ");

        if (ingredients.length == 0 || ingredients == null)
            throw new Exception("Aucun ingredient trouver");
        Ingredients ingredient = ingredients[0];
        System.out.println("Debug alea 2: " + ingredient.getId() + " , length" + ingredients.length);

        if (ingredient.getEstChangeable() == 0)
            throw new Exception("Ingredient non echangeable");

        double montantTotalFille = 0d;
        for (VenteRetourDetail venteRetourDetail : (VenteRetourDetail[]) this.getFille())
            montantTotalFille += venteRetourDetail.getPu() * venteRetourDetail.getQte();
        double montantRetournee = this.getPu() * this.getQte();

        if (ingredient.getEstChangeable() == 1 && montantTotalFille <= montantRetournee)
            throw new Exception("Le montant total ne peut pas etre inferieur au montant retourner");

    }

    private void verifierQuantiteRetournee(Connection c) throws Exception {
        VenteDetails[] venteDetails = (VenteDetails[]) CGenUtil.rechercher(new VenteDetails(), null, null,
                " AND ID='" + this.getIdVenteDetail() + "' ");
        if (venteDetails == null || venteDetails.length == 0)
            throw new Exception("Vente detail associe non trouvee");

        VenteDetails venteDetail = venteDetails[0];
        if (venteDetail.getQte() < this.getQte())
            throw new Exception("Quantite retournee superieure au quantite initial");

        VenteRetourClient[] venteRetourClients = (VenteRetourClient[]) CGenUtil.rechercher(new VenteRetourClient(),
                null, null,
                " AND IDVENTEDETAIL='" + this.getIdVenteDetail() + "' AND ETAT >= 11");
        if (venteRetourClients != null && venteRetourClients.length > 0) {
            double qteRendue = 0d;
            for (int i = 0; i < venteRetourClients.length; i++)
                qteRendue += venteRetourClients[i].getQte();

            if (qteRendue + getQte() > venteDetail.getQte())
                throw new Exception("Tous les produits concernants cette vente ont tous ete rendu");

        }
    }

    @Override
    public void controler(Connection c) throws Exception {
        Logger.getLogger(VenteRetourClient.class.getName()).log(Level.INFO, "VenteRetourClient.controler");
        VenteRetourClient venteRetour = new VenteRetourClient();
        venteRetour.setIdVenteDetail(this.idVenteDetail);

        VenteRetourClient[] venteDetailsArray = (VenteRetourClient[]) CGenUtil.rechercher(venteRetour, null, null, c,
                "");
        if (venteDetailsArray.length > 0)
            throw new Exception("le retour du vente detail a été deja inserer");
        controllerEtatVente(c);
        verifierChangeRetourPrix(c);
        verifierQuantiteRetournee(c);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdVente() {
        return idVente;
    }

    public void setIdVente(String idVente) {
        this.idVente = idVente;
    }

    public String getIdClient() {
        return idClient;
    }

    public void setIdClient(String idClient) {
        this.idClient = idClient;
    }

    public String getIdTypeMotif() {
        return idTypeMotif;
    }

    public void setIdTypeMotif(String idTypeMotif) {
        this.idTypeMotif = idTypeMotif;
    }

    public String getIdMagasin() {
        return idMagasin;
    }

    public void setIdMagasin(String idMagasin) {
        this.idMagasin = idMagasin;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public double getQte() {
        return qte;
    }

    public void setQte(double qte) {
        this.qte = qte;
    }

    @Override
    public String getLiaisonFille() {
        return "idVenteRetour";
    }

    @Override
    public String getNomClasseFille() {
        return "vente.VenteRetourDetail";
    }

    @Override
    public String getTuppleID() {
        return this.getId();
    }

    @Override
    public String getAttributIDName() {
        return "id";
    }

    public VenteRetourDetail[] getVenteRetourDetail() {
        return venteRetourDetail;
    }

    public void setVenteRetourDetail(VenteRetourDetail[] venteRetourDetail) {
        this.venteRetourDetail = venteRetourDetail;
    }

    public String getIdVenteDetail() {
        return idVenteDetail;
    }

    public void setIdVenteDetail(String idVenteDetail) {
        this.idVenteDetail = idVenteDetail;
    }

    public Date getDaty() {
        return daty;
    }

    public void setDaty(Date daty) {
        this.daty = daty;
    }

    public double getPu() {
        return pu;
    }

    public void setPu(double pu) {
        this.pu = pu;
    }

    public Date getDateRetour() {
        return dateRetour;
    }

    public void setDateRetour(Date dateRetour) {
        this.dateRetour = dateRetour;
    }
}
