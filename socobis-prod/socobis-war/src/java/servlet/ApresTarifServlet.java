package servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import bean.ClassMAPTable;
import bean.ClassFille;
import constante.ConstanteEtat;
import servlet.responseUtilitaire.UtilResponse;
import user.UserEJB;
import user.UserEJBClient;
import affichage.PageInsert;
import affichage.Page;
import produits.Ingredients;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet REST équivalent de apresTarif.jsp
 * Gère les opérations de type Tarif: insert, update, delete, valider, annuler, etc.
 * 
 * Supporte les actions:
 * - insert: Insertion simple d'un objet
 * - insertWithAction: Insertion avec action personnalisée
 * - insertUser: Insertion d'un utilisateur
 * - update: Mise à jour d'un objet
 * - updatevalider: Mise à jour puis validation
 * - delete: Suppression d'un objet
 * - deleteFille: Suppression d'un objet fille
 * - valider: Validation d'un objet
 * - annuler: Annulation d'un objet
 * - annulerVisa: Annuler un visa
 * - savevalider: Sauvegarde puis validation
 * - cloturer: Clôturer un objet
 * - payer: Payer un objet
 * - finaliser: Finaliser un objet
 * - dupliquer: Dupliquer un objet
 * - custom: Changement d'état personnalisé
 * - disponible: Marquer un produit disponible/indisponible
 * - insertMereLierFille: Insérer une mère et lier des filles
 * 
 * Format JSON attendu:
 * {
 *   "acte": "insert",
 *   "classe": "vente.Vente",
 *   "nomtable": "VENTE_CPL",
 *   "id": "123",
 *   "bute": "vente/vente-fiche.jsp",
 *   "data": { ... données de l'objet ... }
 * }
 */
@WebServlet(name = "ApresTarifServlet", urlPatterns = {
    "/api/aprestarif",
    "/api/tarif/insert",
    "/api/tarif/update",
    "/api/tarif/delete",
    "/api/tarif/valider",
    "/api/tarif/annuler"
})
public class ApresTarifServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        UtilResponse.addCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

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
        UtilResponse.sendError(response, "Méthode GET non supportée, utilisez POST");
    }

    /**
     * Traite la requête - équivalent de apresTarif.jsp
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
        System.out.println("ApresTarifServlet - JSON reçu: " + jsonBody);

        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonBody).getAsJsonObject();

        // Récupérer les paramètres
        String acte = UtilResponse.getJsonString(json, "acte", null);
        String classe = UtilResponse.getJsonString(json, "classe", null);
        String nomtable = UtilResponse.getJsonString(json, "nomtable", null);
        String id = UtilResponse.getJsonString(json, "id", null);
        String bute = UtilResponse.getJsonString(json, "bute", null);
        String action = UtilResponse.getJsonString(json, "action", null);
        String acteDetail = UtilResponse.getJsonString(json, "acteDetail", null);

        if (acte == null || acte.isEmpty()) {
            UtilResponse.sendError(response, "Paramètre 'acte' requis");
            return;
        }

        if (classe == null || classe.isEmpty()) {
            UtilResponse.sendError(response, "Paramètre 'classe' requis");
            return;
        }

        // Récupérer ou initialiser UserEJB
        HttpSession session = request.getSession();
        UserEJB u = (UserEJB) session.getAttribute("u");
        
        if (u == null) {
            u = initUserEJB(session, response);
            if (u == null) return;
        }

        String idResultat = null;
        ClassMAPTable resultat = null;

        // ============ TRAITEMENT SELON L'ACTION ============

        // INSERT simple
        if ("insert".equalsIgnoreCase(acte)) {
            resultat = traiterInsert(json, classe, nomtable, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // INSERT AVEC ACTION
        else if ("insertWithAction".equalsIgnoreCase(acte)) {
            resultat = traiterInsertWithAction(json, classe, nomtable, action, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // INSERT USER
        else if ("insertUser".equalsIgnoreCase(acte)) {
            idResultat = traiterInsertUser(json, classe, nomtable, u);
        }
        // UPDATE simple
        else if ("update".equalsIgnoreCase(acte)) {
            resultat = traiterUpdate(json, classe, nomtable, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // UPDATE puis VALIDER
        else if ("updatevalider".equalsIgnoreCase(acte)) {
            resultat = traiterUpdateValider(json, classe, nomtable, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // SAVE VALIDER (Insert puis valider)
        else if ("savevalider".equalsIgnoreCase(acte)) {
            resultat = traiterSaveValider(json, classe, nomtable, id, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // DELETE
        else if ("delete".equalsIgnoreCase(acte)) {
            traiterDelete(classe, nomtable, id, u);
            idResultat = id;
        }
        // DELETE FILLE
        else if ("deleteFille".equalsIgnoreCase(acte)) {
            traiterDeleteFille(classe, id, u);
            idResultat = id;
        }
        // VALIDER
        else if ("valider".equalsIgnoreCase(acte)) {
            resultat = traiterValider(classe, nomtable, id, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // ANNULER
        else if ("annuler".equalsIgnoreCase(acte)) {
            traiterAnnuler(classe, id, u);
            idResultat = id;
        }
        // ANNULER VISA
        else if ("annulerVisa".equalsIgnoreCase(acte)) {
            traiterAnnulerVisa(classe, nomtable, id, u);
            idResultat = id;
        }
        // CLOTURER
        else if ("cloturer".equalsIgnoreCase(acte)) {
            traiterCloturer(classe, nomtable, id, u);
            idResultat = id;
        }
        // PAYER
        else if ("payer".equalsIgnoreCase(acte)) {
            resultat = traiterPayer(classe, nomtable, id, u);
            if (resultat != null) {
                idResultat = resultat.getTuppleID();
            }
        }
        // FINALISER
        else if ("finaliser".equalsIgnoreCase(acte)) {
            traiterFinaliser(classe, id, u);
            idResultat = id;
        }
        // DUPLIQUER
        else if ("dupliquer".equalsIgnoreCase(acte)) {
            String classeFille = UtilResponse.getJsonString(json, "nomClasseFille", null);
            String nomColonneMere = UtilResponse.getJsonString(json, "nomColonneMere", null);
            idResultat = traiterDupliquer(classe, nomtable, id, classeFille, nomColonneMere, u);
        }
        // CUSTOM (changement d'état personnalisé)
        else if ("custom".equalsIgnoreCase(acte)) {
            traiterCustom(classe, nomtable, id, acteDetail, u);
            idResultat = id;
        }
        // DISPONIBLE (marquer produit disponible/indisponible)
        else if ("disponible".equalsIgnoreCase(acte)) {
            String idProduit = UtilResponse.getJsonString(json, "idProduit", id);
            String dispo = UtilResponse.getJsonString(json, "isdispo", "1");
            traiterDisponible(idProduit, dispo, u);
            idResultat = idProduit;
        }
        // INSERT MERE LIER FILLE
        else if ("insertMereLierFille".equalsIgnoreCase(acte)) {
            traiterInsertMereLierFille(json, classe, u);
            idResultat = id;
        }
        // INSERT MENU (utilisateur)
        else if ("insertMenu".equalsIgnoreCase(acte)) {
            String utilisateur = UtilResponse.getJsonString(json, "refuser", null);
            String menu = UtilResponse.getJsonString(json, "idmenu", null);
            String acces = UtilResponse.getJsonString(json, "interdit", null);
            u.ajouterMenuUtilisateur(utilisateur, menu, null, acces);
            idResultat = utilisateur;
        }
        else {
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

    /**
     * Initialise UserEJB avec auto-login si nécessaire
     */
    private UserEJB initUserEJB(HttpSession session, HttpServletResponse response) throws IOException {
        UserEJB u = null;
        try {
            u = UserEJBClient.lookupUserEJBBeanLocal();
        } catch (Exception e) {
            System.out.println("ApresTarifServlet - Lookup EJB failed: " + e.getMessage());
            UtilResponse.sendError(response, "Erreur lors du lookup EJB: " + e.getMessage());
            return null;
        }

        historique.MapUtilisateurServiceDirection user = 
            (historique.MapUtilisateurServiceDirection) session.getAttribute("user");

        if (user != null) {
            try {
                u.setU(user);
            } catch (Exception e) {
                UtilResponse.sendError(response, "Erreur lors de l'initialisation utilisateur: " + e.getMessage());
                return null;
            }
        } else {
            // AUTO-LOGIN avec admin/test si aucun utilisateur en session
            System.out.println("ApresTarifServlet - No user in session, doing auto-login");
            try {
                u.testLogin("admin", "test", null, null);
                session.setAttribute("u", u);
                session.setAttribute("user", u.getU());
                session.setAttribute("config", u.findConfiguration());
            } catch (Exception e) {
                System.out.println("ApresTarifServlet - Auto-login failed: " + e.getMessage());
                UtilResponse.sendError(response, "Erreur lors de l'auto-login: " + e.getMessage());
                return null;
            }
        }

        session.setAttribute("u", u);
        return u;
    }

    /**
     * Crée une HttpServletRequest wrapper avec les paramètres du JSON
     */
    private HttpServletRequest createRequestWrapper(HttpServletRequest originalRequest, JsonObject json) {
        final Map<String, String> params = new HashMap<>();
        
        // Convertir le JSON en paramètres
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (!value.isJsonNull()) {
                if (value.isJsonPrimitive()) {
                    params.put(key, value.getAsString());
                } else if (value.isJsonObject()) {
                    // Pour les objets imbriqués comme "data" ou "mere"
                    JsonObject nested = value.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> nestedEntry : nested.entrySet()) {
                        String nestedKey = nestedEntry.getKey();
                        JsonElement nestedValue = nestedEntry.getValue();
                        if (!nestedValue.isJsonNull() && nestedValue.isJsonPrimitive()) {
                            params.put(nestedKey, nestedValue.getAsString());
                        }
                    }
                }
            }
        }

        return new HttpServletRequestWrapper(originalRequest) {
            @Override
            public String getParameter(String name) {
                String value = params.get(name);
                return value != null ? value : super.getParameter(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                Map<String, String[]> map = new HashMap<>(super.getParameterMap());
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    map.put(entry.getKey(), new String[]{entry.getValue()});
                }
                return map;
            }

            @Override
            public Enumeration<String> getParameterNames() {
                java.util.Set<String> names = new java.util.HashSet<>();
                Enumeration<String> superNames = super.getParameterNames();
                while (superNames.hasMoreElements()) {
                    names.add(superNames.nextElement());
                }
                names.addAll(params.keySet());
                return java.util.Collections.enumeration(names);
            }
        };
    }

    // ============ METHODES DE TRAITEMENT ============

    /**
     * INSERT - Insertion simple d'un objet
     */
    private ClassMAPTable traiterInsert(JsonObject json, String classe, String nomtable, UserEJB u) 
            throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        
        // Remplir l'objet depuis les données JSON
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : 
                          json.has("mere") ? json.getAsJsonObject("mere") : json;
        UtilResponse.remplirObjetDepuisJson(t, data);
        
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        
        return (ClassMAPTable) u.createObject(t);
    }

    /**
     * INSERT WITH ACTION - Insertion avec action personnalisée
     */
    private ClassMAPTable traiterInsertWithAction(JsonObject json, String classe, String nomtable, 
            String action, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : 
                          json.has("mere") ? json.getAsJsonObject("mere") : json;
        UtilResponse.remplirObjetDepuisJson(t, data);
        
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        
        return (ClassMAPTable) u.createObject(t, action);
    }

    /**
     * INSERT USER - Insertion d'un utilisateur
     */
    private String traiterInsertUser(JsonObject json, String classe, String nomtable, UserEJB u) 
            throws Exception {
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : json;
        
        String login = UtilResponse.getJsonString(data, "loginuser", null);
        String pwd = UtilResponse.getJsonString(data, "pwduser", null);
        String nom = UtilResponse.getJsonString(data, "nomuser", null);
        String adr = UtilResponse.getJsonString(data, "adruser", null);
        String tel = UtilResponse.getJsonString(data, "teluser", null);
        String idrole = UtilResponse.getJsonString(data, "idrole", null);
        
        return u.createUtilisateurs(login, pwd, nom, adr, tel, idrole);
    }

    /**
     * UPDATE - Mise à jour d'un objet
     */
    private ClassMAPTable traiterUpdate(JsonObject json, String classe, String nomtable, UserEJB u) 
            throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : 
                          json.has("mere") ? json.getAsJsonObject("mere") : json;
        UtilResponse.remplirObjetDepuisJson(t, data);
        
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        
        u.updateObject(t);
        return t;
    }

    /**
     * UPDATE VALIDER - Mise à jour puis validation
     */
    private ClassMAPTable traiterUpdateValider(JsonObject json, String classe, String nomtable, UserEJB u) 
            throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : 
                          json.has("mere") ? json.getAsJsonObject("mere") : json;
        UtilResponse.remplirObjetDepuisJson(t, data);
        
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        
        u.updateObject(t);
        u.validerObject(t);
        return t;
    }

    /**
     * SAVE VALIDER - Sauvegarde puis validation
     */
    private ClassMAPTable traiterSaveValider(JsonObject json, String classe, String nomtable, 
            String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : 
                          json.has("mere") ? json.getAsJsonObject("mere") : json;
        UtilResponse.remplirObjetDepuisJson(t, data);
        
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        
        // Créer l'objet
        ClassMAPTable created = (ClassMAPTable) u.createObject(t);
        
        // Puis valider
        if (created != null) {
            ClassMAPTable toValidate = (ClassMAPTable) Class.forName(classe).newInstance();
            toValidate.setValChamp(toValidate.getAttributIDName(), created.getTuppleID());
            if (nomtable != null && !nomtable.isEmpty()) {
                toValidate.setNomTable(nomtable);
            }
            return (ClassMAPTable) u.validerObject(toValidate);
        }
        return created;
    }

    /**
     * DELETE - Suppression d'un objet
     */
    private void traiterDelete(String classe, String nomtable, String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        u.deleteObject(t);
    }

    /**
     * DELETE FILLE - Suppression d'un objet fille
     */
    private void traiterDeleteFille(String classe, String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        u.deleteObjetFille(t);
    }

    /**
     * VALIDER - Validation d'un objet
     */
    private ClassMAPTable traiterValider(String classe, String nomtable, String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        System.out.println("ApresTarifServlet - Valider: " + nomtable + " ::: " + id);
        System.out.println("ApresTarifServlet - classename: " + t.getClassName());
        return (ClassMAPTable) u.validerObject(t);
    }

    /**
     * ANNULER - Annulation d'un objet
     */
    private void traiterAnnuler(String classe, String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        u.annulerObject(t);
    }

    /**
     * ANNULER VISA - Annulation du visa
     */
    private void traiterAnnulerVisa(String classe, String nomtable, String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        u.annulerVisa(t);
    }

    /**
     * CLOTURER - Clôture d'un objet
     */
    private void traiterCloturer(String classe, String nomtable, String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        u.cloturerObject(t);
    }

    /**
     * PAYER - Paiement d'un objet
     */
    private ClassMAPTable traiterPayer(String classe, String nomtable, String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        return (ClassMAPTable) u.payerObject(t);
    }

    /**
     * FINALISER - Finalisation d'un objet
     */
    private void traiterFinaliser(String classe, String id, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        u.finaliser(t);
    }

    /**
     * DUPLIQUER - Duplication d'un objet
     */
    private String traiterDupliquer(String classe, String nomtable, String id, 
            String classeFille, String nomColonneMere, UserEJB u) throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        Object resultat = u.dupliquerObject(t, classeFille, nomColonneMere);
        return resultat != null ? resultat.toString() : null;
    }

    /**
     * CUSTOM - Changement d'état personnalisé
     */
    private void traiterCustom(String classe, String nomtable, String id, String acteDetail, UserEJB u) 
            throws Exception {
        ClassMAPTable t = (ClassMAPTable) Class.forName(classe).newInstance();
        t.setValChamp(t.getAttributIDName(), id);
        if (nomtable != null && !nomtable.isEmpty()) {
            t.setNomTable(nomtable);
        }
        u.changeState(t, acteDetail);
    }

    /**
     * DISPONIBLE - Marquer un produit disponible/indisponible
     */
    private void traiterDisponible(String idProduit, String dispo, UserEJB u) throws Exception {
        Ingredients ing = new Ingredients();
        ing.produitDisponible(idProduit, dispo, u.getUser().getTuppleID());
    }

    /**
     * INSERT MERE LIER FILLE - Insérer une mère et lier des filles existantes
     */
    private void traiterInsertMereLierFille(JsonObject json, String classe, UserEJB u) throws Exception {
        String colonneMere = UtilResponse.getJsonString(json, "colonneMere", null);
        String colonneFille = UtilResponse.getJsonString(json, "colonneFille", null);
        String classeFille = UtilResponse.getJsonString(json, "classeFille", null);
        
        if (classeFille == null || colonneMere == null || colonneFille == null) {
            throw new Exception("colonneMere, colonneFille et classeFille requis pour insertMereLierFille");
        }
        
        // Créer l'objet mère
        ClassMAPTable mere = (ClassMAPTable) Class.forName(classe).newInstance();
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : 
                          json.has("mere") ? json.getAsJsonObject("mere") : json;
        UtilResponse.remplirObjetDepuisJson(mere, data);
        
        // Récupérer les IDs des filles
        String[] listeIdFille = null;
        if (json.has("ids")) {
            com.google.gson.JsonArray ids = json.getAsJsonArray("ids");
            listeIdFille = new String[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                listeIdFille[i] = ids.get(i).getAsString();
            }
        }
        
        // Créer l'objet fille template
        ClassFille fille = (ClassFille) Class.forName(classeFille).newInstance();
        
        // Appeler la méthode d'insertion avec liaison
        u.insertMereLierFilles((bean.ClassMere) mere, fille, listeIdFille, colonneFille, colonneMere);
    }
}
