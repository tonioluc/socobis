/*
 * Classe utilitaire pour gérer les devises depuis un fichier JSON
 * Créé le : 02/12/2024
 */
package prevision;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;

/**
 * Classe pour gérer les devises depuis un fichier JSON
 * @author Lapatia
 */
public class DeviseJson {
    
    private String code;
    private String nom;
    private double taux;
    private String dateDebut;
    private String dateFin;
    
    // Chemin par défaut du fichier JSON
    private static final String JSON_FILE_PATH = "assets/data/devises.json";
    
    public DeviseJson() {
    }
    
    public DeviseJson(String code, String nom, double taux, String dateDebut, String dateFin) {
        this.code = code;
        this.nom = nom;
        this.taux = taux;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }
    
    /**
     * Lit le fichier JSON et retourne la liste des devises
     * @param basePath Chemin de base du webapp (ex: servletContext.getRealPath("/"))
     * @return Liste des devises
     */
    public static List<DeviseJson> getAllDevises(String basePath) {
        List<DeviseJson> devises = new ArrayList<>();
        try {
            // Normaliser le chemin
            if (basePath == null) basePath = "";
            basePath = basePath.replace("\\", "/");
            if (!basePath.endsWith("/")) basePath += "/";
            
            String fullPath = basePath + JSON_FILE_PATH;
            System.out.println("DeviseJson - Lecture du fichier: " + fullPath);
            
            String content = new String(Files.readAllBytes(Paths.get(fullPath)), "UTF-8");
            devises = parseJsonArray(content);
            System.out.println("DeviseJson - Nombre de devises chargées: " + devises.size());
        } catch (Exception e) {
            System.out.println("DeviseJson - Erreur lors de la lecture: " + e.getMessage());
            e.printStackTrace();
        }
        return devises;
    }
    
