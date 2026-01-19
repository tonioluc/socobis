package utilitaire;

import bean.CGenUtil;
import bean.ClassMAPTable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe utilitaire générique pour gérer l'autocomplétion
 * Basée sur le pattern utilisé dans affichage.Champ
 * 
 * @author AutoComplete System
 */
public class AutoCompleteUtil {

    private static final int MAX_RESULTS = 20;

    public static List<Map<String, Object>> rechercher(
            String classeMapping,
            String nomTable,
            String champValeur,
            String champAffiche,
            String recherche,
            boolean useMotCle,
            String aWhere,
            Connection c) throws Exception {

        return rechercher(classeMapping, nomTable, champValeur, champAffiche,
                recherche, useMotCle, aWhere, null, null, c);
    }

    public static List<Map<String, Object>> rechercher(
            String classeMapping,
            String nomTable,
            String champValeur,
            String champAffiche,
            String recherche,
            boolean useMotCle,
            String aWhere,
            String champRetour,
            String champRetourMapping,
            Connection c) throws Exception {

        boolean canClose = false;
        if (c == null) {
            c = new UtilDB().GetConn();
            canClose = true;
        }

        List<Map<String, Object>> resultats = new ArrayList<>();

        try {
            Class<?> clazz = Class.forName(classeMapping);
            ClassMAPTable instance = (ClassMAPTable) clazz.getDeclaredConstructor().newInstance();

            if (nomTable != null && !nomTable.isEmpty())
                instance.setNomTable(nomTable);

            String whereClause = construireWhere(instance, recherche, useMotCle, aWhere);

            whereClause += " AND ROWNUM <= " + MAX_RESULTS;

            Object[] liste = CGenUtil.rechercher(instance, null, null, c, whereClause);

            String getterAffiche = "get" + capitalizeFirst(champAffiche);
            String getterValeur = "get" + capitalizeFirst(champValeur);

            String[] champsRetour = null;
            if (champRetour != null && !champRetour.isEmpty()) {
                champsRetour = champRetour.split(";");
            }

            // Transformer les résultats
            for (Object obj : liste) {
                Map<String, Object> item = new HashMap<>();

                // Valeur principale
                Method methodValeur = getMethod(clazz, getterValeur);
                Object valeur = methodValeur.invoke(obj);
                item.put("value", valeur != null ? valeur.toString() : "");

                // Libellé à afficher
                Method methodAffiche = getMethod(clazz, getterAffiche);
                Object affiche = methodAffiche.invoke(obj);
                String libelleStr = affiche != null ? affiche.toString() : "";
                // Afficher "ID - libelle" dans le label
                String idStr = valeur != null ? valeur.toString() : "";
                item.put("label", idStr + " - " + libelleStr);
                // Garder la designation originale (sans ID)
                item.put("designation", libelleStr);

                // ID
                item.put("id", valeur != null ? valeur.toString() : "");

                // Champs retour supplémentaires
                if (champsRetour != null) {
                    StringBuilder retourBuilder = new StringBuilder();
                    for (int i = 0; i < champsRetour.length; i++) {
                        String champ = champsRetour[i].trim();
                        String getter = "get" + capitalizeFirst(champ);
                        try {
                            Method method = getMethod(clazz, getter);
                            Object val = method.invoke(obj);
                            retourBuilder.append(val != null ? val.toString() : "");
                        } catch (Exception e) {
                            retourBuilder.append("");
                        }
                        if (i < champsRetour.length - 1) {
                            retourBuilder.append(";");
                        }
                    }
                    item.put("retour", retourBuilder.toString());
                }

                resultats.add(item);
            }

        } catch (ClassNotFoundException e) {
            throw new Exception("Classe non trouvée: " + classeMapping);
        } catch (Exception e) {
            throw e;
        } finally {
            if (canClose && c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                }
            }
        }

