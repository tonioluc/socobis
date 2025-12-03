<%@page import="caisse.Caisse"%>
<%@page import="vente.InsertionVente"%>
<%@page import="vente.*"%>
<%@page import="bean.TypeObjet"%>
<%@page import="user.*"%> 
<%@ page import="bean.*" %>
<%@page import="affichage.*"%>
<%@page import="utilitaire.*"%>
<%@ page import="java.sql.Connection" %>
<%@ page import="client.Client" %>
<%@ page import="faturefournisseur.ModePaiement" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%
    boolean carteExpiree = false;
        out.println("Debug: vente-changer.jsp loaded");
        UserEJB u = null;
        u = (UserEJB) session.getValue("u");
        if(u == null){
            out.println("Veuillez vous connecter pour acceder a ce contenu");
            return;
        }
        String error = (String) session.getAttribute("error");
        if(error != null){
            out.println("<div style='color:red; font-weight:bold; margin:10px;'>" + error + "</div>");
            session.removeAttribute("error");
        }
        String nomtable = "Vente_Details";
        InsertionVente mere = new InsertionVente();
        VenteDetailsLib fille = new VenteDetailsLib();
        fille.setNomTable("VENTE_DETAILS_VIDE");
        int nombreLigne = 10;
        Vente venteOrig = new Vente();
        VenteDetails[] vente_details = null;
        String idVenteOrig = request.getParameter("idVente");
        String idDetail = request.getParameter("idDetail");
        String idClient = request.getParameter("idClient");
        String changerEtPayer = request.getParameter("changerEtPayer");
        if(changerEtPayer == null || changerEtPayer.isEmpty()) changerEtPayer = "0";
        String qteRetourStr = request.getParameter("qteRetour");
        double qteRetour = 0;
        if(qteRetourStr != null && !qteRetourStr.isEmpty()) qteRetour = Double.parseDouble(qteRetourStr);

        System.out.println("DEBUG vente-changer.jsp: idVenteOrig=" + idVenteOrig + ", idDetail=" + idDetail + ", idClient=" + idClient + ", qteRetour=" + qteRetour);

        Connection c = new utilitaire.UtilDB().GetConn();
        double oldTotal = 0;
        Client clientOrig = null;
        try {
        if(idVenteOrig != null){
                venteOrig = Vente.getById(c, idVenteOrig);
                if(venteOrig == null){
                    throw new RuntimeException("Vente non trouvée pour l'ID: " + idVenteOrig);
                }
                vente_details = venteOrig.getDetailsLib(c);
                System.out.println("DEBUG vente-changer.jsp: vente_details length=" + (vente_details != null ? vente_details.length : 0));
                // Filter out null elements
                if(vente_details != null){
                    List<VenteDetails> filtered = new ArrayList<>();
                    for(VenteDetails d : vente_details){
                        if(d != null) filtered.add(d);
                    }
                    vente_details = filtered.toArray(new VenteDetails[0]);
                    System.out.println("DEBUG vente-changer.jsp: after filtering, vente_details length=" + vente_details.length);
                }
                if(vente_details != null){
                    for(VenteDetails d : vente_details){
                        System.out.println("DEBUG vente-changer.jsp: d.id=" + d.getId() + ", d.qte=" + d.getQte());
                    }
                }
                // Calculate oldTotal from details
                oldTotal = 0;
                if(vente_details != null){
                    for(VenteDetails d : vente_details){
                        double ttc = d.getPu() * d.getQte() * (1 + d.getTva()/100);
                        System.out.println("DEBUG vente-changer.jsp: d.id=" + d.getId() + ", d.pu=" + d.getPu() + ", d.qte=" + d.getQte() + ", d.tva=" + d.getTva() + ", calculated ttc=" + ttc);
                        oldTotal += ttc;
                    }
                }
                System.out.println("DEBUG vente-changer.jsp: calculated oldTotal=" + oldTotal);
                // Fix idDetail if not substituted
                if(idDetail != null && idDetail.equals("{id}") && vente_details != null && vente_details.length > 0){
                    idDetail = vente_details[0].getId().toString();
                    System.out.println("DEBUG vente-changer.jsp: Fixed idDetail to " + idDetail);
                }
                // adjust the qte for idDetail
                double reste = 0;
                if(vente_details != null){
                    for(VenteDetails d : vente_details){
                        if(d != null && d.getId() != null && d.getId().toString().equals(idDetail)){
                             reste = d.getQte() - qteRetour;

                            if(reste < 0) {
                                throw new Exception("La quantite a echange depasse la quantite de la vente precedente: " + d.getQte());
                            }

                            d.setQte(reste);
                        }
                    }
                    System.out.println("DEBUG vente-changer.jsp: Adjusted vente_details for idDetail=" + idDetail + ", qteRetour=" + qteRetour);
                    for(VenteDetails d : vente_details){
                        if(d != null && d.getId() != null && d.getId().toString().equals(idDetail)){
                            System.out.println("DEBUG vente-changer.jsp: Adjusted d.id=" + d.getId() + ", new qte=" + d.getQte());
                        }
                    }
                }
                // copy data to mere
                mere.setIdClient(idClient);
                if(idClient != null){
                    clientOrig = (Client) new Client().getById(idClient, "client", c);
                    if(clientOrig != null){
                        mere.setClientlib(clientOrig.getNom());
                    }
                }
                mere.setIdMagasin(venteOrig.getIdMagasin());
                mere.setDesignation("Correction de " + venteOrig.getDesignation());
                mere.setRemarque(venteOrig.getRemarque());
                mere.setTauxdechange(venteOrig.getTauxdechange());
                mere.setEstPrevu(venteOrig.getEstPrevu());
                mere.setModepaiement(venteOrig.getModepaiement());
                mere.setFraislivraison(venteOrig.getFraislivraison());
                mere.setModelivraison(venteOrig.getModelivraison());
                mere.setReferencefact(venteOrig.getReferencefact());
                mere.setIdVenteOrig(idVenteOrig);

                System.out.println("DEBUG vente-changer.jsp: clientOrig loaded, idClient=" + idClient + ", clientOrig.nom=" + (clientOrig != null ? clientOrig.getNom() : "null"));
                // Ensure string fields are not null to avoid NullPointerException in form setting
                if(vente_details != null){
                    for(VenteDetails d : vente_details){
                        if(d.getId() == null) d.setId("");
                        if(d.getDesignation() == null) d.setDesignation("");
                        if(d.getCompte() == null) d.setCompte("");
                        if(d.getLibelle() == null) d.setLibelle("");
                        if(d.getIdOrigine() == null) d.setIdOrigine("");
                        if(d.getIdProduit() == null) d.setIdProduit("");
                        if(d.getIdDevise() == null) d.setIdDevise("");
                        if(d.getIdbcfille() == null) d.setIdbcfille("");
                        if(d.getUnite() == null) d.setUnite("");
                    }
                }
            }
        } finally {
            if(c != null) c.close();
        }
        
        PageInsertMultiple pi = new PageInsertMultiple(mere, fille, request, nombreLigne, u);
        pi.setLien((String) session.getValue("lien"));
        Liste[] liste = new Liste[4];
        liste[0] = new Liste("idMagasin",new magasin.Magasin(),"val","id");
        //liste[1] = new Liste("idDevise",new caisse.Devise(),"val","id");
        //liste[1].setDefaut("AR");
        liste[1] = new Liste("estPrevu");
        liste[1].makeListeOuiNon();
        ModePaiement mp = new ModePaiement();
        liste[2] = new Liste("modepaiement",mp,"val","id");

        Liste listemode = new Liste("modelivraison");

        String [] affVal = new String[2];
        String [] aff = new String[2];
        aff = new String[]{"LIVRAISON","RECUPERATION"};
        affVal = new String[]{"1","2"};

        listemode.ajouterValeur(affVal,aff);
        liste[3] = listemode;

        if(request.getParameter("id")!=null && !request.getParameter("id").isEmpty()){
            if(request.getParameter("idClient")!=null && !request.getParameter("idClient").isEmpty()){
                pi.getFormu().getChamp("idClient").setDefaut(request.getParameter("idClient"));
            }

            if(request.getParameter("idPoint")!=null && !request.getParameter("idPoint").isEmpty()){
                pi.getFormu().getChamp("idMagasin").setDefaut(request.getParameter("idPoint"));
            }
        }

        pi.getFormu().changerEnChamp(liste);
        pi.getFormu().getChamp("idClient").setDefaut(idClient);
        if(clientOrig != null){
            affichage.Champ champClient = pi.getFormu().getChamp("Client");
            if(champClient != null){
                champClient.setDefaut(clientOrig.getNom());
            }
        }
        // Pré-remplir et verrouiller le magasin avec celui de la vente d'origine
        if(venteOrig != null && venteOrig.getIdMagasin() != null && !venteOrig.getIdMagasin().trim().isEmpty()){
            pi.getFormu().getChamp("idMagasin").setDefaut(venteOrig.getIdMagasin());
        }
        pi.getFormu().getChamp("idMagasin").setAutre("onchange=\"updateFille(event, 'formId')\" readonly\"");
        pi.getFormu().getChamp("etat").setVisible(false);
        pi.getFormu().getChamp("idOrigine").setVisible(false);
        pi.getFormu().getChamp("idMagasin").setLibelle("Point de vente");
        pi.getFormu().getChamp("designation").setLibelle("D&eacute;signation");
        pi.getFormu().getChamp("modepaiement").setLibelle("Mode de paiement");
        pi.getFormu().getChamp("referencefact").setLibelle("R&eacute;f&eacute;rence facture");
        pi.getFormu().getChamp("modelivraison").setLibelle("Mode de livraison");
        pi.getFormu().getChamp("fraislivraison").setLibelle("Frais de livraison (Par Kg)");
        pi.getFormu().getChamp("estPrevu").setLibelle("Est Pr&eacutevu");
        pi.getFormu().getChamp("datyPrevu").setLibelle("Date pr&eacute;visionnelle d'encaissement");
        //pi.getFormu().getChamp("designation").setDefaut("Vente particulier du "+utilitaire.Utilitaire.dateDuJour());
        pi.getFormu().getChamp("remarque").setLibelle("Remarque");
        pi.getFormu().getChamp("daty").setLibelle("Date");
        pi.getFormu().getChamp("idClient").setLibelle("Client");
        pi.getFormu().getChamp("idClient").setPageAppelComplete("client.Client","id","Client","echeance","echeancefacture");
        pi.getFormu().getChamp("idClient").setPageAppelInsert("client/client-saisie.jsp","idClient;idClientlibelle;echeance","id;nom;echeancefacture");
        pi.getFormu().getChamp("idClient").setAutre("value='" + idClient + "' readonly");
        pi.getFormu().getChamp("idClient").setVisible(false);
        pi.getFormu().getChamp("idDevise").setLibelle("Devise");
        pi.getFormu().getChamp("idDevise").setVisible(false);
        pi.getFormu().getChamp("echeancefacture").setAutre("onChange='changerValeur()'");
        //i.getFormu().getChamp("echeancefacture").setAutre("readonly");
        pi.getFormu().getChamp("echeancefacture").setLibelle("Ech&eacute;ance facture");

        if(request.getParameter("idclient")!=null){
            pi.getFormu().getChamp("idclient").setDefaut(request.getParameter("idclient"));

        }
        //affichage.Champ.setPageAppelComplete(pi.getFormufle().getChampFille("idProduit"),"annexe.ProduitLib","id","PRODUIT_LIB_MGA","puVente;puAchat;taux;val;compte;compte","pu;puAchat;tauxDeChange;designation;compte;comptelibelle");
        //affichage.Champ.setPageAppelComplete(pi.getFormufle().getChampFille("idProduit"),"produits.IngredientsLib","id","ST_INGREDIENTSAUTOVENTE","pv;compte_vente;libelle","pu;compte;designation");
        affichage.Champ.setPageAppelCompleteAWhere(pi.getFormufle().getChampFille("idProduit"),"produits.IngredientVente","id","AS_INGREDIENT_VENTE_LIB","prixunitaire;compte_vente;libelle;idunite;idunitelib","pu;compte;designation;unite;unitelib","");
        double tva = 0.0;
        if (request.getParameter("onchanged") != null && request.getParameter("onchanged").equals("true")){
            String idmagasin = request.getParameter("idMagasin");
            if(request.getParameter("idMagasin") != null && !request.getParameter("idMagasin").isEmpty()){
                session.setAttribute("idMagasin", request.getParameter("idMagasin"));
            }
            if(idmagasin == null || idmagasin.isEmpty()){
                idmagasin = (String) session.getAttribute("idMagasin");
            }
            Client client = null;
            if(request.getParameter("idClientlibelle")!=null && request.getParameter("idClientlibelle").split(" - ").length>0){
                String idclient = request.getParameter("idClientlibelle").split(" - ")[0];
                client = (Client)new Client().getById(idclient,"client",null);
                if(client == null){
                    throw new RuntimeException("Client non trouvé pour l'ID: " + idclient);
                }
                Calendar cal = Calendar.getInstance();
                cal.setTime(client.getDatecarte());
                cal.add(Calendar.YEAR, 1);
                Date dateCartePlusUnAn = cal.getTime();
                Date dateAujourdhui = new Date();
                carteExpiree = dateAujourdhui.compareTo(dateCartePlusUnAn) >= 0;
                tva = client.getTaxe();
                session.setAttribute("idclient", idclient);
            }else{
                String idclientSess = (String) session.getAttribute("idclient");
                if(idclientSess == null){
                    throw new RuntimeException("ID client manquant dans la session");
                }
                client = (Client)new Client().getById(idclientSess,"client",null);
                if(client == null){
                    throw new RuntimeException("Client non trouvé pour l'ID de session: " + idclientSess);
                }
                tva = client.getTaxe();
            }
            if(idmagasin!=null && client!=null){
                affichage.Champ.setPageAppelCompleteAWhere(pi.getFormufle().getChampFille("idProduit"),"produits.IngredientVente","id","AS_INGREDIENT_VENTE_LIB","prixunitaire;compte_vente;libelle;idunite;idunitelib","pu;compte;designation;unite;unitelib"," AND IDTYPECLIENT='"+client.getIdTypeClient()+"' AND IDMAGASIN='"+idmagasin+"'");
                //affichage.Champ.setPageAppelComplete(pi.getFormufle().getChampFille("idProduit"),"annexe.ProduitLib","id","PRODUIT_LIB_MGA","puVente;puAchat;taux;val;compte;compte","pu;puAchat;tauxDeChange;designation;compte;comptelibelle");
            }
        }else{
            session.removeAttribute("idMagasin");
            session.removeAttribute("idclient");
        }

        pi.getFormufle().getChamp("idProduit_0").setLibelle("Produit");
        pi.getFormufle().getChamp("tva_0").setLibelle("TVA (En %)");
        pi.getFormufle().getChamp("designation_0").setLibelle("Designation");
        //affichage.Champ.setPageAppelComplete(pi.getFormufle().getChampFille("compte"),"mg.cnaps.compta.ComptaCompte","compte","compta_compte","","");
        pi.getFormufle().getChamp("compte_0").setLibelle("Compte");
        pi.getFormufle().getChamp("remise_0").setLibelle("Remise (En %)");
        pi.getFormufle().getChamp("idOrigine_0").setLibelle("Origine");

        pi.getFormufle().getChamp("qte_0").setLibelle("Quantit&eacute;");
        pi.getFormufle().getChamp("pu_0").setLibelle("PU Brut");
        pi.getFormufle().getChamp("punet_0").setLibelle("PU Net");
        pi.getFormufle().getChamp("montantht_0").setLibelle("Montant HT");
        pi.getFormufle().getChamp("montantttc_0").setLibelle("Montant TTC");
        pi.getFormufle().getChampMulitple("idVente").setVisible(false);
        pi.getFormufle().getChampMulitple("id").setVisible(false);
         pi.getFormufle().getChampMulitple("idOrigine").setVisible(false);
        pi.getFormufle().getChampMulitple("puAchat").setVisible(false);
        pi.getFormufle().getChampMulitple("puVente").setVisible(false);
        pi.getFormufle().getChampMulitple("idDevise").setVisible(false);
        pi.getFormufle().getChampMulitple("idbcfille").setVisible(false);
        pi.getFormufle().getChamp("tauxDeChange_0").setLibelle("Taux de change");
        pi.getFormufle().getChamp("unitelib_0").setLibelle("Unit&eacute;");
        pi.getFormufle().getChamp("designation_0").setLibelle("D&eacute;signation ");
        pi.getFormufle().getChampMulitple("tauxDeChange").setVisible(false);
        pi.getFormufle().getChampMulitple("compte").setVisible(false);
        pi.getFormufle().getChampMulitple("unite").setVisible(false);
        if(vente_details!=null && vente_details.length>0){
            try {
                pi.setDefautFille(vente_details);
            } catch (Exception e) {
                System.out.println("ERROR in setDefautFille: " + e.getMessage());
                e.printStackTrace();
            }
            pi.getFormu().getChamp("idOrigine").setDefaut(request.getParameter("id"));
            pi.getFormu().getChamp("designation").setDefaut("Facturation de la livraison num "+request.getParameter("id"));
        }
        String[] order_form = {"daty","designation","idMagasin","idClient","remarque","idDevise","estPrevu","datyPrevu","idOrigine","etat","referencefact"};
        pi.getFormu().setOrdre(order_form);

        pi.preparerDataFormu();
        for(int i=0;i<nombreLigne;i++){
           // pi.getFormufle().getChamp("pu_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("qte_"+i).setAutre("onChange='calculerMontant("+i+")'");
            pi.getFormufle().getChamp("remise_"+i).setAutre("onChange='calculerMontant("+i+")'");
            pi.getFormufle().getChamp("tva_"+i).setAutre("onChange='calculerMontant("+i+")'");
            pi.getFormufle().getChamp("idDevise_"+i).setDefaut("AR");
            //pi.getFormufle().getChamp("compte_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("tva_"+i).setDefaut(tva+"");
            pi.getFormufle().getChamp("unitelib_"+i).setAutre("readonly");
            //pi.getFormufle().getChamp("unite_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("compte_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("punet_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("montantht_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("montantttc_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("pu_"+i).setAutre("readonly");
        }
        String[] order = {"idProduit","idOrigine", "designation","unite","unitelib", "pu", "compte", "qte", "remise","punet", "tva","montantht","montantttc" ,"tauxDeChange","idbcfille"};
        pi.getFormufle().setColOrdre(order);

        if(request.getParameter("idBC")!=null && !request.getParameter("idBC").trim().isEmpty())
        {
            BonDeCommande bc = new BonDeCommande();
            bc.setId(request.getParameter("idBC"));
            InsertionVente v = bc.createVente();
            if(v == null){
                throw new RuntimeException("Impossible de créer la vente à partir du bon de commande ID: " + request.getParameter("idBC"));
            }
            v.setDatyPrevu(null);
            if(v.getIdClient() == null){
                throw new RuntimeException("ID client manquant dans la vente créée pour le bon de commande");
            }
            Client client2 = null;
            if(v.getIdClient() != null){
                client2 = (Client)new Client().getById(v.getIdClient(),"client",null);
            }
            if(client2 == null){
                throw new RuntimeException("Client non trouvé pour l'ID: " + v.getIdClient());
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(client2.getDatecarte());
            cal.add(Calendar.YEAR, 1);
            Date dateCartePlusUnAn = cal.getTime();
            Date dateAujourdhui = new Date();
            carteExpiree = dateAujourdhui.compareTo(dateCartePlusUnAn) >= 0;
            BonDeCommandeFIlleCpl[] details = bc.getFilleBCLib();
            if (details != null && details.length > 0) {
                VenteDetailsLib[] lignes = new VenteDetailsLib[details.length];
                for (int i = 0; i < details.length; i++) {
                    BonDeCommandeFIlleCpl detail = details[i];
                    if(detail == null){
                        throw new RuntimeException("Détail du bon de commande null à l'index: " + i);
                    }
                    lignes[i] = detail.createVenteFilleLib();
                }
                pi.setDefautFille(lignes);
            }
            pi.getFormu().setDefaut(v);
        }

        //Variables de navigation
        String classeMere = "vente.InsertionVente";
        String classeFille = "vente.VenteDetails";
        String butApresPost = "vente/vente-fiche.jsp";
        String colonneMere = "idVente";
        //Preparer les affichages
        pi.getFormu().makeHtmlInsertTabIndex();
        %> <input type="hidden" name="bute" value="vente/vente-fiche.jsp"> 
        <input type="hidden" name="oldTotal" value="<%=oldTotal%>"> <%
        pi.getFormufle().makeHtmlInsertTableauIndex();

        String titre = "Enregistrement d'une facture client";
        if(request.getParameter("acte")!=null){
            titre = "Modification de la facture client";
        }
%>
<div class="content-wrapper">
    <h1><%= titre %></h1>
    <div class="box-body">
        <form id="formId" class='container' action="<%=pi.getLien()%>?but=apresMultiple.jsp" method="post" onsubmit="return validateForm()">
            <%

                out.println(pi.getFormu().getHtmlInsert());
            %>
            <input type="hidden" name="idClient" value="<%= idClient %>">
            <input type="hidden" name="oldTotal" value="<%= oldTotal %>">
            <input type="hidden" name="idVenteOrig" value="<%= idVenteOrig %>">
            <input type="hidden" name="idDetail" value="<%= idDetail %>">
            <div class="col-md-12" >
                <div class="col-md-12 cardradius">
                    <h3 class="fontinter" style="background: white;padding: 16px;margin-top: 10px;border-radius: 16px;" >Total  : <span id="montanttotal">0</span><span id="deviseLibelle"> Ar</span></h3>
                </div>
                <div id="butfillejsp">
                    <script>
                        <%
                        if(carteExpiree) {
                        %>
                        (function() {
                            if(typeof jQuery === 'undefined') {
                                window.alert('La carte du client est expiré.');
                            } else {
                                if(typeof jAlert === 'undefined') {
                                    alert('La carte du client est expiré.');
                                } else {
                                    jAlert('La carte du client est expiré.', 'Attention');
                                }
                            }
                        })();
                        <%
                        }
                        %>
                    </script>
                    <%
                        out.println(pi.getFormufle().getHtmlTableauInsert());
                    %>
                </div>

            <input name="acte" type="hidden" id="nature" value="insert">
            <input name="bute" type="hidden" id="bute" value="<%= butApresPost %>">
            <input name="classe" type="hidden" id="classe" value="<%= classeMere %>">
            <input name="classefille" type="hidden" id="classefille" value="<%= classeFille %>">
            <input name="nombreLigne" type="hidden" id="nombreLigne" value="<%= nombreLigne %>">
            <input name="colonneMere" type="hidden" id="colonneMere" value="<%= colonneMere %>">
            <input name="nomtable" type="hidden" id="nomtable" value="<%= nomtable %>">
            <input type="hidden" name="idVenteOrig" value="<%= idVenteOrig %>">
            <input type="hidden" name="oldTotal" value="<%= oldTotal %>">
            <input type="hidden" name="changerEtPayer" value="<%= changerEtPayer %>">
            <!-- Montant TTC calculé côté JS et envoyé à InsertionVente.newMontant -->
            <input type="hidden" name="newMontant" id="newMontant" value="0">
        </form>
    </div>     
</div>
<script>
   const champ = document.getElementById("echeancefacture");

    document.addEventListener("DOMContentLoaded", function () {
        const champidFournisseur = document.getElementById("idClient");
        const observer = new MutationObserver(function (mutationsList) {
            for (let mutation of mutationsList) {
                if (mutation.type === "attributes" && mutation.attributeName === "value") {
                    console.log("Nouvelle valeur :", champidFournisseur.value);
                    changerValeur();
                    // Ton code ici
                }
            }
        });

        observer.observe(champidFournisseur, { attributes: true });
    });


    champ.addEventListener("input", function () {
        console.log("La valeur a changé :", this.value);
    });

    function sanitizeNumber(str) {
        return parseFloat(str.replace(/\s/g, '').replace(',', '.')) || 0;
    }
    changerValeur();
    function changerValeur() {
        const champDaty = document.getElementById("datyPrevu");

        let jour, mois, annee;
        
        const today = new Date();
        jour = today.getDate();
        mois = today.getMonth() + 1;
        annee = today.getFullYear();
        const date = new Date(annee, mois - 1, jour);

        if (isNaN(date.getTime())) {
            alert("Date invalide !");
            return;
        }

        const nbJours = sanitizeNumber(champ.value);
        date.setDate(date.getDate() + nbJours);

        const formattedDate = [
            String(date.getDate()).padStart(2, '0'),
            String(date.getMonth() + 1).padStart(2, '0'),
            date.getFullYear()
        ].join("/");

        champDaty.value = formattedDate;
        champ.dispatchEvent(new Event("input"));
    }
    

</script>

<script>
    document.addEventListener('DOMContentLoaded', function() {
        var val = 0;
        $('input[id^="montantttc_"]').each(function() {
            var montant = parseFloat($(this).val().replace(/\s/g, ''));
            if(!isNaN(montant)){
                val += montant;
            }
        });
        $("#montanttotal").html(Intl.NumberFormat('fr-FR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(val));
        // Calculate montantttc for pre-filled lines
        for(let i = 0; i < 10; i++) {
            let qte = document.getElementById('qte_' + i);
            if(qte && qte.value && parseFloat(qte.value.replace(/\s/g, '')) > 0) {
                calculerMontant(i);
            }
        }
        const deviseSelect = document.getElementById('idDevise');
        deviseSelect.addEventListener('change', function() {
            const deviseSelectionne = this.value;
            for (let i = 0; i <= 10; i++) {
                const champDevise = document.getElementById('idDevise_' + i);
                if (champDevise) {
                    champDevise.value = deviseSelectionne;
                    if (champDevise.tagName === 'SELECT') {
                        for (let j = 0; j < champDevise.options.length; j++) {
                            if (champDevise.options[j].value === deviseSelectionne) {
                                champDevise.selectedIndex = j;
                                break;
                            }
                        }
                    }
                    const event = new Event('change');
                    champDevise.dispatchEvent(event);
                }
            }
        });
    });
</script>
<script>
    function formatNumber(number) {
        return number.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, " ");
    }
    function calculerMontant(indice) {
        // Récupérer les valeurs des champs
        var pu = parseFloat(document.getElementById('pu_' + indice).value.replace(/\s/g, '')) || 0;
        var qte = parseFloat(document.getElementById('qte_' + indice).value.replace(/\s/g, '')) || 0;
        var remise = parseFloat(document.getElementById('remise_' + indice).value.replace(/\s/g, '')) || 0;
        var tva = parseFloat(document.getElementById('tva_' + indice).value.replace(/\s/g, '')) || 0;

        // Calculer le PU Net (PU après remise)
        var punet = pu - (pu * remise / 100) + ((pu * tva / 100));

        // Calculer le Montant HT
        var montantht = (pu - (pu * remise / 100)) * qte;

        // Calculer le Montant TTC (HT + TVA)
        var montantttc = punet *  qte;

        // Mettre à jour les champs calculés
        document.getElementById('pu_' + indice).value = formatNumber(pu);
        document.getElementById('punet_' + indice).value = formatNumber(punet);
        document.getElementById('montantht_' + indice).value = formatNumber(montantht);
        document.getElementById('montantttc_' + indice).value = formatNumber(montantttc);

        var val = 0;
        $('input[id^="montantttc_"]').each(function() {
            var montant = parseFloat($(this).val().replace(/\s/g, ''));
            if(!isNaN(montant)){
                val += montant;
            }
        });
        $("#montanttotal").html(Intl.NumberFormat('fr-FR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(val));

        // stocker aussi ce total TTC dans le champ caché newMontant
        var hiddenNewMontant = document.getElementById('newMontant');
        if (hiddenNewMontant) {
            hiddenNewMontant.value = val.toFixed(2);
        }
    }

    function validateForm() {
        return true;
    }

    document.addEventListener('DOMContentLoaded', function() {
        // Validation before submit
        document.getElementById('formId').addEventListener('submit', function(e) {
            if (!validateForm()) {
                e.preventDefault();
                return false;
            }
            // à la soumission, recalculer le total et l'injecter dans newMontant pour être sûr
            var newTotal = 0;
            $('input[id^="montantttc_"]').each(function() {
                var val = $(this).val().replace(/\s/g, '').replace(',', '.');
                newTotal += parseFloat(val) || 0;
            });
            var hiddenNewMontant = document.getElementById('newMontant');
            if (hiddenNewMontant) {
                hiddenNewMontant.value = newTotal.toFixed(2);
            }
        });
    });
</script>