    /**
     * Retourne les devises valides pour une date donnée (devises uniques par code)
     * @param basePath Chemin de base du webapp
     * @param dateStr Date au format yyyy-MM-dd ou dd/MM/yyyy
     * @return Liste des devises valides pour la date
     */
    public static List<DeviseJson> getDevisesValides(String basePath, String dateStr) {
        List<DeviseJson> toutes = getAllDevises(basePath);
        List<DeviseJson> valides = new ArrayList<>();
        List<String> codesAjoutes = new ArrayList<>();
        
        try {
            // Supporter les deux formats de date
            SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdfFR = new SimpleDateFormat("dd/MM/yyyy");
            java.util.Date dateRecherche;
            
            // Détecter le format de la date
            if (dateStr.contains("/")) {
                dateRecherche = sdfFR.parse(dateStr);
            } else {
                dateRecherche = sdfISO.parse(dateStr);
            }
            
            for (DeviseJson devise : toutes) {
                java.util.Date debut = sdfISO.parse(devise.getDateDebut());
                java.util.Date fin = sdfISO.parse(devise.getDateFin());
                
                // Vérifier si la date est dans la plage et si le code n'est pas déjà ajouté
                if (!dateRecherche.before(debut) && !dateRecherche.after(fin)) {
                    if (!codesAjoutes.contains(devise.getCode())) {
                        valides.add(devise);
                        codesAjoutes.add(devise.getCode());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // En cas d'erreur, retourner toutes les devises uniques
            for (DeviseJson devise : toutes) {
                if (!codesAjoutes.contains(devise.getCode())) {
                    valides.add(devise);
                    codesAjoutes.add(devise.getCode());
                }
            }
        }
        return valides;
    }
    
    /**
     * Retourne le taux d'une devise pour une date donnée
     * @param basePath Chemin de base du webapp
     * @param code Code de la devise (ex: USD)
     * @param dateStr Date au format yyyy-MM-dd ou dd/MM/yyyy
     * @return Le taux de la devise ou 1.0 si non trouvé
     */
    public static double getTauxByCodeAndDate(String basePath, String code, String dateStr) {
        List<DeviseJson> toutes = getAllDevises(basePath);
        
        try {
            // Supporter les deux formats de date
            SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdfFR = new SimpleDateFormat("dd/MM/yyyy");
            java.util.Date dateRecherche;
            
            // Détecter le format de la date
            if (dateStr.contains("/")) {
                dateRecherche = sdfFR.parse(dateStr);
            } else {
                dateRecherche = sdfISO.parse(dateStr);
            }
            
            for (DeviseJson devise : toutes) {
                if (devise.getCode().equals(code)) {
                    java.util.Date debut = sdfISO.parse(devise.getDateDebut());
                    java.util.Date fin = sdfISO.parse(devise.getDateFin());
                    
                    if (!dateRecherche.before(debut) && !dateRecherche.after(fin)) {
                        return devise.getTaux();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1.0; // Taux par défaut
    }
    
    /**
     * Génère le HTML pour une liste déroulante des devises
     * @param basePath Chemin de base du webapp
     * @param dateStr Date pour filtrer les devises valides
     * @param selectedCode Code de la devise sélectionnée par défaut
     * @return HTML de la liste déroulante
     */
    public static String getSelectHtml(String basePath, String dateStr, String selectedCode) {
        StringBuilder sb = new StringBuilder();
        List<DeviseJson> devises = getDevisesValides(basePath, dateStr);
        
        for (DeviseJson devise : devises) {
            String selected = devise.getCode().equals(selectedCode) ? " selected" : "";
            sb.append("<option value=\"").append(devise.getCode()).append("\"")
              .append(" data-taux=\"").append(devise.getTaux()).append("\"")
              .append(selected).append(">")
              .append(devise.getCode()).append(" - ").append(devise.getNom())
              .append("</option>\n");
        }
        return sb.toString();
    }
    
    /**
     * Génère un tableau JSON des devises pour JavaScript
     * @param basePath Chemin de base du webapp
     * @return String JSON des devises
     */
    public static String getDevisesJsonForJs(String basePath) {
        List<DeviseJson> devises = getAllDevises(basePath);
        StringBuilder sb = new StringBuilder("[");
        
        for (int i = 0; i < devises.size(); i++) {
            DeviseJson d = devises.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"code\":\"").append(d.getCode()).append("\",");
            sb.append("\"nom\":\"").append(d.getNom()).append("\",");
            sb.append("\"taux\":").append(d.getTaux()).append(",");
            sb.append("\"dateDebut\":\"").append(d.getDateDebut()).append("\",");
            sb.append("\"dateFin\":\"").append(d.getDateFin()).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Parse une chaîne JSON pour extraire la liste des devises
     * Parser JSON simple sans dépendance externe
     */
    private static List<DeviseJson> parseJsonArray(String json) {
        List<DeviseJson> devises = new ArrayList<>();
        
        // Nettoyer et parser le JSON manuellement
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);
        
        // Séparer les objets
        int braceCount = 0;
        int start = 0;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (braceCount == 0) start = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    String objStr = json.substring(start, i + 1);
                    DeviseJson devise = parseJsonObject(objStr);
                    if (devise != null) {
                        devises.add(devise);
                    }
                }
            }
        }
        
        return devises;
    }
    
    /**
     * Parse un objet JSON individuel
     */
    private static DeviseJson parseJsonObject(String json) {
        try {
            String code = extractJsonValue(json, "code");
            String nom = extractJsonValue(json, "nom");
            String tauxStr = extractJsonValue(json, "taux");
            String dateDebut = extractJsonValue(json, "dateDebut");
            String dateFin = extractJsonValue(json, "dateFin");
            
            double taux = Double.parseDouble(tauxStr);
            
            return new DeviseJson(code, nom, taux, dateDebut, dateFin);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Extrait une valeur d'un objet JSON par clé
     */
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "";
        
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) return "";
        
        char startChar = json.charAt(valueStart);
        
        if (startChar == '"') {
            // Valeur chaîne
            int valueEnd = json.indexOf("\"", valueStart + 1);
            return json.substring(valueStart + 1, valueEnd);
        } else {
            // Valeur numérique ou autre
            int valueEnd = valueStart;
            while (valueEnd < json.length() && 
                   json.charAt(valueEnd) != ',' && 
                   json.charAt(valueEnd) != '}' &&
                   json.charAt(valueEnd) != ']') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }
    
    // Getters et Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public double getTaux() {
        return taux;
    }

    public void setTaux(double taux) {
        this.taux = taux;
    }

    public String getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(String dateDebut) {
        this.dateDebut = dateDebut;
    }

    public String getDateFin() {
        return dateFin;
    }

    public void setDateFin(String dateFin) {
        this.dateFin = dateFin;
    }
    
    @Override
    public String toString() {
        return code + " - " + nom + " (Taux: " + taux + ")";
    }
        
    /**
     * Valide que la date de prévision est dans la fourchette de la devise
     * @param basePath Chemin de base du webapp
     * @param deviseValue Valeur au format "CODE|dateDebut|dateFin"
     * @param datePrevision Date de la prévision au format yyyy-MM-dd ou dd/MM/yyyy
     * @throws Exception si la date n'est pas dans la fourchette
     */
    public static void validerDatePrevision(String basePath, String deviseValue, String datePrevision) throws Exception {
        if (deviseValue == null || deviseValue.isEmpty()) {
            throw new Exception("La devise est obligatoire");
        }
        
        String[] parts = deviseValue.split("\\|");
        if (parts.length < 3) {
            // Format ancien (juste le code), pas de validation de date
            return;
        }
        
        String codeDevise = parts[0];
        String dateDebutDevise = parts[1];
        String dateFinDevise = parts[2];
        
        // Parser les dates
        SimpleDateFormat sdfISO = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfFR = new SimpleDateFormat("dd/MM/yyyy");
        
        java.util.Date datePrev;
        if (datePrevision.contains("/")) {
            datePrev = sdfFR.parse(datePrevision);
        } else {
            datePrev = sdfISO.parse(datePrevision);
        }
        
        java.util.Date dateDebut = sdfISO.parse(dateDebutDevise);
        java.util.Date dateFin = sdfISO.parse(dateFinDevise);
        
        // Vérifier que la date de prévision est dans la fourchette
        if (datePrev.before(dateDebut) || datePrev.after(dateFin)) {
            throw new Exception("La date de la pr\u00e9vision (" + datePrevision + ") n'est pas dans la p\u00e9riode de validit\u00e9 de la devise " + codeDevise + " (" + dateDebutDevise + " au " + dateFinDevise + ")");
        }
    }
    
    /**
     * Extrait le code devise depuis la valeur composite "CODE|dateDebut|dateFin"
     * @param deviseValue Valeur composite
     * @return Le code devise seul
     */
    public static String extractCodeFromValue(String deviseValue) {
        if (deviseValue == null || deviseValue.isEmpty()) {
            return "AR";
        }
        String[] parts = deviseValue.split("\\|");
        return parts[0];
    }
    
    /**
     * Récupère le taux depuis la valeur composite et la date
     * @param basePath Chemin de base du webapp
     * @param deviseValue Valeur au format "CODE|dateDebut|dateFin"
     * @param datePrevision Date de la prévision
     * @return Le taux de la devise
     */
    public static double getTauxFromValue(String basePath, String deviseValue, String datePrevision) {
        String code = extractCodeFromValue(deviseValue);
        return getTauxByCodeAndDate(basePath, code, datePrevision);
    }
}
