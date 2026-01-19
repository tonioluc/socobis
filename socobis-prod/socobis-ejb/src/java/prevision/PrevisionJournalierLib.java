package prevision;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bean.CGenUtil;
import bean.ClassMAPTable;
import caisse.MvtCaisse;
import faturefournisseur.FactureFournisseur;
import utilitaire.Utilitaire;
import vente.Vente;

public class PrevisionJournalierLib extends ClassMAPTable {
    Date daty;
    double recettes;
    double depenses;
    double reste;

    public PrevisionJournalierLib() {
        super();
    }

    @Override
    public String getAttributIDName() {
        return "daty";
    }

    @Override
    public String getTuppleID() {
        return getDaty().toString();
    }

    @Override
    public String getNomTable() {
        return "PREVISION_JOURNALIER_LIB";
    }

    // Getters and setters
    public Date getDaty() {
        return daty;
    }

    public void setDaty(Date daty) {
        this.daty = daty;
    }

    public double getRecettes() {
        return recettes;
    }

    public void setRecettes(double recettes) {
        this.recettes = recettes;
    }

    public double getDepenses() {
        return depenses;
    }

    public void setDepenses(double depenses) {
        this.depenses = depenses;
    }

    public double getReste() {
        return reste;
    }

    public void setReste(double reste) {
        this.reste = reste;
    }

    protected double[] calculeMontantPaye(PrevisionComplet previsionComplet) throws Exception {
        if (previsionComplet.isDepense()) {
            MvtCaisse[] mvtCaisses = (MvtCaisse[]) CGenUtil.rechercher(new MvtCaisse(), null, null,
                    " AND IDPREVISION = '" + previsionComplet.getId() + "' ");
            if (mvtCaisses != null && mvtCaisses.length > 0) {
                double totalDebit = 0;
                for (MvtCaisse mvtCaisse : mvtCaisses) {
                    if (mvtCaisse.getEtat() >= 1) {
                        if (mvtCaisse.getIdOrigine().startsWith("FCF")) {
                            FactureFournisseur[] facture = (FactureFournisseur[]) CGenUtil.rechercher(
                                    new FactureFournisseur(), null, null,
                                    " AND ID = '" + mvtCaisse.getIdOrigine() + "' ");
                            if (facture != null && facture.length > 0 && facture[0].getEtat() >= 1) {
                                totalDebit += mvtCaisse.getDebit();
                            }
                        } else if (mvtCaisse.getIdOrigine().startsWith("VNT")) {
                            Vente[] ventes = (Vente[]) CGenUtil.rechercher(new Vente(), null, null,
                                    " AND ID = '" + mvtCaisse.getIdOrigine() + "' ");
                            if (ventes != null && ventes.length > 0 && ventes[0].getEtat() >= 1) {
                                totalDebit += mvtCaisse.getDebit();
                            }
                            totalDebit += mvtCaisse.getDebit();
                        } else if (mvtCaisse.getIdOrigine() == null) {
                            totalDebit += mvtCaisse.getDebit();
                        }

                    }
                }
                return new double[] { totalDebit, 0 };
            }
        } else if (previsionComplet.isRecette()) {
            MvtCaisse[] mvtCaisses = (MvtCaisse[]) CGenUtil.rechercher(new MvtCaisse(), null, null,
                    " AND IDPREVISION = '" + previsionComplet.getId() + "' ");
            if (mvtCaisses != null && mvtCaisses.length > 0) {
                double totalCredit = 0;
                for (MvtCaisse mvtCaisse : mvtCaisses) {
                    if (mvtCaisse.getEtat() >= 1) {
                        if (mvtCaisse.getIdOrigine().startsWith("FCF")) {
                            FactureFournisseur[] facture = (FactureFournisseur[]) CGenUtil.rechercher(
                                    new FactureFournisseur(), null, null,
                                    " AND ID = '" + mvtCaisse.getIdOrigine() + "' ");
                            if (facture != null && facture.length > 0 && facture[0].getEtat() >= 1)
                                totalCredit += mvtCaisse.getCredit();

                        } else if (mvtCaisse.getIdOrigine().startsWith("VNT")) {
                            Vente[] ventes = (Vente[]) CGenUtil.rechercher(new Vente(), null, null,
                                    " AND ID = '" + mvtCaisse.getIdOrigine() + "' ");
                            if (ventes != null && ventes.length > 0 && ventes[0].getEtat() >= 1)
                                totalCredit += mvtCaisse.getCredit();
                        } else if (mvtCaisse.getIdOrigine() == null) {
                            totalCredit += mvtCaisse.getCredit();
                        }
                    }
                }
                return new double[] { 0, totalCredit };
            }
        } else if (!previsionComplet.isDepense() && !previsionComplet.isRecette()) {
            MvtCaisse[] mvtCaisses = (MvtCaisse[]) CGenUtil.rechercher(new MvtCaisse(), null, null,
                    " AND IDPREVISION = '" + previsionComplet.getId() + "' ");
            double totalDebit = 0;
            double totalCredit = 0;
            if (mvtCaisses != null && mvtCaisses.length > 0) {
                for (MvtCaisse mvtCaisse : mvtCaisses) {
                    if (mvtCaisse.getEtat() >= 1) {
                        if (mvtCaisse.getIdOrigine() == null) {
                            totalDebit += mvtCaisse.getDebit();
                            totalCredit += mvtCaisse.getCredit();
                        } else if (mvtCaisse.getIdOrigine().startsWith("FCF")) {
                            FactureFournisseur[] facture = (FactureFournisseur[]) CGenUtil.rechercher(
                                    new FactureFournisseur(), null, null,
                                    " AND ID = '" + mvtCaisse.getIdOrigine() + "' ");
                            if (facture != null && facture.length > 0 && facture[0].getEtat() >= 1) {
                                totalDebit += mvtCaisse.getDebit();
                                totalCredit += mvtCaisse.getCredit();
                            }
                        } else if (mvtCaisse.getIdOrigine().startsWith("VNT")) {
                            Vente[] ventes = (Vente[]) CGenUtil.rechercher(new Vente(), null, null,
                                    " AND ID = '" + mvtCaisse.getIdOrigine() + "' ");
                            if (ventes != null && ventes.length > 0 && ventes[0].getEtat() >= 1) {
                                totalDebit += mvtCaisse.getDebit();
                                totalCredit += mvtCaisse.getCredit();
                            }
                        }
                    }
                }
            }
            return new double[] { totalDebit, totalCredit };
        }
        return new double[] { 0, 0 };
    }

