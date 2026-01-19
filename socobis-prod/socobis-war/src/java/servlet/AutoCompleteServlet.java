package servlet;

import com.google.gson.Gson;

import utilitaire.AutoCompleteUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "AutoCompleteServlet", urlPatterns = {
        "/api/autocomplete",
        "/api/autocomplete/client",
        "/api/autocomplete/produit",
        "/api/autocomplete/ingredient"
})
public class AutoCompleteServlet extends HttpServlet {

    private final Gson gson = new Gson();

    /**
     * Ajoute les headers CORS à la réponse
     */
    private void addCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String servletPath = request.getServletPath();

        try {
            switch (servletPath) {
                case "/api/autocomplete/client":
                    rechercherClient(request, response);
                    break;
                case "/api/autocomplete/produit":
                    rechercherProduit(request, response);
                    break;
                case "/api/autocomplete/ingredient":
                    rechercherIngredient(request, response);
                    break;
                case "/api/autocomplete":
                default:
                    rechercherGenerique(request, response);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, e.getMessage());
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void rechercherGenerique(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String classeMapping = request.getParameter("classe");
        String nomTable = request.getParameter("table");
        String champValeur = request.getParameter("valeur");
        String champAffiche = request.getParameter("affiche");
        String recherche = request.getParameter("q");
        String useMotCleStr = request.getParameter("useMotCle");
        String aWhere = request.getParameter("where");
        String champRetour = request.getParameter("champRetour");
        String champRetourMapping = request.getParameter("champRetourMapping");

        if (classeMapping == null || classeMapping.isEmpty()) {
            sendError(response, "Paramètre 'classe' requis");
            return;
        }

        if (champValeur == null || champValeur.isEmpty()) {
            champValeur = "id";
        }

        if (champAffiche == null || champAffiche.isEmpty()) {
            champAffiche = "val";
        }

        boolean useMotCle = "true".equalsIgnoreCase(useMotCleStr);

        List<Map<String, Object>> resultats = AutoCompleteUtil.rechercher(
                classeMapping,
                nomTable,
                champValeur,
                champAffiche,
                recherche,
                useMotCle,
                aWhere,
                champRetour,
                champRetourMapping,
                null);

        sendSuccess(response, resultats);
    }

    private void rechercherClient(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String recherche = request.getParameter("q");
                System.out.println("Tonga eto ve"+recherche);
        List<Map<String, Object>> resultats = AutoCompleteUtil.rechercher(
                "client.Client",
                "CLIENT",
                "id",
                "nom",
                recherche,
                true,
                null,
                "nom;adresse;tel;email",
                null,
                null);

        System.out.println("Resultats: "+resultats.size());

        sendSuccess(response, resultats);
    }

    private void rechercherProduit(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String recherche = request.getParameter("q");

        // Utiliser la même classe et table que le JSP: produits.IngredientsLib avec AS_INGREDIENTS_PRODUIT_FINIE
        List<Map<String, Object>> resultats = AutoCompleteUtil.rechercher(
                "produits.IngredientsLib",
                "AS_INGREDIENTS_PRODUIT_FINIE",
                "id",
                "libelle",
                recherche,
                true,
                null,
                "pu;compte_vente;tva;unite;image;durre",
                null,
                null);

        sendSuccess(response, resultats);
    }

    private void rechercherIngredient(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String recherche = request.getParameter("q");

        List<Map<String, Object>> resultats = AutoCompleteUtil.rechercherSQL(
                "ST_INGREDIENTSAUTOVENTE_MIMAGE",
                "id",
                "libelle",
                recherche,
                null,
                new String[] { "pu", "compte_vente", "tva", "unite", "image", "durre" },
                null);

        sendSuccess(response, resultats);
    }

    private void sendSuccess(HttpServletResponse response, Object data) throws IOException {
        addCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        out.print(gson.toJson(result));
        out.flush();
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        addCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = response.getWriter();
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);
        out.print(gson.toJson(result));
        out.flush();
    }
}
