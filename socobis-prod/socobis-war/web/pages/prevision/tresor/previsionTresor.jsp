<%@page import="affichage.*"%>
<%@page import="prevision.*"%>
<%@page import="user.*"%>
<%@ page import="java.util.Map" %> 
<%@ page import="java.util.HashMap" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="utilitaire.UtilDB" %>
<%@ page import="java.time.LocalDate" %>

<%

    try{
        String datyDebut = request.getParameter("daty1");
        String datyFin = request.getParameter("daty2");
        if(datyDebut == null || datyDebut.equals("")) datyDebut = LocalDate.now().minusDays(1).toString();
        if(datyFin == null || datyFin.equals("")) datyFin = LocalDate.now().toString();

        PrevisionJournalierLib prevJournalier = new PrevisionJournalierLib();
        Map<String, Object> map = prevJournalier.getSoldeDebitCredit(datyDebut, datyFin);
        PrevisionJournalierLib[] previsions = (PrevisionJournalierLib[]) map.get("previsions");
        double soldeInitiale = (double) map.get("soldeinitial");
        double soldeFinale = (double) map.get("soldefinal");
        
        String[] intervalles = {"daty"};
        String[] criteres = {"daty"}; // Simples pour le formulaire
        String[] libEntete = {"daty", "recettes", "depenses", "reste"};
        String[] libEnteteAffiche = {"Date", "Recettes", "D&eacute;penses", "Solde"};
        PageRecherche pr = new PageRecherche( new PrevisionJournalierLib(), request, criteres, intervalles, 3, libEntete, libEntete.length );
    
        pr.setTitre("Pr&eacute;visions Tr&eacute;sorerie");
        pr.setUtilisateur((UserEJB) session.getValue("u"));
        pr.setLien((String) session.getValue("lien"));
    
        pr.setApres("prevision/tresor/previsionTresor.jsp");
        String[] colSomme = {"recettes", "depenses", "reste"};
        pr.creerObjetPage(libEntete, colSomme);
        
        // Set data personnalisée APRÈS creerObjetPage
        pr.getTableau().setData(previsions);
        pr.getTableau().transformerDataString();
        pr.getTableau().setLibelleAffiche(libEnteteAffiche);
        
        pr.getFormu().getChamp("daty1").setLibelle("Date D&eacute;but");
        pr.getFormu().getChamp("daty2").setLibelle("Date Fin");
    
        // Pas de liens de modification pour cette vue
        String lienTableau[] = {}; // Pas de lien clic
        // pr.getTableau().setLibelleAffiche(libEnteteAffiche);
%>  


<div class="content-wrapper">
    <section class="content-header">
        <h1><%= pr.getTitre() %></h1>
    </section>
    <section class="content">
        <div class="row">
            <div class="col-md-6">
                <div class="info-box">
                     <span class="info-box-icon bg-green"><i class="fa fa-money"></i></span>
                    <div class="info-box-content">
                        <span class="info-box-text">Solde Initial</span>
                        <span class="info-box-number"><%= soldeInitiale %></span>
                    </div>
                </div>
            </div>
            <div class="col-md-6">
                <div class="info-box">
                    <span class="info-box-icon bg-blue"><i class="fa fa-money"></i></span>
                    <div class="info-box-content">
                        <span class="info-box-text">Solde Final</span>
                        <span class="info-box-number"><%= soldeFinale %></span>
                    </div>
                </div>
            </div>
        </div>
        <form action="<%=pr.getLien()%>?but=<%= pr.getApres() %>" method="post" name="vente" id="vente">
            <%
                out.println(pr.getFormu().getHtmlEnsemble());
            %>
        </form>
        <%
            out.println(pr.getTableau().getHtml());
            out.println(pr.getBasPage());
        %>
    </section>
</div>


<% }catch(Exception e){
    e.printStackTrace();
}
%>