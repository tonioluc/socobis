package servlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import bean.ClassMAPTable;
import bean.ClassEtat;
import bean.ClassFille;
import constante.ConstanteEtat;
import servlet.login.LoginRestServlet;
import servlet.responseUtilitaire.UtilResponse;
import user.UserEJB;
import user.UserEJBClient;
import utilitaire.Utilitaire;
import affichage.PageConsulte;
import fabrication.Of;
import fabrication.OfFille;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet générique pour traiter les insertions/modifications multiples.
 * Equivalent REST de apresMultiple.jsp - fonctionne avec n'importe quelle
 * classe ClassMAPTable
 * 
 * Supporte les actions:
 * - insert: Insertion d'un objet mère avec ses filles
 * - insertSansControle: Insertion sans contrôle
 * - update: Mise à jour d'un objet
 * - updateInsert: Mise à jour ou insertion
 * - updateSansControle: Mise à jour sans contrôle
 * - insertFille: Insertion de filles uniquement
 * - delete: Suppression
 * - valider: Validation d'un objet
 * - annuler: Annulation
 * - dupliquer: Duplication
 * - interview: Pour les classes avec état
 * 
 * Format JSON attendu:
 * {
 * "acte": "insert",
 * "classe": "proforma.Proforma",
 * "classeFille": "proforma.ProformaDetails",
 * "colonneMere": "idProforma",
 * "nomTable": "PROFORMA_INSERT",
 * "nomTableFille": "PROFORMADETAILS_CPLIMAGE",
 * "bute": "vente/proforma/proforma-fiche.jsp",
 * "mere": { ... données de l'objet mère ... },
 * "filles": [ { ... }, { ... } ]
 * }
 */
@WebServlet(name = "ApresMultipleServlet", urlPatterns = {
        "/api/apresmultiple",
        "/api/apresmultiple/insert",
        "/api/apresmultiple/update",
        "/api/apresmultiple/delete",
        "/api/apresmultiple/valider",
        "/api/session/check",
        "/api/session/login",
        "api/sequence/max"
})
public class ApresMultipleServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        UtilResponse.addCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();
        if (path.contains("/api/session/login")) {
            LoginRestServlet.doLogin(request, response);
            return;
        }

        if (path.contains("/api/sequence/max")) {
            getMaxSequence(request, response);
            return;
        }

        try {
            traiterRequete(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            UtilResponse.sendError(response, e.getMessage());
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UtilResponse.addCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        UtilResponse.addCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI();
        if (path.contains("/api/session/check")) {
            UtilResponse.checkSession(request, response);
            return;
        } 
        // Pour les autres GET, on peut gérer ici si besoin
        UtilResponse.sendError(response, "Méthode non supportée");
    }

    private void getMaxSequence(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String jsonBody = sb.toString().trim();
        System.out.println("ApresMultipleServlet - JSON reçu pour sequence: " + jsonBody);

        String nomSequence = null;
        if (!jsonBody.isEmpty()) {
            try {
                JsonParser parser = new JsonParser();
                JsonObject json = parser.parse(jsonBody).getAsJsonObject();
                nomSequence = UtilResponse.getJsonString(json, "nomSequence", null);
            } catch (Exception e) {
                System.err.println("getMaxSequence - erreur parsing JSON: " + e.getMessage());
            }
        }

        // Fallback to request parameter if not provided in JSON
        if (nomSequence == null || nomSequence.isEmpty()) {
            nomSequence = request.getParameter("nomSequence");
        }

        // Default sequence name if still missing
        if (nomSequence == null || nomSequence.isEmpty()) {
            nomSequence = "GETSEQBONDELIVRAISON";
        }

        Map<String, Object> responseData = new HashMap<>();
        try {
            int maxSeq = Utilitaire.getMaxSeq(nomSequence);
            responseData.put("sequence", maxSeq);
            UtilResponse.sendSuccess(response, responseData, "Max sequence retrieved");
        } catch (Exception e) {
            System.err.println("getMaxSequence - erreur getMaxSeq: " + e.getMessage());
            e.printStackTrace();
            // Return a fallback sequence but indicate a warning
            responseData.put("sequence", 1);
            responseData.put("warning", "Erreur getMaxSeq: " + e.getMessage());
            UtilResponse.sendSuccess(response, responseData, "Default sequence returned due to error");
        }
    }

    /**
     * Traite la requête générique - équivalent de apresMultiple.jsp
     */
    private void traiterRequete(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        // Lire le corps JSON de la requête
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String jsonBody = sb.toString();
        System.out.println("ApresMultipleServlet - JSON reçu: " + jsonBody);

        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonBody).getAsJsonObject();

        // Paramètres obligatoires
        String acte = UtilResponse.getJsonString(json, "acte", "insert");
        String classe = UtilResponse.getJsonString(json, "classe", null);

        // Déterminer automatiquement la classe si elle n'est pas fournie
        if (classe == null || classe.isEmpty()) {
            classe = getClasseFromActe(acte);
            if (classe == null) {
                UtilResponse.sendError(response, "Paramètre 'classe' requis ou acte non supporté sans classe");
                return;
            }
        }

        // Paramètres optionnels
        String classeFille = UtilResponse.getJsonString(json, "classeFille", null);
        String colonneMere = UtilResponse.getJsonString(json, "colonneMere", null);
        String nomTable = UtilResponse.getJsonString(json, "nomTable", null);
        String nomTableFille = UtilResponse.getJsonString(json, "nomTableFille", null);
        String id = UtilResponse.getJsonString(json, "id", null);
        String bute = UtilResponse.getJsonString(json, "bute", null);

        HttpSession session = request.getSession();
        UserEJB u = (UserEJB) session.getAttribute("u");
        System.out.println(u == null ? "UserEJB is null in session" : "UserEJB found in session");
        if (u == null) {
            try {
                u = UserEJBClient.lookupUserEJBBeanLocal();
                System.out.println("Lookup successful: " + (u != null ? "EJB found" : "EJB null"));
            } catch (Exception e) {
                System.out.println("Lookup failed: " + e.getMessage());
                UtilResponse.sendError(response, "Erreur lors du lookup EJB: " + e.getMessage());
                return;
            }

            historique.MapUtilisateurServiceDirection user = (historique.MapUtilisateurServiceDirection) session
                    .getAttribute("user");
            System.out.println("User in session: " + (user != null ? "found" : "null"));

            if (user != null) {
                try {
                    u.setU(user);
                    System.out.println("User initialized in looked up EJB");
                } catch (Exception e) {
                    System.out.println("SetU failed: " + e.getMessage());
                    UtilResponse.sendError(response, "Erreur lors de l'initialisation utilisateur: " + e.getMessage());
                    return;
                }
            } else {
                // AUTO-LOGIN avec admin/paop si aucun utilisateur en session
                System.out.println("No user in session, doing auto-login with admin/paop");
                try {
                    u.testLogin("admin", "test", null, null);

                    // Stocker en session pour cohérence
                    session.setAttribute("u", u);
                    session.setAttribute("user", u.getU());
                    session.setAttribute("config", u.findConfiguration());

                    System.out.println("Auto-login successful for admin user");
                } catch (Exception e) {
                    System.out.println("Auto-login failed: " + e.getMessage());
                    UtilResponse.sendError(response, "Erreur lors de l'auto-login: " + e.getMessage());
                    return;
                }
            }
        }

        String idResultat = null;
        ClassMAPTable resultat = null;

        // ============ TRAITEMENT SELON L'ACTION ============

        // INSERT avec filles
        if ("insert".equalsIgnoreCase(acte)) {
            resultat = traiterInsert(json, classe, classeFille, nomTable, nomTableFille, colonneMere, u);
            if (resultat != null) {
                // Utiliser getTuppleID() qui fonctionne pour tous les types ClassMAPTable
                idResultat = resultat.getTuppleID();
            }
        }
        // INSERT SANS CONTROLE
        else if ("insertSansControle".equalsIgnoreCase(acte)) {
            resultat = traiterInsertSansControle(json, classe, classeFille, nomTable, nomTableFille, colonneMere, u);
            if (resultat != null) {
                // Utiliser getTuppleID() qui fonctionne pour tous les types ClassMAPTable
                idResultat = resultat.getTuppleID();
            }
        }
        // UPDATE INSERT (modification ou insertion)
        else if ("updateInsert".equalsIgnoreCase(acte) || "modifier".equalsIgnoreCase(acte)) {
            resultat = traiterUpdateInsert(json, classe, classeFille, nomTable, nomTableFille, colonneMere, u);
            if (resultat != null) {
                // Utiliser getTuppleID() qui fonctionne pour tous les types ClassMAPTable
                idResultat = resultat.getTuppleID();
            }
        }
        // UPDATE simple
        else if ("update".equalsIgnoreCase(acte)) {
            resultat = traiterUpdate(json, classe, nomTable, u);
            if (resultat != null) {
                // Pour les fabrications, s'assurer que l'ID retourné est l'ID de la fabrication, pas l'ID de l'ordre de fabrication
                if ("fabrication.Fabrication".equals(classe)) {
                    idResultat = ((fabrication.Fabrication) resultat).getId();
                } else {
                    idResultat = resultat.getTuppleID();
                }
            }
        }
        // UPDATE SANS CONTROLE
        else if ("updateSansControle".equalsIgnoreCase(acte)) {
            resultat = traiterUpdateSansControle(json, classe, classeFille, nomTable, nomTableFille, colonneMere, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // DELETE
        else if ("delete".equalsIgnoreCase(acte)) {
            traiterDelete(json, classe, nomTable, id, u);
            idResultat = id;
        }
        // VALIDER
        else if ("valider".equalsIgnoreCase(acte)) {
            resultat = traiterValider(json, classe, id, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // ANNULER
        else if ("annuler".equalsIgnoreCase(acte)) {
            traiterAnnuler(json, classe, id, u);
            idResultat = id;
        }
        // DUPLIQUER
        else if ("dupliquer".equalsIgnoreCase(acte)) {
            String classeFilleDup = UtilResponse.getJsonString(json, "nomClasseFille", null);
            String nomColonneMere = UtilResponse.getJsonString(json, "nomColonneMere", null);
            idResultat = traiterDupliquer(classe, id, classeFilleDup, nomColonneMere, u);
        }
        // INTERVIEW (pour les classes avec état)
        else if ("interview".equalsIgnoreCase(acte)) {
            resultat = traiterInterview(json, classe, classeFille, nomTable, nomTableFille, colonneMere, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // CONSULTE - Consultation d'un objet
        else if ("consulte".equalsIgnoreCase(acte)) {
            resultat = traiterConsulte(json, classe, nomTable, id, request, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
                Map<String, Object> objectData = extractAllFields(resultat);

                UtilResponse.sendSuccess(response, objectData, "Consultation réussie");
                return;
            }
        }
        // LISTE FILLES - Récupérer les filles d'un objet
        else if ("listeFilles".equalsIgnoreCase(acte)) {
            if (classeFille == null || colonneMere == null) {
                UtilResponse.sendError(response, "classeFille et colonneMere requis pour listeFilles");
                return;
            }

            List<Map<String, Object>> fillesData = traiterListeFilles(classeFille, nomTableFille, colonneMere, id, u);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("filles", fillesData);
            UtilResponse.sendSuccess(response, responseData, "Liste des filles récupérée");
            return;
        }
        // LISTE - Récupérer une liste d'objets avec filtres
        else if ("liste".equalsIgnoreCase(acte)) {
            List<Map<String, Object>> listeData = traiterListe(json, classe, nomTable, request, u);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("liste", listeData);
            UtilResponse.sendSuccess(response, responseData, "Liste récupérée avec " + listeData.size() + " résultats");
            return;
        }
        // GET FAB FILLE - Récupérer les composants d'un bon de commande
        else if ("getFabFille".equalsIgnoreCase(acte)) {
            String idBc = UtilResponse.getJsonString(json, "idBc", null);
            if (idBc == null) {
                UtilResponse.sendError(response, "idBc requis pour getFabFille");
                return;
            }
            try {
                vente.BonDeCommande comm = new vente.BonDeCommande();
                comm.setId(idBc);
                fabrication.FabricationFille[] fabfille = comm.getFabFille();
                List<Map<String, Object>> result = new ArrayList<>();
                if (fabfille != null) {
                    for (fabrication.FabricationFille f : fabfille) {
                        Map<String, Object> item = extractAllFields(f);
                        result.add(item);
                    }
                }
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("liste", result);
                UtilResponse.sendSuccess(response, responseData, "Composants récupérés");
            } catch (Exception e) {
                UtilResponse.sendError(response, "Erreur getFabFille: " + e.getMessage());
            }
            return;
        }
        // GENERER FABRICATION - Générer une fabrication à partir d'un ordre de fabrication
        else if ("genererFabrication".equalsIgnoreCase(acte)) {
            String idOffille = UtilResponse.getJsonString(json, "idOffille", null);
            if (idOffille == null) {
                UtilResponse.sendError(response, "idOffille requis pour genererFabrication");
                return;
            }
            try {
                fabrication.OfFille ofFille = new fabrication.OfFille();
                ofFille.setId(idOffille);
                fabrication.Fabrication fab = ofFille.genererFabrication(null);
                Map<String, Object> fabData = extractAllFields(fab);
                fabrication.FabricationFille[] filles = (fabrication.FabricationFille[]) fab.getFille();
                List<Map<String, Object>> composants = new ArrayList<>();
                if (filles != null) {
                    for (fabrication.FabricationFille f : filles) {
                        Map<String, Object> comp = extractAllFields(f);
                        composants.add(comp);
                    }
                }
                fabData.put("fille", composants);
                // Retourner l'idOffille au lieu de l'ID de la fabrication générée pour simplifier
                fabData.put("id", ofFille.getIdMere());
                fabData.put("idOffille", idOffille);
                UtilResponse.sendSuccess(response, fabData, "Fabrication générée");
            } catch (Exception e) {
                UtilResponse.sendError(response, "Erreur genererFabrication: " + e.getMessage());
            }
            return;
        }
        // GENERER FABRICATION UN PAR UN - Générer une fabrication un par un à partir d'un ordre de fabrication
        else if ("genererFabricationUnParUn".equalsIgnoreCase(acte)) {
            String idOffille = UtilResponse.getJsonString(json, "idOffille", null);
            if (idOffille == null) {
                UtilResponse.sendError(response, "idOffille requis pour genererFabricationUnParUn");
                return;
            }
            try {
                fabrication.OfFille ofFille = new fabrication.OfFille();
                ofFille.setId(idOffille);
                fabrication.Fabrication fab = ofFille.genererFabricationUnParUn(null);
                Map<String, Object> fabData = extractAllFields(fab);
                fabrication.FabricationFille[] filles = (fabrication.FabricationFille[]) fab.getFille();
                List<Map<String, Object>> composants = new ArrayList<>();
                if (filles != null) {
                    for (fabrication.FabricationFille f : filles) {
                        Map<String, Object> comp = extractAllFields(f);
                        composants.add(comp);
                    }
                }
                fabData.put("fille", composants);
                // Retourner l'idOffille au lieu de l'ID de la fabrication générée pour simplifier
                fabData.put("id", idOffille);
                fabData.put("idOffille", idOffille);
                UtilResponse.sendSuccess(response, fabData, "Fabrication un par un générée");
            } catch (Exception e) {
                UtilResponse.sendError(response, "Erreur genererFabricationUnParUn: " + e.getMessage());
            }
            return;
        }
        // GENERER MVT STOCK - Générer un mouvement de stock à partir d'une fabrication
        else if ("genererMvtStock".equalsIgnoreCase(acte)) {
            String idFab = UtilResponse.getJsonString(json, "id", null);
            String idTypeMvStock = UtilResponse.getJsonString(json, "idTypeMvStock", "TPMVST000001");
            String isResidu = UtilResponse.getJsonString(json, "isResidu", null);

            if (idFab == null) {
                UtilResponse.sendError(response, "id requis pour genererMvtStock");
                return;
            }
            try {
                fabrication.Fabrication fab = new fabrication.Fabrication();
                fab.setId(idFab);

                stock.MvtStock mvtStock = null;
                if ("true".equals(isResidu)) {
                    mvtStock = fab.genererMvtStock(null);
                } else {
                    mvtStock = fab.genererMvtStock(idTypeMvStock, null);
                }

                if (mvtStock != null) {
                    Map<String, Object> mvtData = extractAllFields(mvtStock);
                    List<Map<String, Object>> details = new ArrayList<>();
                    bean.ClassFille[] filles = mvtStock.getFille();
                    if (filles != null) {
                        for (bean.ClassFille f : filles) {
                            if (f instanceof stock.MvtStockFille) {
                                Map<String, Object> det = extractAllFields(f);
                                details.add(det);
                            }
                        }
                    }
                    mvtData.put("details", details);
                    UtilResponse.sendSuccess(response, mvtData, "Mouvement de stock généré");
                } else {
                    UtilResponse.sendError(response, "Impossible de générer le mouvement de stock");
                }
            } catch (Exception e) {
                UtilResponse.sendError(response, "Erreur genererMvtStock: " + e.getMessage());
            }
            return;
        } else {
            UtilResponse.sendError(response, "Action non supportée: " + acte);
            return;
        }

        // Construire la réponse
        Map<String, Object> data = new HashMap<>();
        data.put("id", idResultat);
        if (bute != null && !bute.isEmpty() && idResultat != null) {
            data.put("redirectUrl", bute + "?id=" + idResultat);
        }

        UtilResponse.sendSuccess(response, data, "Opération '" + acte + "' réussie");
    }

    // ============ METHODES DE TRAITEMENT ============

    /**
     * INSERT - Insertion d'un objet mère avec ses filles
     */
    private ClassMAPTable traiterInsert(JsonObject json, String classe, String classeFille,
            String nomTable, String nomTableFille, String colonneMere, UserEJB u) throws Exception {

        ClassMAPTable mere = (ClassMAPTable) Class.forName(classe).newInstance();
        // NE PAS appliquer nomTable à la mère - comme dans apresMultiple.jsp
        // Le nomTable de la mère est une vue pour l'affichage, pas pour l'insertion
        // if (nomTable != null) {
        // mere.setNomTable(nomTable);
        // }

        // Remplir l'objet mère
        JsonObject mereData = json.getAsJsonObject("mere");
        if (mereData != null) {
            UtilResponse.remplirObjetDepuisJson(mere, mereData);
        }

        // Vérifier si c'est un updateInsert (ID déjà existant)
        String idExistant = mere.getTuppleID();
        if (idExistant != null && !idExistant.isEmpty()) {
            return traiterUpdateInsert(json, classe, classeFille, nomTable, nomTableFille, colonneMere, u);
        }
        System.out.println(mere);
        // Si pas de filles, insertion simple
        if (classeFille == null || !json.has("filles")) {
            return (ClassMAPTable) u.createObject(mere);
        }

        // Vérifier que colonneMere est fourni pour les insertions avec filles
        if (colonneMere == null || colonneMere.isEmpty()) {
            throw new Exception("colonneMere requis pour les insertions avec filles");
        }

        // Insertion avec filles
        ClassFille[] cfille = creerTableauFilles(json, classeFille, nomTableFille);

        // Vérifications supplémentaires
        if (cfille == null) {
            throw new Exception("cfille est null");
        }
        for (int i = 0; i < cfille.length; i++) {
            if (cfille[i] == null) {
                throw new Exception("cfille[" + i + "] est null");
            }
        }
        System.out.println();

        System.out.println("mere " + mere);
        System.out.println("mere class: " + mere.getClass().getName() + ", cfill  e length: "
                + (cfille != null ? cfille.length : "null"));
        System.out.println("user EJB: " + (u == null ? "null" : "found" + u));
        return (ClassMAPTable) u.createObjectMultiple(mere, colonneMere, cfille);
    }

    /**
     * INSERT SANS CONTROLE
     */
    private ClassMAPTable traiterInsertSansControle(JsonObject json, String classe, String classeFille,
            String nomTable, String nomTableFille, String colonneMere, UserEJB u) throws Exception {

        ClassMAPTable mere = (ClassMAPTable) Class.forName(classe).newInstance();
        // NE PAS appliquer nomTable a la mere - comme dans apresMultiple.jsp
        // if (nomTable != null) {
        // mere.setNomTable(nomTable);
        // }

        JsonObject mereData = json.getAsJsonObject("mere");
        if (mereData != null) {
            UtilResponse.remplirObjetDepuisJson(mere, mereData);
        }

        if (classeFille == null || !json.has("filles")) {
            return (ClassMAPTable) u.createObject(mere);
        }

        // Vérifier que colonneMere est fourni pour les insertions avec filles
        if (colonneMere == null || colonneMere.isEmpty()) {
            throw new Exception("colonneMere requis pour les insertions avec filles");
        }

        ClassFille[] cfille = creerTableauFilles(json, classeFille, nomTableFille);

        // Vérifications supplémentaires
        if (cfille == null) {
            throw new Exception("cfille est null");
        }
        for (int i = 0; i < cfille.length; i++) {
            if (cfille[i] == null) {
                throw new Exception("cfille[" + i + "] est null");
            }
        }
        if (mere == null) {
            throw new Exception("mere est null");
        }

        // Log des paramètres pour debug
        System.out.println("ApresMultipleServlet - Insert avec filles:");
        System.out.println("  classe: " + classe);
        System.out.println("  classeFille: " + classeFille);
        System.out.println("  colonneMere: " + colonneMere);
        System.out.println("  nomTable: " + nomTable);
        System.out.println("  nomTableFille: " + nomTableFille);
        System.out.println("  mere: " + (mere != null ? mere.getClass().getName() : "null"));
        System.out.println("  cfille length: " + (cfille != null ? cfille.length : "null"));

        return (ClassMAPTable) u.createObjectMultiple(mere, colonneMere, cfille);
    }

    /**
     * UPDATE INSERT - Mise à jour ou insertion
     */
    private ClassMAPTable traiterUpdateInsert(JsonObject json, String classe, String classeFille,
            String nomTable, String nomTableFille, String colonneMere, UserEJB u) throws Exception {

        ClassMAPTable mere = (ClassMAPTable) Class.forName(classe).newInstance();
        // NE PAS appliquer nomTable a la mere - comme dans apresMultiple.jsp
        // if (nomTable != null) {
        // mere.setNomTable(nomTable);
        // }

        JsonObject mereData = json.getAsJsonObject("mere");
        if (mereData != null) {
            UtilResponse.remplirObjetDepuisJson(mere, mereData);
        }

        if (classeFille == null || !json.has("filles")) {
            u.updateObject(mere);
            return mere;
        }

        ClassFille[] cfille = creerTableauFilles(json, classeFille, nomTableFille);
        return (ClassMAPTable) u.updateObjectMultiple(mere, colonneMere, cfille);
    }

    /**
     * UPDATE simple
     */
    private ClassMAPTable traiterUpdate(JsonObject json, String classe, String nomTable, UserEJB u) throws Exception {

        ClassMAPTable obj = (ClassMAPTable) Class.forName(classe).newInstance();
        // NE PAS appliquer nomTable a la mere - comme dans apresMultiple.jsp
        // if (nomTable != null) {
        // obj.setNomTable(nomTable);
        // }

        JsonObject mereData = json.getAsJsonObject("mere");
        if (mereData != null) {
            UtilResponse.remplirObjetDepuisJson(obj, mereData);
        }

        u.updateObject(obj);
        return obj;
    }

    /**
     * UPDATE SANS CONTROLE
     */
    private ClassMAPTable traiterUpdateSansControle(JsonObject json, String classe, String classeFille,
            String nomTable, String nomTableFille, String colonneMere, UserEJB u) throws Exception {

        ClassMAPTable mere = (ClassMAPTable) Class.forName(classe).newInstance();
        // NE PAS appliquer nomTable a la mere - comme dans apresMultiple.jsp
        // if (nomTable != null) {
        // mere.setNomTable(nomTable);
        // }

        JsonObject mereData = json.getAsJsonObject("mere");
        if (mereData != null) {
            UtilResponse.remplirObjetDepuisJson(mere, mereData);
        }

        if (classeFille == null || !json.has("filles")) {
            u.updateObject(mere);
            return mere;
        }

        ClassFille[] cfille = creerTableauFilles(json, classeFille, nomTableFille);
        return (ClassMAPTable) u.updateObjectMultiple(mere, colonneMere, cfille);
    }

    /**
     * DELETE - Suppression
     */
    private void traiterDelete(JsonObject json, String classe, String nomTable, String id, UserEJB u) throws Exception {

        ClassMAPTable obj = (ClassMAPTable) Class.forName(classe).newInstance();
        obj.setValChamp(obj.getAttributIDName(), id);
        if (nomTable != null && !nomTable.isEmpty()) {
            obj.setNomTable(nomTable);
        }
        u.deleteObject(obj);
    }

    /**
     * VALIDER - Validation d'un objet
     */
    private ClassMAPTable traiterValider(JsonObject json, String classe, String id, UserEJB u) throws Exception {

        ClassMAPTable obj = (ClassMAPTable) Class.forName(classe).newInstance();
        obj.setValChamp(obj.getAttributIDName(), id);
        return (ClassMAPTable) u.validerObject(obj);
    }

    /**
     * ANNULER - Annulation
     */
    private void traiterAnnuler(JsonObject json, String classe, String id, UserEJB u) throws Exception {

        ClassMAPTable obj = (ClassMAPTable) Class.forName(classe).newInstance();
        obj.setValChamp(obj.getAttributIDName(), id);
        u.annulerObject(obj);
    }

    /**
     * DUPLIQUER - Duplication d'un objet
     */
    private String traiterDupliquer(String classe, String id, String classeFille, String nomColonneMere, UserEJB u)
            throws Exception {

        ClassMAPTable obj = (ClassMAPTable) Class.forName(classe).newInstance();
        obj.setValChamp(obj.getAttributIDName(), id);
        Object resultat = u.dupliquerObject(obj, classeFille, nomColonneMere);
        return resultat != null ? resultat.toString() : null;
    }

    /**
     * INTERVIEW - Pour les classes avec état (ClassEtat)
     */
    private ClassMAPTable traiterInterview(JsonObject json, String classe, String classeFille,
            String nomTable, String nomTableFille, String colonneMere, UserEJB u) throws Exception {

        ClassMAPTable mere = (ClassMAPTable) Class.forName(classe).newInstance();
        if (nomTable != null) {
            mere.setNomTable(nomTable);
        }

        JsonObject mereData = json.getAsJsonObject("mere");
        if (mereData != null) {
            UtilResponse.remplirObjetDepuisJson(mere, mereData);
        }

        // Définir l'état interviewé
        if (mere instanceof ClassEtat) {
            ((ClassEtat) mere).setEtat(ConstanteEtat.getEtatInterviewe());
        }

        if (classeFille == null || !json.has("filles")) {
            u.updateObject(mere);
            return mere;
        }

        ClassFille[] cfille = creerTableauFilles(json, classeFille, nomTableFille);
        return (ClassMAPTable) u.updateObjectMultiple(mere, colonneMere, cfille);
    }

    /**
     * CONSULTE - Consultation d'un objet
     */
    private ClassMAPTable traiterConsulte(JsonObject json, String classe, String nomTable, String id,
            HttpServletRequest request,
            UserEJB u) throws Exception {
        if (id == null || id.isEmpty()) {
            throw new Exception("ID requis pour la consultation");
        }

        ClassMAPTable objet = (ClassMAPTable) Class.forName(classe).newInstance();
        if (nomTable != null && !nomTable.isEmpty())
            objet.setNomTable(nomTable);
        // Créer un wrapper de requête qui contient le paramètre id
        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                if ("id".equals(name)) {
                    return id;
                }
                return super.getParameter(name);
            }
        };

        // Utiliser PageConsulte pour récupérer l'objet comme dans le JSP
        PageConsulte pc = new PageConsulte(objet, wrappedRequest, u);
        return pc.getBase();
    }

    /**
     * LISTE FILLES - Récupérer les filles d'un objet
     */
    private List<Map<String, Object>> traiterListeFilles(String classeFille, String nomTableFille,
            String colonneMere, String idMere, UserEJB u) throws Exception {

        if (idMere == null || idMere.isEmpty()) {
            throw new Exception("ID de la mère requis pour listeFilles");
        }

        if (colonneMere == null || colonneMere.isEmpty()) {
            throw new Exception("Colonne mère requise pour listeFilles");
        }

        System.out.println("traiterListeFilles - classeFille: " + classeFille + ", colonneMere: " + colonneMere
                + ", idMere: " + idMere);

        // Créer un objet fille pour la recherche
        ClassFille fille = (ClassFille) Class.forName(classeFille).newInstance();
        if (nomTableFille != null) {
            fille.setNomTable(nomTableFille);
        }

        // Récupérer la liste des filles avec filtrage
        String requete = "SELECT * FROM " + nomTableFille + " WHERE " + colonneMere + " = '" + idMere + "'";
        System.out.println("Requête SQL: " + requete);
        // Utiliser un lookup local pour éviter les blocages sur le UserEJB stateful
        // stocké en session
        UserEJB localU = UserEJBClient.lookupUserEJBBeanLocal();
        Object[] filles = localU.getData(fille, requete, null);

        System.out.println("Résultat getData: " + (filles != null ? filles.length + " éléments" : "null"));

        // Convertir en liste de Map en utilisant extractAllFields (réutilisation)
        List<Map<String, Object>> result = new ArrayList<>();
        if (filles != null) {
            // Si c'est OfFille, calculer les revients comme dans le JSP
            if (classeFille.equals("fabrication.OfFilleCpl") || classeFille.equals("fabrication.OfFille")) {
                OfFille[] listeFille = new OfFille[filles.length];
                for (int i = 0; i < filles.length; i++) {
                    listeFille[i] = (OfFille) filles[i];
                }
                if (listeFille.length > 0) {
                    // Charger l'ordre principal comme dans le JSP
                    Of o = new Of();
                    o.setId(idMere);
                    o.setFille(listeFille);
                    o.calculerRevient(null);
                }
                // Maintenant extraire les champs après calcul
                for (OfFille obj : listeFille) {
                    Map<String, Object> filleData = extractAllFields(obj);
                    result.add(filleData);
                }
            } else {
                // Comportement normal pour les autres classes
                for (Object obj : filles) {
                    Map<String, Object> filleData = extractAllFields(obj);
                    result.add(filleData);
                }
            }
        }

        return result;
    }

    /**
     * LISTE - Récupérer une liste d'objets avec filtres
     * Utilise directement u.getData() et récupère tous les champs via réflexion
     */
    private List<Map<String, Object>> traiterListe(JsonObject json, String classe, String nomTable,
            HttpServletRequest request, UserEJB u) throws Exception {

        System.out.println("traiterListe - classe: " + classe + ", nomTable: " + nomTable);

        // Créer l'objet de base
        ClassMAPTable obj = (ClassMAPTable) Class.forName(classe).newInstance();
        if (nomTable != null) {
            obj.setNomTable(nomTable);
        }

        // Construire la requête WHERE à partir des critères JSON
        StringBuilder whereClause = new StringBuilder();

        // Critères texte (LIKE)
        String[] criteresTexte = { "id", "designation", "idClientLib", "libelle", "lancepar", "cible", "remarque", "lanceparLib", "idOf", "idOffille" };
        for (String crit : criteresTexte) {
            String val = UtilResponse.getJsonString(json, crit, null);
            if (val != null && !val.isEmpty()) {
                whereClause.append(" and UPPER(").append(crit).append(") LIKE UPPER('%").append(val).append("%')");
            }
        }

        // Critère idMere (pour les filles) - égalité exacte
        String idMere = UtilResponse.getJsonString(json, "idMere", null);
        String colonneMereFiltre = UtilResponse.getJsonString(json, "colonneMereFiltre", "idMere");
        if (idMere != null && !idMere.isEmpty()) {
            whereClause.append(" and ").append(colonneMereFiltre).append(" = '").append(idMere).append("'");
            System.out.println("Filtre " + colonneMereFiltre + " appliqué: " + idMere);
        }

        // Critères date (interval)
        String[] criteresDate = { "daty", "datyprevu" };
        for (String crit : criteresDate) {
            String val1 = UtilResponse.getJsonString(json, crit + "1", null);
            String val2 = UtilResponse.getJsonString(json, crit + "2", null);
            if (val1 != null && !val1.isEmpty()) {
                whereClause.append(" and ").append(crit).append(" >= TO_DATE('").append(val1)
                        .append("', 'YYYY-MM-DD')");
            }
            if (val2 != null && !val2.isEmpty()) {
                whereClause.append(" and ").append(crit).append(" <= TO_DATE('").append(val2)
                        .append("', 'YYYY-MM-DD')");
            }
        }

        // Critère état
        String etat = UtilResponse.getJsonString(json, "etat", null);
        if (etat != null && !etat.isEmpty()) {
            whereClause.append(" and etat = ").append(etat);
        }

        // Clause WHERE personnalisée
        String aWhere = UtilResponse.getJsonString(json, "aWhere", null);
        if (aWhere != null && !aWhere.isEmpty()) {
            whereClause.append(aWhere);
        }

        System.out.println("WHERE clause: " + whereClause.toString());

        // Construire et exécuter la requête
        String tableName = (nomTable != null) ? nomTable : obj.getNomTable();
        // Déterminer colonne d'ordre en vérifiant réellement les colonnes de la table
        String orderBy = " ORDER BY id DESC";
        java.sql.Connection metaConn = null;
        java.sql.Statement metaSt = null;
        java.sql.ResultSet metaRs = null;
        try {
            metaConn = new utilitaire.UtilDB().GetConn();
            metaSt = metaConn.createStatement();
            // aucune ligne retournée, juste pour récupérer les métadonnées
            metaRs = metaSt.executeQuery("SELECT * FROM " + tableName + " WHERE 1=0");
            java.sql.ResultSetMetaData md = metaRs.getMetaData();
            boolean hasDaty = false;
            boolean hasId = false;
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String col = md.getColumnName(i);
                if ("DATY".equalsIgnoreCase(col)) {
                    hasDaty = true;
                }
                if ("ID".equalsIgnoreCase(col)) {
                    hasId = true;
                }
            }
            if (hasDaty) {
                orderBy = " ORDER BY daty DESC";
            } else if (hasId) {
                orderBy = " ORDER BY id DESC";
            } else {
                // fallback: use first column name
                if (md.getColumnCount() >= 1) {
                    orderBy = " ORDER BY " + md.getColumnName(1) + " DESC";
                }
            }
        } catch (Exception e) {
            // Si on ne peut pas obtenir les métadonnées, on garde ORDER BY id DESC
        } finally {
            try {
                if (metaRs != null)
                    metaRs.close();
            } catch (Exception e) {
            }
            try {
                if (metaSt != null)
                    metaSt.close();
            } catch (Exception e) {
            }
            try {
                if (metaConn != null)
                    metaConn.close();
            } catch (Exception e) {
            }
        }
        String requete = "SELECT * FROM " + tableName + " WHERE 1=1 " + whereClause.toString() + orderBy;
        System.out.println("Requête SQL: " + requete);

        // Utiliser un lookup local pour les opérations de lecture afin d'éviter la
        // concurrence
        UserEJB localU = UserEJBClient.lookupUserEJBBeanLocal();
        Object[] objets = localU.getData(obj, requete, null);

        System.out.println("Résultat getData: " + (objets != null ? objets.length + " éléments" : "null"));

        // Convertir en liste de Map pour la réponse JSON
        List<Map<String, Object>> result = new ArrayList<>();
        if (objets != null) {
            for (Object item : objets) {
                ClassMAPTable o = (ClassMAPTable) item;
                Map<String, Object> itemData = extractAllFields(o);
                result.add(itemData);
            }
        }

        return result;
    }

    /**
     * Extrait TOUS les champs d'un objet via réflexion (incluant les classes
     * parentes)
     */
    private Map<String, Object> extractAllFields(Object obj) {
        Map<String, Object> data = new HashMap<>();

        // Parcourir toute la hiérarchie de classes
        Class<?> currentClass = obj.getClass();
        while (currentClass != null && currentClass != Object.class) {
            // Récupérer les champs déclarés de la classe courante
            java.lang.reflect.Field[] fields = currentClass.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value != null) {
                        // Ne pas inclure certains champs internes
                        String fieldName = field.getName();
                        if (!fieldName.equals("serialVersionUID") &&
                                !fieldName.equals("connexion") &&
                                !fieldName.equals("rs") &&
                                !fieldName.startsWith("this$")) {
                            data.put(fieldName, value);
                        }
                    }
                } catch (Exception e) {
                    // Ignorer les champs non accessibles
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // Ajouter aussi les valeurs via getValChamp si disponible (pour les champs
        // calculés)
        try {
            java.lang.reflect.Method getValChamp = obj.getClass().getMethod("getValChamp", String.class);
            String[] champsSup = { "id", "daty", "designation", "idClient", "idClientLib", "idclientLib",
                    "etat", "etatlib", "etatLib", "idDevise", "montant", "montanttotal",
                    "montanttva", "montantttc", "montantpaye", "montantreste", "montantRemise",
                    "datyprevu", "etatlogistiquelib", "remise", "remarque", "idMagasinLib",
                    "telephone", "avoir", "periode" };
            for (String fieldName : champsSup) {
                if (!data.containsKey(fieldName)) {
                    try {
                        Object value = getValChamp.invoke(obj, fieldName);
                        if (value != null) {
                            data.put(fieldName, value);
                        }
                    } catch (Exception e) {
                        // Champ non trouvé, continuer
                    }
                }
            }
        } catch (Exception e) {
            // Méthode getValChamp non disponible
        }

        // S'assurer que l'ID est présent
        if (!data.containsKey("id") && obj instanceof ClassMAPTable) {
            String id = ((ClassMAPTable) obj).getTuppleID();
            if (id != null) {
                data.put("id", id);
            }
        }

        return data;
    }

    // ============ METHODES UTILITAIRES ============

    /**
     * Crée un tableau d'objets filles depuis le JSON
     */
    private ClassFille[] creerTableauFilles(JsonObject json, String classeFille, String nomTableFille)
            throws Exception {
        JsonArray fillesData = json.getAsJsonArray("filles");
        if (fillesData == null || fillesData.size() == 0) {
            // Créer un tableau du type spécifique même s'il est vide
            Class<?> filleClass = Class.forName(classeFille);
            return (ClassFille[]) java.lang.reflect.Array.newInstance(filleClass, 0);
        }
        
        // Créer un tableau du type spécifique (FabricationFille[], OfFille[], etc.)
        Class<?> filleClass = Class.forName(classeFille);
        ClassFille[] cfille = (ClassFille[]) java.lang.reflect.Array.newInstance(filleClass, fillesData.size());
        
        for (int i = 0; i < fillesData.size(); i++) {
            cfille[i] = (ClassFille) filleClass.newInstance();
            if (nomTableFille != null) {
                cfille[i].setNomTable(nomTableFille);
            }
            JsonObject filleData = fillesData.get(i).getAsJsonObject();
            UtilResponse.remplirObjetDepuisJson(cfille[i], filleData);
        }

        return cfille;
    }

    /**
     * Crée un tableau de ClassMAPTable depuis le JSON (pour les classes qui
     * n'héritent pas de ClassFille)
     * Utilisé notamment pour Check qui hérite de ClassEtat
     */
    private ClassMAPTable[] creerTableauFilleClassMAPTable(JsonObject json, String classeFille, String nomTableFille)
            throws Exception {
        JsonArray fillesData = json.getAsJsonArray("filles");
        if (fillesData == null || fillesData.size() == 0) {
            return new ClassMAPTable[0];
        }

        ClassMAPTable[] cfille = new ClassMAPTable[fillesData.size()];
        for (int i = 0; i < fillesData.size(); i++) {
            cfille[i] = (ClassMAPTable) Class.forName(classeFille).newInstance();
            if (nomTableFille != null) {
                cfille[i].setNomTable(nomTableFille);
            }
            JsonObject filleData = fillesData.get(i).getAsJsonObject();
            UtilResponse.remplirObjetDepuisJson(cfille[i], filleData);
        }
        return cfille;
    }

    /**
     * Détermine automatiquement la classe basée sur l'acte
     * Pour les actes qui n'ont pas besoin du paramètre classe
     */
    private String getClasseFromActe(String acte) {
        switch (acte.toLowerCase()) {
            case "genererfabrication":
            case "genererfabricationunparun":
                return "fabrication.OfFille";
            case "liste":
            case "consulte":
            case "listefilles":
            case "getfabfille":
                // Ces actes peuvent être génériques, mais pour l'instant on retourne null
                // pour forcer l'envoi du paramètre classe
                return null;
            default:
                return null; // Classe doit être fournie explicitement
        }
    }
}
