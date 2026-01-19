package servlet.api;

import bean.CGenUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fabrication.Fabrication;
import fabrication.FabricationCpl;
import fabrication.FabricationFille;
import fabrication.FabricationFilleCpl;
import produits.Ingredients;
import produits.Recette;
import produits.RecetteLib;
import utilitaire.UtilDB;
import utils.ConstanteProcess;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST pour la gestion des fabrications.
 * Expose les EJB Fabrication en web service JSON.
 */
@WebServlet(name = "FabricationApi", urlPatterns = {"/api/fabrication/*"})
public class FabricationApiServlet extends HttpServlet {

    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd")
            .create();

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/liste")) {
                // GET /api/fabrication/liste - Liste des fabrications
                handleListe(request, response, out);
            } else if (pathInfo.equals("/historique")) {
                // GET /api/fabrication/historique - Historique paginé
                handleHistorique(request, response, out);
            } else if (pathInfo.startsWith("/fiche/")) {
                // GET /api/fabrication/fiche/{id} - Détail d'une fabrication
                String id = pathInfo.substring("/fiche/".length());
                handleFiche(id, response, out);
            } else if (pathInfo.startsWith("/formule/")) {
                // GET /api/fabrication/formule/{produitId} - Formule d'un produit
                String produitId = pathInfo.substring("/formule/".length());
                handleFormule(produitId, response, out);
            } else if (pathInfo.equals("/simuler")) {
                // GET /api/fabrication/simuler?produitId=xxx&quantite=xxx
                handleSimuler(request, response, out);
            } else {
                sendError(response, out, 404, "Endpoint non trouvé: " + pathInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, out, 500, "Erreur serveur: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/creer")) {
                // POST /api/fabrication/creer - Créer une fabrication
                handleCreer(request, response, out);
            } else if (pathInfo.equals("/executer")) {
                // POST /api/fabrication/executer - Créer et terminer directement
                handleExecuter(request, response, out);
            } else {
                sendError(response, out, 404, "Endpoint non trouvé: " + pathInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, out, 500, "Erreur serveur: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.contains("/valider/")) {
                String id = pathInfo.substring(pathInfo.lastIndexOf("/") + 1);
                handleValider(id, request, response, out);
            } else if (pathInfo != null && pathInfo.contains("/entamer/")) {
                String id = pathInfo.substring(pathInfo.lastIndexOf("/") + 1);
                handleEntamer(id, request, response, out);
            } else if (pathInfo != null && pathInfo.contains("/bloquer/")) {
                String id = pathInfo.substring(pathInfo.lastIndexOf("/") + 1);
                handleBloquer(id, request, response, out);
            } else if (pathInfo != null && pathInfo.contains("/terminer/")) {
                String id = pathInfo.substring(pathInfo.lastIndexOf("/") + 1);
                handleTerminer(id, request, response, out);
            } else {
                sendError(response, out, 404, "Endpoint non trouvé: " + pathInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, out, 500, "Erreur serveur: " + e.getMessage());
        }
    }

    /**
     * Liste des fabrications avec filtres.
     */
    private void handleListe(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            FabricationCpl fab = new FabricationCpl();
            
            // Filtre par état
            String etat = request.getParameter("etat");
            String nomTable = "FABRICATIONCPL";
            if (etat != null && !etat.isEmpty()) {
                nomTable = etat;
            }
            fab.setNomTable(nomTable);
            
            // Filtre par date
            String condition = "";
            String dateMin = request.getParameter("dateMin");
            String dateMax = request.getParameter("dateMax");
            
            if (dateMin != null && !dateMin.isEmpty()) {
                condition += " AND daty >= TO_DATE('" + dateMin + "', 'YYYY-MM-DD')";
            }
            if (dateMax != null && !dateMax.isEmpty()) {
                condition += " AND daty <= TO_DATE('" + dateMax + "', 'YYYY-MM-DD')";
            }
            
            FabricationCpl[] fabrications = (FabricationCpl[]) CGenUtil.rechercher(fab, null, null, c, condition + " ORDER BY daty DESC");
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (FabricationCpl f : fabrications) {
                result.add(fabricationToMap(f));
            }
            
            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Historique paginé des fabrications.
     */
    private void handleHistorique(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            int page = 0;
            int size = 20;
            try {
                if (request.getParameter("page") != null) {
                    page = Integer.parseInt(request.getParameter("page"));
                }
                if (request.getParameter("size") != null) {
                    size = Integer.parseInt(request.getParameter("size"));
                }
            } catch (NumberFormatException ignored) {}
            
            FabricationCpl fab = new FabricationCpl();
            
            // Filtre par état
            String etat = request.getParameter("etat");
            String nomTable = "FABRICATIONCPL";
            if (etat != null && !etat.isEmpty()) {
                nomTable = etat;
            }
            fab.setNomTable(nomTable);
            
            // Filtre par date
            String condition = "";
            String dateMin = request.getParameter("dateMin");
            String dateMax = request.getParameter("dateMax");
            
            if (dateMin != null && !dateMin.isEmpty()) {
                condition += " AND daty >= TO_DATE('" + dateMin + "', 'YYYY-MM-DD')";
            }
            if (dateMax != null && !dateMax.isEmpty()) {
                condition += " AND daty <= TO_DATE('" + dateMax + "', 'YYYY-MM-DD')";
            }
            
            // Récupérer toutes les fabrications pour le compte total
            FabricationCpl[] allFabrications = (FabricationCpl[]) CGenUtil.rechercher(fab, null, null, c, condition + " ORDER BY daty DESC");
            int totalElements = allFabrications.length;
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            // Pagination manuelle
            int start = page * size;
            int end = Math.min(start + size, totalElements);
            
            List<Map<String, Object>> content = new ArrayList<>();
            for (int i = start; i < end; i++) {
                content.add(fabricationToMap(allFabrications[i]));
            }
            
            // Réponse paginée
            Map<String, Object> result = new HashMap<>();
            result.put("content", content);
            result.put("page", page);
            result.put("size", size);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("first", page == 0);
            result.put("last", page >= totalPages - 1);
            
            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Détail d'une fabrication.
     */
    private void handleFiche(String id, HttpServletResponse response, PrintWriter out) throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            Fabrication fab = new Fabrication();
            fab.setNomTable("FABRICATION");
            fab.setId(id);
            
            Fabrication[] fabs = (Fabrication[]) CGenUtil.rechercher(fab, null, null, c, "");
            
            if (fabs.length == 0) {
                sendError(response, out, 404, "Fabrication non trouvée: " + id);
                return;
            }
            
            Fabrication fabrication = fabs[0];
            Map<String, Object> result = fabricationToMap(fabrication);
            
            // Récupérer les lignes filles
            FabricationFilleCpl fille = new FabricationFilleCpl();
            fille.setNomTable("FABRICATIONFILLECPL");
            FabricationFilleCpl[] filles = (FabricationFilleCpl[]) CGenUtil.rechercher(fille, null, null, c, " AND idmere = '" + id + "'");
            
            List<Map<String, Object>> lignes = new ArrayList<>();
            for (FabricationFilleCpl f : filles) {
                Map<String, Object> ligne = new HashMap<>();
                ligne.put("id", f.getId());
                ligne.put("idIngredients", f.getIdIngredients());
                ligne.put("libelle", f.getIdingredientsLib());
                ligne.put("qte", f.getQte());
                ligne.put("unite", f.getIdunitelib());
                ligne.put("pu", f.getPu());
                lignes.add(ligne);
            }
            result.put("lignes", lignes);
            
            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Formule/recette d'un produit.
     */
    private void handleFormule(String produitId, HttpServletResponse response, PrintWriter out) throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            RecetteLib recette = new RecetteLib();
            recette.setNomTable("RECETTELIB");
            
            RecetteLib[] recettes = (RecetteLib[]) CGenUtil.rechercher(recette, null, null, c, " AND idproduits = '" + produitId + "'");
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (RecetteLib r : recettes) {
                Map<String, Object> item = new HashMap<>();
                item.put("idIngredients", r.getIdingredients());
                item.put("libelle", r.getLibelleingredient());
                item.put("quantite", r.getQuantite());
                item.put("unite", r.getIdunite());
                item.put("qteav", r.getQteav());
                result.add(item);
            }
            
            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Simuler une fabrication.
     */
    private void handleSimuler(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            
            String produitId = request.getParameter("produitId");
            double quantite = Double.parseDouble(request.getParameter("quantite"));
            
            // Récupérer le produit
            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");
            ing.setId(produitId);
            Ingredients[] ings = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, "");
            
            if (ings.length == 0) {
                sendError(response, out, 404, "Produit non trouvé: " + produitId);
                return;
            }
            
            Ingredients produit = ings[0];
            
            // Récupérer la recette
            RecetteLib recette = new RecetteLib();
            recette.setNomTable("RECETTELIB");
            RecetteLib[] recettes = (RecetteLib[]) CGenUtil.rechercher(recette, null, null, c, " AND idproduits = '" + produitId + "'");
            
            List<Map<String, Object>> besoins = new ArrayList<>();
            boolean peutFabriquer = true;
            
            for (RecetteLib r : recettes) {
                // Récupérer le stock de l'ingrédient
                Ingredients ingredient = new Ingredients();
                ingredient.setNomTable("AS_INGREDIENTS");
                ingredient.setId(r.getIdingredients());
                Ingredients[] ingredients = (Ingredients[]) CGenUtil.rechercher(ingredient, null, null, c, "");
                
                double stockDispo = 0;
                if (ingredients.length > 0) {
                    stockDispo = ingredients[0].getReste();
                }
                
                double besoinTotal = r.getQuantite() * quantite;
                double manquant = Math.max(0, besoinTotal - stockDispo);
                boolean suffisant = stockDispo >= besoinTotal;
                
                if (!suffisant) {
                    peutFabriquer = false;
                }
                
                Map<String, Object> item = new HashMap<>();
                item.put("idIngredients", r.getIdingredients());
                item.put("libelle", r.getLibelleingredient());
                item.put("qteParUnite", r.getQuantite());
                item.put("besoinTotal", besoinTotal);
                item.put("stockDisponible", stockDispo);
                item.put("manquant", manquant);
                item.put("suffisant", suffisant);
                item.put("unite", r.getIdunite());
                besoins.add(item);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("produitId", produitId);
            result.put("produitLibelle", produit.getLibelle());
            result.put("produitUnite", produit.getUnite());
            result.put("quantiteAFabriquer", quantite);
            result.put("besoins", besoins);
            result.put("peutFabriquer", peutFabriquer);
            result.put("message", peutFabriquer ? "Tous les ingrédients sont disponibles" : "Stock insuffisant pour certains ingrédients");
            
            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Créer une fabrication.
     */
    private void handleCreer(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            
            // Lire le body JSON
            String body = getRequestBody(request);
            Map<String, Object> data = gson.fromJson(body, Map.class);
            
            String produitId = (String) data.get("produitId");
            double quantite = ((Number) data.get("quantite")).doubleValue();
            String user = data.get("user") != null ? (String) data.get("user") : "API";
            
            // Récupérer le produit
            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");
            ing.setId(produitId);
            Ingredients[] ings = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, "");
            
            if (ings.length == 0) {
                sendError(response, out, 404, "Produit non trouvé: " + produitId);
                return;
            }
            
            Ingredients produit = ings[0];
            
            // Créer la fabrication
            Fabrication fab = new Fabrication();
            fab.setNomTable("FABRICATION");
            fab.setCible(produitId);
            fab.setLibelle("Fabrication de " + produit.getLibelle() + " x " + quantite);
            fab.setDaty(Date.valueOf(LocalDate.now()));
            fab.setLancePar(user);
            
            // Récupérer la recette pour créer les lignes
            RecetteLib recette = new RecetteLib();
            recette.setNomTable("RECETTELIB");
            RecetteLib[] recettes = (RecetteLib[]) CGenUtil.rechercher(recette, null, null, c, " AND idproduits = '" + produitId + "'");
            
            FabricationFille[] lignes = new FabricationFille[recettes.length];
            for (int i = 0; i < recettes.length; i++) {
                FabricationFille ligne = new FabricationFille();
                ligne.setIdIngredients(recettes[i].getIdingredients());
                ligne.setLibelle(recettes[i].getLibelleingredient());
                ligne.setQte(recettes[i].getQuantite() * quantite);
                ligne.setIdunite(recettes[i].getIdunite());
                lignes[i] = ligne;
            }
            fab.setFille(lignes);
            
            // Sauvegarder
            fab.createObject(user, c);
            
            c.commit();
            
            Map<String, Object> result = fabricationToMap(fab);
            result.put("message", "Fabrication créée avec succès");
            
            out.print(gson.toJson(result));
        } catch (Exception e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Exécuter une fabrication (créer + terminer directement).
     */
    private void handleExecuter(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            
            // Lire le body JSON
            String body = getRequestBody(request);
            Map<String, Object> data = gson.fromJson(body, Map.class);
            
            String produitId = (String) data.get("produitId");
            double quantite = ((Number) data.get("quantite")).doubleValue();
            String user = data.get("user") != null ? (String) data.get("user") : "API";
            
            // Récupérer le produit
            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");
            ing.setId(produitId);
            Ingredients[] ings = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, "");
            
            if (ings.length == 0) {
                sendError(response, out, 404, "Produit non trouvé: " + produitId);
                return;
            }
            
            Ingredients produit = ings[0];
            
            // Créer la fabrication
            Fabrication fab = new Fabrication();
            fab.setNomTable("FABRICATION");
            fab.setCible(produitId);
            fab.setLibelle("Fabrication de " + produit.getLibelle() + " x " + quantite);
            fab.setDaty(Date.valueOf(LocalDate.now()));
            fab.setLancePar(user);
            
            // Récupérer la recette
            RecetteLib recette = new RecetteLib();
            recette.setNomTable("RECETTELIB");
            RecetteLib[] recettes = (RecetteLib[]) CGenUtil.rechercher(recette, null, null, c, " AND idproduits = '" + produitId + "'");
            
            FabricationFille[] lignes = new FabricationFille[recettes.length];
            for (int i = 0; i < recettes.length; i++) {
                FabricationFille ligne = new FabricationFille();
                ligne.setIdIngredients(recettes[i].getIdingredients());
                ligne.setLibelle(recettes[i].getLibelleingredient());
                ligne.setQte(recettes[i].getQuantite() * quantite);
                ligne.setIdunite(recettes[i].getIdunite());
                lignes[i] = ligne;
            }
            fab.setFille(lignes);
            
            // Créer et terminer
            fab.createObject(user, c);
            fab.validerObject(user, c);
            fab.entamerObject(user, c);
            fab.terminerObject(user, c);
            
            c.commit();
            
            Map<String, Object> result = fabricationToMap(fab);
            result.put("message", "Fabrication exécutée avec succès");
            
            out.print(gson.toJson(result));
        } catch (Exception e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Valider une fabrication (CRÉÉ -> VALIDÉ).
     */
    private void handleValider(String id, HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            
            String user = request.getParameter("user") != null ? request.getParameter("user") : "API";
            
            Fabrication fab = new Fabrication();
            fab.setNomTable("FABRICATION");
            fab.setId(id);
            Fabrication[] fabs = (Fabrication[]) CGenUtil.rechercher(fab, null, null, c, "");
            
            if (fabs.length == 0) {
                sendError(response, out, 404, "Fabrication non trouvée: " + id);
                return;
            }
            
            fab = fabs[0];
            fab.validerObject(user, c);
            
            c.commit();
            
            Map<String, Object> result = fabricationToMap(fab);
            result.put("message", "Fabrication validée avec succès");
            
            out.print(gson.toJson(result));
        } catch (Exception e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Entamer une fabrication (VALIDÉ -> ENTAMÉ).
     */
    private void handleEntamer(String id, HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            
            String user = request.getParameter("user") != null ? request.getParameter("user") : "API";
            
            Fabrication fab = new Fabrication();
            fab.setNomTable("FABRICATION");
            fab.setId(id);
            Fabrication[] fabs = (Fabrication[]) CGenUtil.rechercher(fab, null, null, c, "");
            
            if (fabs.length == 0) {
                sendError(response, out, 404, "Fabrication non trouvée: " + id);
                return;
            }
            
            fab = fabs[0];
            fab.entamerObject(user, c);
            
            c.commit();
            
            Map<String, Object> result = fabricationToMap(fab);
            result.put("message", "Fabrication entamée avec succès");
            
            out.print(gson.toJson(result));
        } catch (Exception e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Bloquer une fabrication (ENTAMÉ -> BLOQUÉ).
     */
    private void handleBloquer(String id, HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            
            String user = request.getParameter("user") != null ? request.getParameter("user") : "API";
            
            Fabrication fab = new Fabrication();
            fab.setNomTable("FABRICATION");
            fab.setId(id);
            Fabrication[] fabs = (Fabrication[]) CGenUtil.rechercher(fab, null, null, c, "");
            
            if (fabs.length == 0) {
                sendError(response, out, 404, "Fabrication non trouvée: " + id);
                return;
            }
            
            fab = fabs[0];
            fab.bloquerObject(user, c);
            
            c.commit();
            
            Map<String, Object> result = fabricationToMap(fab);
            result.put("message", "Fabrication bloquée avec succès");
            
            out.print(gson.toJson(result));
        } catch (Exception e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Terminer une fabrication (ENTAMÉ -> TERMINÉ).
     */
    private void handleTerminer(String id, HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();
            c.setAutoCommit(false);
            
            String user = request.getParameter("user") != null ? request.getParameter("user") : "API";
            
            Fabrication fab = new Fabrication();
            fab.setNomTable("FABRICATION");
            fab.setId(id);
            Fabrication[] fabs = (Fabrication[]) CGenUtil.rechercher(fab, null, null, c, "");
            
            if (fabs.length == 0) {
                sendError(response, out, 404, "Fabrication non trouvée: " + id);
                return;
            }
            
            fab = fabs[0];
            fab.terminerObject(user, c);
            
            c.commit();
            
            Map<String, Object> result = fabricationToMap(fab);
            result.put("message", "Fabrication terminée avec succès");
            
            out.print(gson.toJson(result));
        } catch (Exception e) {
            if (c != null) c.rollback();
            throw e;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Convertir une Fabrication en Map pour JSON.
     */
    private Map<String, Object> fabricationToMap(Fabrication fab) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", fab.getId());
        map.put("libelle", fab.getLibelle());
        map.put("cible", fab.getCible());
        map.put("remarque", fab.getRemarque());
        map.put("daty", fab.getDaty());
        map.put("besoin", fab.getBesoin());
        map.put("etat", fab.getEtat());
        map.put("etatLib", getEtatLib(fab.getEtat()));
        map.put("lancePar", fab.getLancePar());
        map.put("idOf", fab.getIdOf());
        map.put("idOffille", fab.getIdOffille());
        map.put("equipe", fab.getEquipe());
        
        if (fab instanceof FabricationCpl) {
            FabricationCpl cpl = (FabricationCpl) fab;
            map.put("lanceparLib", cpl.getLanceparLib());
            map.put("cibleLib", cpl.getCibleLib());
        }
        
        return map;
    }

    /**
     * Obtenir le libellé de l'état.
     */
    private String getEtatLib(int etat) {
        switch (etat) {
            case 1: return "CRÉÉ";
            case 11: return "VALIDÉ";
            case ConstanteProcess.entame: return "ENTAMÉ";
            case ConstanteProcess.bloque: return "BLOQUÉ";
            case ConstanteProcess.termine: return "TERMINÉ";
            case -1: return "ANNULÉ";
            default: return "INCONNU";
        }
    }

    /**
     * Lire le body de la requête.
     */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Envoyer une erreur JSON.
     */
    private void sendError(HttpServletResponse response, PrintWriter out, int status, String message) {
        response.setStatus(status);
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("status", status);
        error.put("message", message);
        out.print(gson.toJson(error));
    }

    /**
     * Configurer les headers CORS.
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age", "86400");
    }
}
