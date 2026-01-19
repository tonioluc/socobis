package servlet.login;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import servlet.responseUtilitaire.UtilResponse;
import user.UserEJB;
import user.UserEJBClient;

public class LoginRestServlet {
    /**
     * Login REST - permet au frontend de créer une session
     */
    public static void doLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Lire le corps JSON
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String jsonBody = sb.toString();
            System.out.println("Login request: " + jsonBody);

            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(jsonBody).getAsJsonObject();

            String username = UtilResponse.getJsonString(json, "username", null);
            String password = UtilResponse.getJsonString(json, "password", null);
            String interim = UtilResponse.getJsonString(json, "interim", null);
            String service = UtilResponse.getJsonString(json, "service", null);

            if (username == null || password == null) {
                UtilResponse.sendError(response, "username et password requis");
                return;
            }

            System.out.println("Tentative de login pour: " + username);

            UserEJB u = UserEJBClient.lookupUserEJBBeanLocal();
            System.out.println("EJB lookup successful");

            u.testLogin(username, password, interim, service);
            System.out.println("testLogin completed");

            System.out.println("u.getU(): " + u.getU());
            if (u.getU() != null) {
                System.out.println("User ID: " + u.getU().getRefuser());
                System.out.println("User login: " + u.getU().getLoginuser());
            }

            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(216000);
            session.setAttribute("u", u);
            session.setAttribute("user", u.getU());
            session.setAttribute("config", u.findConfiguration());

            System.out.println("Session attributes set");

            Map<String, Object> data = new HashMap<>();
            data.put("sessionId", session.getId());
            if (u.getU() != null) {
                data.put("userName", u.getU().getLoginuser());
                data.put("userRole", u.getU().getIdrole());
            }

            UtilResponse.sendSuccess(response, data, "Login réussi");

        } catch (Exception e) {
            e.printStackTrace();
            UtilResponse.sendError(response, "Erreur de login: " + e.getMessage());
        }
    }

    public static UserEJB getUserEbj(HttpServletRequest request, HttpServletResponse response) throws Exception {
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
                return null;
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
                    return null;
                }
            } else {
                // AUTO-INITIALISATION avec admin/paop si aucun utilisateur en session
                System.out.println("No user in session, creating admin user directly");
                try {
                    // Créer directement l'utilisateur admin sans passer par testLogin
                    historique.MapUtilisateurServiceDirection adminUser = new historique.MapUtilisateurServiceDirection();
                    adminUser.setRefuser(1060); // ID de l'utilisateur admin
                    adminUser.setLoginuser("admin");
                    adminUser.setPwduser("paop");
                    adminUser.setNomuser("Administrateur");
                    adminUser.setAdruser("DIR42");
                    adminUser.setTeluser("03444044044");
                    adminUser.setIdrole("dg");
                    adminUser.setRang(6);
                    // Stocker directement dans l'EJB
                    u.setU(adminUser);

                    // Stocker aussi en session pour cohérence
                    session.setAttribute("u", u);
                    session.setAttribute("user", adminUser);
                    session.setAttribute("config", u.findConfiguration());

                    System.out.println("Admin user initialized directly in EJB");
                    return u;
                } catch (Exception e) {
                    System.out.println("Direct user initialization failed: " + e.getMessage());
                    UtilResponse.sendError(response,
                            "Erreur lors de l'initialisation directe utilisateur: " + e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }
}
