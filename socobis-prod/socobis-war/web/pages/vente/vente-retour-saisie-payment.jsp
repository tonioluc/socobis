
<%@page import="avoir.AvoirFC"%>
<%@page import="avoir.AvoirFCFille"%>
<%@page import="bean.TypeObjet"%>
<%@page import="user.*"%>
<%@ page import="bean.*" %>
<%@page import="affichage.*"%>
<%@page import="utilitaire.*"%>
<%@page import="vente.*"%>
<%
    try {
        UserEJB u = null;
        u = (UserEJB) session.getValue("u");
        String titre = "";
        if(request.getParameter("acte") != null && !request.getParameter("acte").isEmpty()){
            titre = "Modification d'une facture d'avoir";
        }else{
            titre="Enregistrement d'un retour client";
        }
        VenteDetails[] venteDetails = null;
        Vente vente = null;
        if(request.getParameter("id")!=null){
            System.out.println("ok");
            VenteDetails venteDetail = new VenteDetails();
            venteDetail.setId(request.getParameter("id"));

            venteDetails = (VenteDetails[]) CGenUtil.rechercher(venteDetail, null, null, "");

            if (venteDetails.length >0){
                vente = (Vente) CGenUtil.rechercher(new Vente(),null,null," AND ID = '"+ venteDetails[0].getIdVente()+"'")[0];
            }
        }

        VenteRetourClient mere = new VenteRetourClient();
        VenteRetourDetail fille = new VenteRetourDetail();
        int nombreLigne = 5;
        PageInsertMultiple pi = new PageInsertMultiple(mere, fille, request, nombreLigne, u);
        pi.setLien((String) session.getValue("lien"));

        Liste[] liste = new Liste[2];
        liste[0] = new Liste("idMagasin",new magasin.Magasin(),"val","id");

        TypeObjet motif = new TypeObjet();
        motif.setNomTable("TYPEVENTERETOUR");
        liste[1] = new Liste("idTypeMotif",motif,"val","id");

        pi.getFormu().changerEnChamp(liste);
        pi.getFormu().getChamp("idMagasin").setLibelle("Point de vente");
        pi.getFormu().getChamp("designation").setLibelle("D&eacute;signation");
        pi.getFormu().getChamp("remarque").setLibelle("Remarque");
        pi.getFormu().getChamp("daty").setLibelle("Date");
        pi.getFormu().getChamp("idTypeMotif").setLibelle("Motif");
        pi.getFormu().getChamp("idClient").setLibelle("Client");
        pi.getFormu().getChamp("idVente").setLibelle("ID Facture client");
        pi.getFormu().getChamp("idVenteDetail").setLibelle("ID vente vendu");
        pi.getFormu().getChamp("idClient").setPageAppelComplete("client.Client","id","Client");
        pi.getFormu().getChamp("idClient").setPageAppelInsert("client/client-saisie.jsp","idClient;idClientlibelle","id;nom");
        pi.getFormu().getChamp("dateRetour").setLibelle("Date de retour");
        pi.getFormu().getChamp("qte").setLibelle("Quantite a retourner");
        pi.getFormu().getChamp("pu").setLibelle("prix unitaire");
        pi.getFormu().getChamp("idVente").setAutre("readonly");
        pi.getFormu().getChamp("idVenteDetail").setAutre("readonly");
        pi.getFormu().getChamp("etat").setVisible(false);
        //fille
        affichage.Champ.setPageAppelComplete(pi.getFormufle().getChampFille("idProduit"),"produits.IngredientVente","id","AS_INGREDIENT_VENTE_LIB","prixunitaire;compte_vente;libelle;idunite;idunitelib","pu;compte;designation;unite;unitelib");
        // affichage.Champ.setPageAppelComplete(pi.getFormufle().getChampFille("idProduit"),"annexe.ProduitLib","id","PRODUIT_LIB_MGA","puVente;puAchat;taux;val","pu;puAchat;tauxDeChange;designation");
        affichage.Champ.setPageAppelInsert(pi.getFormufle().getChampFille("idProduit"),"annexe/produit/produit-saisie.jsp","id;val");

        pi.getFormufle().getChamp("idProduit_0").setLibelle("Produit");
        pi.getFormufle().getChamp("tva_0").setLibelle("TVA");
        pi.getFormufle().getChamp("designation_0").setLibelle("Designation");
        pi.getFormufle().getChamp("idOrigine_0").setLibelle("Origine");
        pi.getFormufle().getChamp("pu_0").setLibelle("Montant");
        pi.getFormufle().getChamp("designation_0").setLibelle("D&eacute;signation");
        pi.getFormufle().getChamp("tauxDeChange_0").setLibelle("Taux de change");
        pi.getFormufle().getChamp("qte_0").setLibelle("Quantit&eacute;");
        String[] ordre={"daty"};
        pi.getFormu().setOrdre(ordre);
        //pi.getFormufle().getChampMulitple("tauxDeChange").setAutre("readonly");
        pi.preparerDataFormu();
        for(int i=0;i<nombreLigne;i++){
            // pi.getFormufle().getChamp("pu_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("qte_"+i).setDefaut("0");
            pi.getFormufle().getChamp("tva_"+i).setDefaut("0");
            pi.getFormufle().getChamp("tauxDeChange_"+i).setDefaut("0");
            pi.getFormufle().getChamp("idDevise_"+i).setDefaut("AR");
        }
        pi.getFormufle().getChampMulitple("id").setVisible(false);
        pi.getFormufle().getChampMulitple("idVenteRetour").setVisible(false);
        pi.getFormufle().getChampMulitple("idDevise").setVisible(false);


        if(venteDetails!=null){
            pi.getFormu().getChamp("idVente").setDefaut(vente.getId());
            System.out.println("ID MADAGASIN="+vente.getIdMagasin());
            if(vente.getIdMagasin()!=null)
                pi.getFormu().getChamp("idMagasin").setDefaut(String.valueOf(vente.getIdMagasin()));


            pi.getFormu().getChamp("idClient").setDefaut(vente.getIdClient());
            pi.getFormu().getChamp("idVenteDetail").setDefaut(venteDetails[0].getId());
            pi.getFormu().getChamp("designation").setDefaut(venteDetails[0].getDesignation());
            //pi.getFormu().getChamp("qte").setDefaut(String.valueOf(venteDetails[0].getQte()));
            pi.getFormu().getChamp("qte").setDefaut(String.valueOf(0));
            pi.getFormu().getChamp("pu").setDefaut(String.valueOf(venteDetails[0].getPu()));
            pi.getFormu().getChamp("pu").setAutre("readonly");
            System.out.println("tonga eto "+venteDetails.length + " nombre +" +nombreLigne );
            for (int i = 0; i < venteDetails.length; i++) {
                pi.getFormufle().getChamp("idProduit_"+i).setDefaut(venteDetails[i].getIdProduit());
                pi.getFormufle().getChamp("pu_"+i).setDefaut(String.valueOf(venteDetails[i].getPu()));
                pi.getFormufle().getChamp("tva_"+i).setDefaut(String.valueOf(venteDetails[i].getTva()));
                pi.getFormufle().getChamp("tauxDeChange_"+i).setDefaut(String.valueOf(venteDetails[i].getTauxDeChange()));
                pi.getFormufle().getChamp("designation_"+i).setDefaut(venteDetails[i].getDesignation());
                //pi.getFormufle().getChamp("qte_"+i).setDefaut(String.valueOf(venteDetails[i].getQte()));
                pi.getFormufle().getChamp("qte_"+i).setDefaut(String.valueOf(0));
            }
        }
        //Variables de navigation
        String classeMere = "vente.VenteRetourClient";
        String classeFille = "vente.VenteRetourDetail";
        String butApresPost = "vente/fiche-vente-retour-payment.jsp";
        String colonneMere = "idVenteRetour";
        //Preparer les affichages
        pi.getFormu().makeHtmlInsertTabIndex();
        pi.getFormufle().makeHtmlInsertTableauIndex();

%>
<div class="content-wrapper">
    <div class="row">
        <div class="col-md-12">
            <div class="box-fiche">
                <div class="box">
                    <div class="box-title with-border">
                        <h1><%= titre %></h1>
                    </div>
                    <div class="box-body">
                        <form class='container' action="<%=pi.getLien()%>?but=apresMultiple.jsp" method="post" >
                            <%

                                out.println(pi.getFormu().getHtmlInsert());
                            %>
                            <%
                                out.println(pi.getFormufle().getHtmlTableauInsert());
                            %>

                            <input name="acte" type="hidden" id="nature" value="insert">
                            <input name="bute" type="hidden" id="bute" value="<%= butApresPost %>">
                            <input name="classe" type="hidden" id="classe" value="<%= classeMere %>">
                            <input name="classefille" type="hidden" id="classefille" value="<%= classeFille %>">
                            <input name="nombreLigne" type="hidden" id="nombreLigne" value="<%= nombreLigne %>">
                            <input name="colonneMere" type="hidden" id="colonneMere" value="<%= colonneMere %>">
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
<% }%>

