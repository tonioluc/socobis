<%--
    Document   : genererPlanPaiementMultiple
    Created on : 04/12/2024
    Description: Génère plusieurs prévisions à partir d'une chaîne de plan de paiement
    Format: "dd/MM/yyyy:pourcentage, dd/MM/yyyy:pourcentage, ..."
    Exemple: "07/12/2025:30, 12/12/2025:40, 30/12/2025:30"
--%>

<%@page import="prevision.Prevision"%>
<%@page import="bean.CGenUtil"%>
<%@page import="faturefournisseur.*"%>
<%@page import="user.UserEJB"%>
<%@page import="user.UserEJBBean"%>
<%@page import="utilitaire.UtilDB"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.sql.SQLException"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
    Connection c = null;
    try {
        UserEJB u = (UserEJB) session.getAttribute("u");
        String lien = (String) session.getAttribute("lien");
        String bute = request.getParameter("bute");
        String idFacture = request.getParameter("idFacture");
        String planPaiementStr = request.getParameter("planPaiementStr");
        
        // Validation des paramètres
        if (idFacture == null || idFacture.trim().isEmpty()) {
            throw new Exception("L'identifiant de la facture est requis");
        }
        
        if (planPaiementStr == null || planPaiementStr.trim().isEmpty()) {
            throw new Exception("Le plan de paiement est requis. Format: dd/MM/yyyy:pourcentage, dd/MM/yyyy:pourcentage, ...");
        }
        
        // Ouverture de la connexion
        c = new UtilDB().GetConn();
        c.setAutoCommit(false);
        
        // Récupérer la facture fournisseur
        FactureFournisseur ff = new FactureFournisseur();
        ff.setId(idFacture);
        ff = (FactureFournisseur) CGenUtil.rechercher(ff, null, null, c, " ")[0];
        
        // Vérifier si des prévisions existent déjà pour cette facture
        Prevision existingPrev = new Prevision();
        existingPrev.setIdFacture(idFacture);
        Prevision[] existingPrevisions = (Prevision[]) CGenUtil.rechercher(existingPrev, null, null, c, " ");
        
        if (existingPrevisions != null && existingPrevisions.length > 0) {
            throw new Exception("Des prévisions existent déjà pour cette facture. Veuillez les supprimer d'abord si vous souhaitez en créer de nouvelles.");
        }
        
        // Générer les prévisions à partir du plan de paiement
        Prevision[] previsions = ff.genererPrevisionsFromPlanPaiement(
            u.getUser().getTuppleID(), 
            c, 
            planPaiementStr
        );
        
        // Commit de la transaction
        c.commit();
        
        String message = previsions.length + " prévision(s) créée(s) avec succès";
        System.out.println("=== genererPlanPaiementMultiple: " + message + " pour facture " + idFacture + " ===");
        
%>
<script language="JavaScript">
    alert('<%=message%>');
    document.location.replace("<%=lien%>?but=<%=bute%>");
</script>
<%
    } catch (Exception e) {
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        e.printStackTrace();
        String errorMsg = e.getMessage().replace("'", "\\'").replace("\n", "\\n");
%>
<script language="JavaScript">
    alert('Erreur: <%=errorMsg%>');
    history.back();
</script>
<%
    } finally {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
%>