        return resultats;
    }

    /**
     * Recherche par requête SQL directe
     */
    public static List<Map<String, Object>> rechercherSQL(
            String nomTable,
            String champValeur,
            String champAffiche,
            String recherche,
            String aWhere,
            String[] champsSupplementaires,
            Connection c) throws Exception {

        boolean canClose = false;
        if (c == null) {
            c = new UtilDB().GetConn();
            canClose = true;
        }

        List<Map<String, Object>> resultats = new ArrayList<>();
        Statement st = null;
        ResultSet rs = null;

        try {
            StringBuilder colonnes = new StringBuilder();
            colonnes.append(champValeur).append(", ").append(champAffiche);
            if (champsSupplementaires != null) {
                for (String champ : champsSupplementaires) {
                    colonnes.append(", ").append(champ);
                }
            }

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(colonnes).append(" FROM ").append(nomTable);
            sql.append(" WHERE 1=1");

            if (recherche != null && !recherche.isEmpty()) {
                // Recherche par ID OU par libellé
                sql.append(" AND (UPPER(").append(champValeur).append(") LIKE UPPER('%")
                        .append(escapeSQL(recherche)).append("%')");
                sql.append(" OR UPPER(").append(champAffiche).append(") LIKE UPPER('%")
                        .append(escapeSQL(recherche)).append("%'))");
            }

            if (aWhere != null && !aWhere.isEmpty()) {
                sql.append(" ").append(aWhere);
            }

            sql.append(" AND ROWNUM <= ").append(MAX_RESULTS);

            st = c.createStatement();
            rs = st.executeQuery(sql.toString());

            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                String id = rs.getString(champValeur);
                String libelle = rs.getString(champAffiche);
                item.put("value", id);
                // Afficher "ID - libelle" dans le label
                item.put("label", id + " - " + libelle);
                // Garder la designation originale (sans ID)
                item.put("designation", libelle);
                item.put("id", id);

                if (champsSupplementaires != null) {
                    StringBuilder retour = new StringBuilder();
                    for (int i = 0; i < champsSupplementaires.length; i++) {
                        String val = rs.getString(champsSupplementaires[i]);
                        retour.append(val != null ? val : "");
                        if (i < champsSupplementaires.length - 1) {
                            retour.append(";");
                        }
                    }
                    item.put("retour", retour.toString());
                }

                resultats.add(item);
            }

        } finally {
            if (rs != null)
                rs.close();
            if (st != null)
                st.close();
            if (canClose && c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                }
            }
        }

        return resultats;
    }

    private static String construireWhere(ClassMAPTable instance, String recherche,
            boolean useMotCle, String aWhere) throws Exception {
        StringBuilder where = new StringBuilder();

        if (recherche != null && !recherche.isEmpty()) {
            // Recherche par ID (colonne id) OU par mots-clés
            String searchTerm = escapeSQL(recherche).toUpperCase();
            where.append(" AND (UPPER(id) LIKE '%").append(searchTerm).append("%'");
            
            if (useMotCle) {
                String whereMotCle = CGenUtil.makeWhereMotsCles(instance, recherche);
                if (whereMotCle != null && !whereMotCle.isEmpty()) {
                    where.append(" OR (").append(whereMotCle).append(")");
                }
            } else {
                String whereSimple = CGenUtil.makeWhereOr(instance);
                if (whereSimple != null && !whereSimple.isEmpty()) {
                    where.append(" OR (").append(whereSimple).append(")");
                }
            }
            where.append(")");
        }

        if (aWhere != null && !aWhere.isEmpty()) {
            where.append(" ").append(aWhere);
        }

        return where.toString();
    }

    private static Method getMethod(Class<?> clazz, String methodName) throws Exception {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            Class<?> parent = clazz.getSuperclass();
            while (parent != null && parent != Object.class) {
                try {
                    return parent.getMethod(methodName);
                } catch (NoSuchMethodException ex) {
                    parent = parent.getSuperclass();
                }
            }
            throw e;
        }
    }

    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String escapeSQL(String str) {
        if (str == null)
            return "";
        return str.replace("'", "''").replace("\\", "\\\\");
    }
}
