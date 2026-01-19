package servlet.api;

import bean.CGenUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import produits.Ingredients;
import utilitaire.UtilDB;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST pour la gestion des produits (ingrédients).
 * Expose les EJB Ingredients en web service JSON.
 */
@WebServlet(name = "ProduitsApi", urlPatterns = {"/api/produits/*"})
public class ProduitsApiServlet extends HttpServlet {

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
                // GET /api/produits/liste - Liste de tous les produits
                handleListe(request, response, out);
            } else if (pathInfo.equals("/page")) {
                // GET /api/produits/page - Liste paginée
                handlePage(request, response, out);
            } else if (pathInfo.equals("/autocomplete")) {
                // GET /api/produits/autocomplete?q=xxx - Autocomplete
                handleAutocomplete(request, response, out);
            } else if (pathInfo.equals("/finis")) {
                // GET /api/produits/finis - Produits finis
                handleProduitsFinis(request, response, out);
            } else if (pathInfo.equals("/intermediaires")) {
                // GET /api/produits/intermediaires - Produits intermédiaires
                handleProduitsIntermediaires(request, response, out);
            } else if (pathInfo.equals("/matieres-premieres")) {
                // GET /api/produits/matieres-premieres - Matières premières
                handleMatierePremieres(request, response, out);
            } else if (pathInfo.equals("/fabricables")) {
                // GET /api/produits/fabricables - Produits fabricables (composés)
                handleProduitsFabricables(request, response, out);
            } else if (pathInfo.startsWith("/fiche/")) {
                // GET /api/produits/fiche/{id} - Détail d'un produit
                String id = pathInfo.substring("/fiche/".length());
                handleFiche(id, response, out);
            } else {
                sendError(response, out, 404, "Endpoint non trouvé: " + pathInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, out, 500, "Erreur serveur: " + e.getMessage());
        }
    }

    /**
     * Liste de tous les produits.
     */
    private void handleListe(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");

            String condition = " AND (actif = 1 OR actif IS NULL)";
            
            // Filtre par type
            String type = request.getParameter("type");
            if (type != null && !type.isEmpty()) {
                switch (type.toUpperCase()) {
                    case "PRODUIT_FINI":
                    case "PF":
                        condition += " AND compose = 1 AND (pv > 0 OR libellevente IS NOT NULL)";
                        break;
                    case "PRODUIT_INTERMEDIAIRE":
                    case "PI":
                        condition += " AND compose = 1 AND (pv IS NULL OR pv = 0) AND libellevente IS NULL";
                        break;
                    case "MATIERE_PREMIERE":
                    case "MP":
                        condition += " AND (compose = 0 OR compose IS NULL)";
                        break;
                }
            }

            // Filtre par recherche
            String search = request.getParameter("search");
            if (search != null && !search.isEmpty()) {
                condition += " AND LOWER(libelle) LIKE LOWER('%" + search.replace("'", "''") + "%')";
            }

            Ingredients[] produits = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, condition + " ORDER BY libelle");

            List<Map<String, Object>> result = new ArrayList<>();
            for (Ingredients p : produits) {
                result.add(ingredientToMap(p));
            }

            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Liste paginée des produits.
     */
    private void handlePage(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
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

            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");

            String condition = " AND (actif = 1 OR actif IS NULL)";

            // Filtre par type
            String type = request.getParameter("type");
            if (type != null && !type.isEmpty()) {
                switch (type.toUpperCase()) {
                    case "PRODUIT_FINI":
                    case "PF":
                        condition += " AND compose = 1 AND (pv > 0 OR libellevente IS NOT NULL)";
                        break;
                    case "PRODUIT_INTERMEDIAIRE":
                    case "PI":
                        condition += " AND compose = 1 AND (pv IS NULL OR pv = 0) AND libellevente IS NULL";
                        break;
                    case "MATIERE_PREMIERE":
                    case "MP":
                        condition += " AND (compose = 0 OR compose IS NULL)";
                        break;
                }
            }

            // Filtre par recherche
            String search = request.getParameter("search");
            if (search != null && !search.isEmpty()) {
                condition += " AND LOWER(libelle) LIKE LOWER('%" + search.replace("'", "''") + "%')";
            }

            // Récupérer tous les produits pour le compte total
            Ingredients[] allProduits = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, condition + " ORDER BY libelle");
            int totalElements = allProduits.length;
            int totalPages = (int) Math.ceil((double) totalElements / size);

            // Pagination manuelle
            int start = page * size;
            int end = Math.min(start + size, totalElements);

            List<Map<String, Object>> content = new ArrayList<>();
            for (int i = start; i < end; i++) {
                content.add(ingredientToMap(allProduits[i]));
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
     * Autocomplete pour recherche de produits.
     */
    private void handleAutocomplete(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            String q = request.getParameter("q");
            if (q == null || q.trim().isEmpty()) {
                out.print(gson.toJson(new ArrayList<>()));
                return;
            }

            int limit = 10;
            try {
                if (request.getParameter("limit") != null) {
                    limit = Integer.parseInt(request.getParameter("limit"));
                }
            } catch (NumberFormatException ignored) {}

            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");

            String condition = " AND (actif = 1 OR actif IS NULL)";
            condition += " AND LOWER(libelle) LIKE LOWER('%" + q.replace("'", "''") + "%')";
            
            // Filtre par type si spécifié
            String type = request.getParameter("type");
            if (type != null && !type.isEmpty()) {
                switch (type.toUpperCase()) {
                    case "FABRICABLE":
                        condition += " AND compose = 1";
                        break;
                    case "PRODUIT_FINI":
                    case "PF":
                        condition += " AND compose = 1 AND (pv > 0 OR libellevente IS NOT NULL)";
                        break;
                }
            }

            Ingredients[] produits = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, condition + " ORDER BY libelle");

            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < Math.min(limit, produits.length); i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", produits[i].getId());
                item.put("libelle", produits[i].getLibelle());
                item.put("unite", produits[i].getUnite());
                item.put("stock", produits[i].getReste());
                item.put("type", determineType(produits[i]));
                result.add(item);
            }

            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Produits finis (composés et vendables).
     */
    private void handleProduitsFinis(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");

            String condition = " AND (actif = 1 OR actif IS NULL) AND compose = 1 AND (pv > 0 OR libellevente IS NOT NULL) ORDER BY libelle";

            Ingredients[] produits = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, condition);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Ingredients p : produits) {
                result.add(ingredientToMap(p));
            }

            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Produits intermédiaires (composés mais pas vendables).
     */
    private void handleProduitsIntermediaires(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");

            String condition = " AND (actif = 1 OR actif IS NULL) AND compose = 1 AND (pv IS NULL OR pv = 0) AND libellevente IS NULL ORDER BY libelle";

            Ingredients[] produits = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, condition);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Ingredients p : produits) {
                result.add(ingredientToMap(p));
            }

            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Matières premières (non composées).
     */
    private void handleMatierePremieres(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");

            String condition = " AND (actif = 1 OR actif IS NULL) AND (compose = 0 OR compose IS NULL) ORDER BY libelle";

            Ingredients[] produits = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, condition);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Ingredients p : produits) {
                result.add(ingredientToMap(p));
            }

            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Produits fabricables (composés).
     */
    private void handleProduitsFabricables(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");

            String condition = " AND (actif = 1 OR actif IS NULL) AND compose = 1 ORDER BY libelle";

            Ingredients[] produits = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, condition);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Ingredients p : produits) {
                result.add(ingredientToMap(p));
            }

            out.print(gson.toJson(result));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Détail d'un produit.
     */
    private void handleFiche(String id, HttpServletResponse response, PrintWriter out) throws Exception {
        Connection c = null;
        try {
            c = new UtilDB().GetConn();

            Ingredients ing = new Ingredients();
            ing.setNomTable("AS_INGREDIENTS");
            ing.setId(id);

            Ingredients[] produits = (Ingredients[]) CGenUtil.rechercher(ing, null, null, c, "");

            if (produits.length == 0) {
                sendError(response, out, 404, "Produit non trouvé: " + id);
                return;
            }

            out.print(gson.toJson(ingredientToMap(produits[0])));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Convertir un Ingredient en Map pour JSON.
     */
    private Map<String, Object> ingredientToMap(Ingredients ing) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ing.getId());
        map.put("libelle", ing.getLibelle());
        map.put("unite", ing.getUnite());
        map.put("stock", ing.getReste());
        map.put("seuil", ing.getSeuil());
        map.put("pu", ing.getPu());
        map.put("pv", ing.getPv());
        map.put("compose", ing.getCompose());
        map.put("actif", ing.getActif());
        map.put("photo", ing.getPhoto());
        map.put("categorie", ing.getCategorieIngredient());
        map.put("codebarre", ing.getCodebarre());
        map.put("type", determineType(ing));
        map.put("stockStatus", determineStockStatus(ing));
        return map;
    }

    /**
     * Déterminer le type de produit.
     */
    private String determineType(Ingredients ing) {
        if (ing.getCompose() == 1) {
            if (ing.getPv() > 0 || (ing.getLibelleVente() != null && !ing.getLibelleVente().isEmpty())) {
                return "PRODUIT_FINI";
            }
            return "PRODUIT_INTERMEDIAIRE";
        }
        return "MATIERE_PREMIERE";
    }

    /**
     * Déterminer le statut du stock.
     */
    private String determineStockStatus(Ingredients ing) {
        double stock = ing.getReste();
        double seuil = ing.getSeuil();

        if (stock <= 0) {
            return "RUPTURE";
        }
        if (seuil > 0 && stock < seuil) {
            return "CRITIQUE";
        }
        if (seuil > 0 && stock < seuil * 1.3) {
            return "BAS";
        }
        return "OK";
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
