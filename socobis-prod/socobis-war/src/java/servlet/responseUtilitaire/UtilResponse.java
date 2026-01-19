package servlet.responseUtilitaire;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import bean.ClassMAPTable;
import user.UserEJB;

public class UtilResponse {
    private static final Gson gson = new Gson();

    /**
     * Ajoute les headers CORS à la réponse
     */
    public static void addCorsHeaders(HttpServletResponse response) {
        // En développement, permettre tous les origins
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    /**
     * Remplit un objet ClassMAPTable depuis un JsonObject
     */
    public static void remplirObjetDepuisJson(ClassMAPTable obj, JsonObject json) throws Exception {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement element = entry.getValue();
            if (element != null && !element.isJsonNull()) {
                String value = element.isJsonPrimitive() ? element.getAsString() : element.toString();
                if (value != null && !value.isEmpty() && !value.equals("null")) {
                    try {
                        obj.setValChamp(key, value);
                        System.out.println("Setting field '" + key + "' to value: " + value);
                    } catch (Exception e) {
                        // Ignorer les champs qui ne peuvent pas être set (champ inexistant, etc.)
                        System.out.println("Warning: Cannot set field '" + key + "' on " + obj.getClass().getName()
                                + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Récupère une chaîne depuis un JsonObject avec valeur par défaut
     */
    public static String getJsonString(JsonObject json, String key, String defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }

    /**
     * Vérifier si la session contient l'UserEJB
     */
    public static void checkSession(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false); // false pour ne pas créer de session
        if (session == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("hasSession", false);
            data.put("hasUserEJB", false);
            data.put("sessionId", "null");
            sendSuccess(response, data, "Pas de session");
            return;
        }

        UserEJB u = (UserEJB) session.getAttribute("u");
        boolean hasUserEJB = (u != null);
        String sessionId = session.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("hasSession", true);
        data.put("hasUserEJB", hasUserEJB);
        data.put("sessionId", sessionId);
        if (hasUserEJB) {
            data.put("userClass", u.getClass().getName());
            try {
                data.put("userName", u.getUser().getLoginuser());
            } catch (Exception e) {
                data.put("userName", "error: " + e.getMessage());
            }
        }

        sendSuccess(response, data, "Vérification de session");
    }

    public static void sendSuccess(HttpServletResponse response, Map<String, Object> data, String message)
            throws IOException {
        addCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        result.put("message", message);
        out.print(gson.toJson(result));
        out.flush();
    }

    public static void sendError(HttpServletResponse response, String message) throws IOException {
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
