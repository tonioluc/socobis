package change;

import java.sql.Connection;
import java.sql.Timestamp;

import bean.CGenUtil;
import bean.ClassMAPTable;
import utilitaire.UtilDB;
import utilitaire.Utilitaire;

public class TauxDeChange extends ClassMAPTable {
    String id;
    String idDevise;
    double taux;
    java.sql.Date daty;

    public TauxDeChange() {
        super.setNomTable("tauxdechange");
    }

    @Override
    public void construirePK(Connection c) throws Exception {
        this.preparePk("TX", "GETSEQTAUXDECHANGE");
        this.setId(makePK(c));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdDevise() {
        return idDevise;
    }

    public void setIdDevise(String idDevise) {
        this.idDevise = idDevise;
    }

    public double getTaux() {

        return taux;
    }

    public void setTaux(double taux) throws Exception {
        if (this.getMode().compareTo("modif") == 0) {
            if (taux <= 0)
                throw new Exception("taux non valide");
        }
        this.taux = taux;
    }

    public java.sql.Date getDaty() {
        return daty;
    }

    public void setDaty(java.sql.Date daty) throws Exception {
        if (this.getMode().compareTo("modif") == 0) {
            if (daty == null)
                throw new Exception("Date obligatoire");
        }
        this.daty = daty;
    }

    @Override
    public String getAttributIDName() {
        // TODO Auto-generated method stub
        return "id";
    }

    @Override
    public String getTuppleID() {
        // TODO Auto-generated method stub
        return this.id;
    }

    @Override
    public void controler(Connection c) throws Exception {
        // Raha efa misy meme date aminy de meme devise de mthrows Exception
        TauxDeChange tc = new TauxDeChange();
        tc.setIdDevise(this.getIdDevise());
        TauxDeChange[] tableTaux = (TauxDeChange[]) CGenUtil.rechercher(tc, null, null, c,
                " and daty ='" + utilitaire.Utilitaire.formatterDaty(this.getDaty()) + "'");
        if (tableTaux.length > 0) {
            throw new Exception("Taux de change déja existant pour cette date et devise");
        }
    }

    public static double getLastTaux(Connection c, String daty, String iddevise) throws Exception {
        boolean estOuvert = false;
        Connection conn = c; // Utiliser une variable locale

        try {
            if (conn == null) {
                conn = new UtilDB().GetConn();
                estOuvert = true;
            }

            if (daty == null || daty.isEmpty())
                daty = Utilitaire.dateDuJour();
            String req = "select *\n" +
                    "from TAUXDECHANGE t1\n" +
                    "where t1.DATY = (select max(t2.daty) from TAUXDECHANGE t2 where t2.IDDEVISE = t1.IDDEVISE and t2.daty <= '"
                    + daty + "')\n" +
                    "  and t1.IDDEVISE = '" + iddevise + "' ";

            TauxDeChange[] taux = (TauxDeChange[]) CGenUtil.rechercher(new TauxDeChange(), req, conn);
            return taux.length > 0 ? taux[0].getTaux() : 1;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            // NE PAS FERMER SI LA CONNEXION ÉTAIT PASSÉE EN PARAMÈTRE
            if (estOuvert && conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    // Ignorer les erreurs de fermeture
                }
            }
        }
    }

}
