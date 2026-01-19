package vente;

public class VenteClientRetourLib extends VenteRetourClient{
    private  String idClientLib;
    private String idMagasinLib;
    private double montantRetour;
    private double montantTTC;
    private String idDevise;
    private String etatLib;

    public VenteClientRetourLib(){
        this.setNomTable("VenteRetourLib");
    }

    public String getIdClientLib() {
        return idClientLib;
    }

    public void setIdClientLib(String idClientLib) {
        this.idClientLib = idClientLib;
    }

    public String getIdMagasinLib() {
        return idMagasinLib;
    }

    public void setIdMagasinLib(String idMagasinLib) {
        this.idMagasinLib = idMagasinLib;
    }

    public double getMontantRetour() {
        return montantRetour;
    }

    public void setMontantRetour(double montantRetour) {
        this.montantRetour = montantRetour;
    }

    public String getIdDevise() {
        return idDevise;
    }

    public void setIdDevise(String idDevise) {
        this.idDevise = idDevise;
    }

    public String getEtatLib() {
        return etatLib;
    }

    public void setEtatLib(String etatLib) {
        this.etatLib = etatLib;
    }
}