    public Map<String, Object> getSoldeDebitCredit(String dateDebut, String dateFin) throws Exception {
        // Utiliser la date de début directement (pas -1 jour) pour correspondre à
        // AdminPrevision
        Date dateD = Utilitaire.string_date("dd/MM/yyyy", dateDebut);
        Date dateF = Utilitaire.string_date("dd/MM/yyyy", dateFin);

        String apresWhere = " AND DATY >= TO_DATE('" + Utilitaire.datetostring(dateD)
                + "','DD/MM/YYYY') AND DATY <= TO_DATE('"
                + Utilitaire.datetostring(dateF)
                + "','DD/MM/YYYY') ORDER BY DATY ASC";
        
        System.out.println("DEBUG getSoldeDebitCredit - Query: " + apresWhere);
        
        PrevisionComplet[] previsions = (PrevisionComplet[]) CGenUtil.rechercher(new PrevisionComplet(),
                null, null, apresWhere);
        
        System.out.println("DEBUG getSoldeDebitCredit - Nombre de previsions trouvées: " + 
                          (previsions != null ? previsions.length : 0));
        
        if (previsions != null && previsions.length > 0) {
            for (PrevisionComplet iterable_element : previsions) {
                System.out.println("Prevision ID: " + iterable_element.getId() + 
                                 ", Date: " + iterable_element.getDaty() +
                                 ", Recette: " + iterable_element.getRecetteEcart() +
                                 ", Depense: " + iterable_element.getDepenseEcart());
            }
        }
        
        if (previsions == null || previsions.length == 0) {
            throw new Exception("Aucun prevision trouvée pour cette periode");
        }
        AdminPrevision admin = new AdminPrevision();

        double soldeInitial = admin.getSoldeInitiale(null, dateDebut);
        double currentSolde = soldeInitial;

        // Grouper par date - équivalent SQL: GROUP BY daty
        Map<String, PrevisionJournalierLib> mapParDate = new HashMap<>();

        for (PrevisionComplet previsionComplet : previsions) {
            System.out.println(previsionComplet.getDebit());
            System.out.println(previsionComplet.getCredit());
            // Calculer les montants effectivement payés
            double[] soldeDebitCredit = calculeMontantPaye(previsionComplet);
            if (previsionComplet.isDepense()) {
                previsionComplet.setDepenseEcart(previsionComplet.getDepenseEcart() - soldeDebitCredit[0]);
            } else if (previsionComplet.isRecette()) {
                previsionComplet.setRecetteEcart(previsionComplet.getRecetteEcart() - soldeDebitCredit[1]);
            } else {
                previsionComplet.setDepenseEcart(previsionComplet.getDepenseEcart() - soldeDebitCredit[0]);
                previsionComplet.setRecetteEcart(previsionComplet.getRecetteEcart() - soldeDebitCredit[1]);
            }

            Date daty = previsionComplet.getDaty();
            String dateKey = daty.toString();

            PrevisionJournalierLib prevJour = mapParDate.get(dateKey);
            if (prevJour == null) {
                prevJour = new PrevisionJournalierLib();
                prevJour.setDaty(daty);
                prevJour.setRecettes(0);
                prevJour.setDepenses(0);
                mapParDate.put(dateKey, prevJour);
            }

            prevJour.setRecettes(prevJour.getRecettes() + previsionComplet.getRecetteEcart());
            prevJour.setDepenses(prevJour.getDepenses() + previsionComplet.getDepenseEcart());
        }

        List<PrevisionJournalierLib> liste = new ArrayList<>();
        Date currentDate = Utilitaire.string_date("dd/MM/yyyy",dateDebut);
        Date endDate = Utilitaire.string_date("dd/MM/yyyy",dateFin);

        // Calculer le solde cumulatif jour par jour
        while (!currentDate.after(endDate)) {
            String dateKey = currentDate.toString();
            PrevisionJournalierLib prevJour = mapParDate.get(dateKey);

            if (prevJour == null) {
                prevJour = new PrevisionJournalierLib();
                prevJour.setDaty(currentDate);
                prevJour.setRecettes(0);
                prevJour.setDepenses(0);
            }

            currentSolde += prevJour.getRecettes() - prevJour.getDepenses();
            prevJour.setReste(currentSolde);

            liste.add(prevJour);
            currentDate = new Date(currentDate.getTime() + 24 * 60 * 60 * 1000);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("soldeinitial", soldeInitial);
        result.put("soldefinal", currentSolde);
        result.put("previsions", liste.toArray(new PrevisionJournalierLib[0]));
        return result;
    }

    public Map<String, Object> getPrevisionsJournalier(String dateDebut, String dateFin) throws Exception {
        try {
            AdminPrevision admin = new AdminPrevision();
            double soldeInitial = admin.getSoldeInitiale(null, dateDebut);

            // Utiliser getSoldeDebitCredit pour obtenir les données groupées par date
            Map<String, Object> dataMap = getSoldeDebitCredit(dateDebut, dateFin);
            PrevisionJournalierLib[] previsions = (PrevisionJournalierLib[]) dataMap.get("previsions");

            if (previsions == null || previsions.length == 0) {
                throw new Exception("Aucun prevision trouvée pour cette periode");
            }

            // Calculer le solde cumulatif
            double currentSolde = soldeInitial;
            for (int i = 0; i < previsions.length; i++) {
                currentSolde += previsions[i].getRecettes() - previsions[i].getDepenses();
                previsions[i].setReste(currentSolde);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("soldeinitial", soldeInitial);
            map.put("soldefinal", currentSolde);
            map.put("previsions", previsions);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Erreur : " + e.getMessage());
        }
    }
}
