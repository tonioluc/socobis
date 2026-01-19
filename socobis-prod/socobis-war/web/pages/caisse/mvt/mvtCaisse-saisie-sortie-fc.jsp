<%@page import="utils.ConstanteStation"%>
<%@page import="affichage.*"%>
<%@page import="caisse.MvtCaisse"%>
<%@page import="caisse.Caisse"%>
<%@page import="user.*"%>
<%@page import="java.io.*"%>
<%@page import="org.json.*"%>

<%


    try{
        System.out.println("=== mvtCaisse-saisie-sortie-fc.jsp DEBUT ===");
        
        String lien = (String) session.getValue("lien");

        UserEJB user = (UserEJB) session.getValue("u");
        MvtCaisse mouvement = new MvtCaisse();
        PageInsert pageInsert = new PageInsert( mouvement, request, user );
        pageInsert.setLien(lien);
        String  montant=request.getParameter("montant");
        String idOrigine=request.getParameter("idOrigine");
        String devise=request.getParameter("devise");
        String tiers=request.getParameter("tiers");
        String prev=request.getParameter("idPrevision");
        
        // Récupérer le taux depuis le JSON basé sur la devise
        String tauxStr = "1";
        System.out.println("=== mvtCaisse-saisie-sortie-fc.jsp devise=" + devise + " ===");
        if(devise != null && !devise.isEmpty()) {
            try {
                String jsonPath = application.getRealPath("/assets/data/devises.json");
                System.out.println("=== mvtCaisse-saisie-sortie-fc.jsp jsonPath=" + jsonPath + " ===");
                
                // Lire le fichier JSON directement
                StringBuilder content = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(jsonPath));
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                reader.close();
                
                JSONArray devises = new JSONArray(content.toString());
                for (int i = 0; i < devises.length(); i++) {
                    JSONObject dev = devises.getJSONObject(i);
                    if (dev.getString("code").equals(devise)) {
                        tauxStr = String.valueOf(dev.getDouble("taux"));
                        break;
                    }
                }
                System.out.println("=== mvtCaisse-saisie-sortie-fc.jsp tauxStr=" + tauxStr + " ===");
            } catch(Exception ex) {
                System.out.println("=== mvtCaisse-saisie-sortie-fc.jsp ERREUR: " + ex.getMessage() + " ===");
                ex.printStackTrace();
            }
        }

        affichage.Champ[] liste = new affichage.Champ[2];
        // affichage.Champ[] liste = new affichage.Champ[1];
//				liste[0] = new Liste("idDevise",new caisse.Devise(),"val","id");
				Caisse c = new Caisse();
				//c.setIdPoint(ConstanteStation.getFichierCentre());
				liste[0] = new Liste("idCaisse",c,"val","id");
        liste[1] = new Liste("idDevise", new TypeObjet("DEVISE"), "val" ,"id");
			
        pageInsert.getFormu().changerEnChamp(liste);
				pageInsert.getFormu().getChamp("designation").setDefaut("Paiement de la facture : "+idOrigine);
        pageInsert.getFormu().getChamp("idCaisse").setLibelle("Caisse");
        pageInsert.getFormu().getChamp("idDevise").setLibelle("Devise");
        pageInsert.getFormu().getChamp("designation").setLibelle("D&eacute;signation");
        pageInsert.getFormu().getChamp("debit").setLibelle("D&eacute;bit");
        pageInsert.getFormu().getChamp("daty").setLibelle("Date");
        pageInsert.getFormu().getChamp("idtiers").setLibelle("Fournisseur");
        pageInsert.getFormu().getChamp("idDevise").setDefaut(devise);
        // pageInsert.getFormu().getChamp("idDevise").setAutre("readonly");
        pageInsert.getFormu().getChamp("taux").setDefaut("1");
        pageInsert.getFormu().getChamp("taux").setAutre("readonly");
        pageInsert.getFormu().getChamp("idVirement").setVisible(false);
        pageInsert.getFormu().getChamp("idVenteDetail").setVisible(false);
        pageInsert.getFormu().getChamp("idOp").setVisible(false);
        pageInsert.getFormu().getChamp("idOrigine").setVisible(false);
				pageInsert.getFormu().getChamp("credit").setVisible(false);
        pageInsert.getFormu().getChamp("etat").setVisible(false);
				pageInsert.getFormu().getChamp("idOrigine").setDefaut(idOrigine);
				pageInsert.getFormu().getChamp("idOrigine").setVisible(false);
        pageInsert.getFormu().getChamp("debit").setDefaut(montant);
				pageInsert.getFormu().getChamp("idTiers").setDefaut(tiers);
        //pageInsert.getFormu().getChamp("idTiers").setVisible(false);
        pageInsert.getFormu().getChamp("idPrevision").setLibelle("Pr&eacute;vision");
        pageInsert.getFormu().getChamp("idPrevision").setDefaut(prev);
        //pageInsert.getFormu().getChamp("idPrevision").setPageAppelComplete("prevision.Prevision", "id", "PREVISION");
        pageInsert.getFormu().getChamp("compte").setLibelle("Compte de regroupement");
        String[] ordre={"daty"};
        pageInsert.getFormu().setOrdre(ordre);

        String classe = "caisse.MvtCaisse";
        String nomTable = "MOUVEMENTCAISSE";
        String butApresPost = "caisse/mvt/mvtCaisse-fiche.jsp";

        pageInsert.preparerDataFormu();
        pageInsert.getFormu().makeHtmlInsertTabIndex();

%>

    <div class="content-wrapper">
        <h1 align="center">D&eacute;caissement</h1>
        <form action="<%=pageInsert.getLien()%>?but=apresTarif.jsp" method="post"  data-parsley-validate>
            <%
                out.println(pageInsert.getFormu().getHtmlInsert());
            %>
            <input name="acte" type="hidden" id="nature" value="insert">
            <input name="bute" type="hidden" id="bute" value="<%= butApresPost %>">
            <input name="classe" type="hidden" id="classe" value="<%= classe %>">
            <input name="nomtable" type="hidden" id="nomtable" value="<%= nomTable %>">
        </form>
    </div>

<%

    }catch(Exception e){
        out.println("<div class='alert alert-danger'>Erreur: " + e.getMessage() + "</div>");
        e.printStackTrace();
    }

%>
<jsp:include page='../../taux.jsp'/>