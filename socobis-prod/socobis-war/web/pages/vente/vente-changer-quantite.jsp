<%@page import="caisse.Caisse"%>
<%@page import="vente.InsertionVente"%>
<%@page import="vente.*"%>
<%@page import="bean.TypeObjet"%>
<%@page import="user.*"%> 
<%@ page import="bean.*" %>
<%@page import="affichage.*"%>
<%@page import="utilitaire.*"%>
<%@ page import="client.Client" %>
<%@ page import="faturefournisseur.ModePaiement" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.sql.Connection" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    try {
        UserEJB u = null;
        u = (UserEJB) session.getValue("u");
        String idVenteOrig = request.getParameter("idVente");
        String idDetail = request.getParameter("idDetail");
        
        // Fix: if {id} was not substituted, use the 'id' parameter instead
        if(idDetail != null && idDetail.equals("{id}")) {
            idDetail = request.getParameter("id");
        }
        
        String idClient = request.getParameter("idClient");
        String changerEtPayer = request.getParameter("changerEtPayer");
%>
<div class="content-wrapper">
    <h1 class="box-title">Changer le détail de vente</h1>
    <div class="row m-0">
        <div class="col-md-6">
            <div class="box-fiche">
                <div class="box">
                    <div class="box-body">
                        <form action="<%= (String) session.getValue("lien") %>?but=vente/vente-changer.jsp" method="post">
                            <input type="hidden" name="idVente" value="<%= idVenteOrig %>">
                            <input type="hidden" name="idDetail" value="<%= idDetail %>">
                            <input type="hidden" name="idClient" value="<%= idClient %>">
                            <input type="hidden" name="changerEtPayer" value="<%= changerEtPayer %>">
                            <div class="form-group">
                                <label for="qteRetour">Quantité à retourner/changer :</label>
                                <input type="number" class="form-control" id="qteRetour" name="qteRetour" min="0" step="0.01" required>
                            </div>
                            <button type="submit" class="btn btn-primary">Continuer</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<%
    } catch (Exception e) {
        e.printStackTrace();
%>
    <script language="JavaScript">
        alert('<%=e.getMessage()%>');
        history.back();
    </script>
<% } %>