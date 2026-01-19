<%-- 
    Document   : vente-saisie
    Created on : 22 mars 2024, 14:37:44
    Author     : Angela
--%>


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
<%@ page import="prevision.DeviseJson" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    boolean carteExpiree = false;
    try {
        UserEJB u = null;
        u = (UserEJB) session.getValue("u");
        String nomtable = "Vente_Details";
        InsertionVente mere = new InsertionVente();
        VenteDetailsLib fille = new VenteDetailsLib();
        fille.setNomTable("VENTE_DETAILS_VIDE");
        int nombreLigne = 10;
        As_BondeLivraisonClient blf = new As_BondeLivraisonClient();
        VenteDetails[] vente_details = null;
        if(request.getParameter("id")!=null){
            blf.setId(request.getParameter("id"));
            vente_details = blf.getListeVenteDetails("AS_BC_FILLE_AVEC_PRIX",null);
        }
        
        // Récupérer le chemin de base pour le fichier JSON des devises
        String basePath = application.getRealPath("/");
        String dateJour = utilitaire.Utilitaire.dateDuJour();
        
        // Récupérer TOUTES les devises depuis le JSON
        List<DeviseJson> toutesDevises = DeviseJson.getAllDevises(basePath);
        String devisesJson = DeviseJson.getDevisesJsonForJs(basePath);
        
        // Construire les tableaux pour la liste déroulante des devises
        int nbDevises = toutesDevises.size();
        if(nbDevises == 0) {
            toutesDevises.add(new DeviseJson("AR", "Ariary", 1.0, "2025-01-01", "2035-12-31"));
            nbDevises = 1;
        }
        
        String[] libellesDevises = new String[nbDevises];
        String[] codesDevises = new String[nbDevises];
        for(int i = 0; i < nbDevises; i++) {
            DeviseJson d = toutesDevises.get(i);
            codesDevises[i] = d.getCode() + "|" + d.getDateDebut() + "|" + d.getDateFin();
            libellesDevises[i] = d.getCode() + " (" + d.getDateDebut() + " au " + d.getDateFin() + ")";
        }
        
        PageInsertMultiple pi = new PageInsertMultiple(mere, fille, request, nombreLigne, u);
        pi.setLien((String) session.getValue("lien"));
        Liste[] liste = new Liste[5];
        liste[0] = new Liste("idMagasin",new magasin.Magasin(),"val","id");
        // Utiliser la liste des devises JSON au lieu de la table DB
        liste[4] = new Liste("idDevise", libellesDevises, codesDevises);
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
        pi.getFormu().getChamp("idMagasin").setAutre("onchange=\"updateFille(event, 'formId')\"");
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
        pi.getFormu().getChamp("detailVenteMultiple").setLibelle("Date multiple");
        pi.getFormu().getChamp("detailVenteMultiple").setDefaut("");
        //pi.getFormu().getChamp("designation").setDefaut("Vente particulier du "+utilitaire.Utilitaire.dateDuJour());
        pi.getFormu().getChamp("remarque").setLibelle("Remarque");
        pi.getFormu().getChamp("daty").setLibelle("Date");
        pi.getFormu().getChamp("idClient").setLibelle("Client");
        pi.getFormu().getChamp("idClient").setPageAppelComplete("client.Client","id","Client","echeance","echeancefacture");
        pi.getFormu().getChamp("idClient").setPageAppelInsert("client/client-saisie.jsp","idClient;idClientlibelle;echeance","id;nom;echeancefacture");
        pi.getFormu().getChamp("idClient").setAutre("onchange=\"updateFille(event, 'formId')\"");
        pi.getFormu().getChamp("idDevise").setLibelle("Devise");
        pi.getFormu().getChamp("idDevise").setAutre("onChange='deviseModification()'");
        // Devise par défaut AR
        String defautDeviseValue = "AR|2025-01-01|2035-12-31";
        double defautTauxChange = 1.0;
        
        // Si devise passée en paramètre (lors de rechargement), la conserver
        String deviseParam = request.getParameter("idDevise");
        if(deviseParam != null && !deviseParam.isEmpty()) {
            // La devise vient du formulaire, la conserver
            defautDeviseValue = deviseParam;
            // Extraire le code et trouver le taux correspondant
            String codeDevise = deviseParam.contains("|") ? deviseParam.split("\\|")[0] : deviseParam;
            for(DeviseJson d : toutesDevises) {
                if(d.getCode().equals(codeDevise)) {
                    defautTauxChange = d.getTaux();
                    // S'assurer que la valeur est au bon format
                    defautDeviseValue = d.getCode() + "|" + d.getDateDebut() + "|" + d.getDateFin();
                    break;
                }
            }
        } else {
            // Devise par défaut AR
            for(DeviseJson d : toutesDevises) {
                if(d.getCode().equals("AR")) {
                    defautDeviseValue = d.getCode() + "|" + d.getDateDebut() + "|" + d.getDateFin();
                    defautTauxChange = d.getTaux();
                    break;
                }
            }
        }
        pi.getFormu().getChamp("idDevise").setDefaut(defautDeviseValue);
        pi.getFormu().getChamp("echeancefacture").setAutre("onChange='changerValeur()'");
        //i.getFormu().getChamp("echeancefacture").setAutre("readonly");
        pi.getFormu().getChamp("echeancefacture").setLibelle("Ech&eacute;ance facture");

        if(request.getParameter("idclient")!=null){
            pi.getFormu().getChamp("idclient").setDefaut(request.getParameter("idclient"));

        }
        // affichage.Champ.setPageAppelComplete(pi.getFormufle().getChampFille("idProduit"),"annexe.ProduitLib","id","PRODUIT_LIB_MGA","puVente;puAchat;taux;val;compte;compte","pu;puAchat;tauxDeChange;designation;compte;comptelibelle");
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
            Client c = null;
            String clientLibelle = request.getParameter("idClientlibelle");
            if(clientLibelle != null){
                String[] parts = clientLibelle.split(" - ");
                if(parts.length > 1 && parts[0] != null && !parts[0].trim().isEmpty()){
                    String idclient = parts[0].trim();
                    c = (Client)new Client().getById(idclient,"client",null);
                    if(c != null){
                        session.setAttribute("idclient", idclient);
                    }
                }
            }
            if(idmagasin!=null && c!=null){
                //affichage.Champ.setPageAppelCompleteAWhere(pi.getFormufle().getChampFille("idProduit"),"produits.IngredientVente","id","AS_INGREDIENT_VENTE_LIB","prixunitaire;compte_vente;libelle;idunite;idunitelib","pu;compte;designation;unite;unitelib"," AND IDTYPECLIENT='"+c.getIdTypeClient()+"' AND IDMAGASIN='"+idmagasin+"'");
                // Utiliser une vue unique - le taux est géré par le JSON, ne pas le mapper ici
                affichage.Champ.setPageAppelComplete(pi.getFormufle().getChampFille("idProduit"),"annexe.ProduitLib","id","PRODUIT_LIB_MGA","puVente;puAchat;val;compte;compte","pu;puAchat;designation;compte;comptelibelle");
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
        // idDevise doit être hidden pour éviter modification manuelle, la valeur est gérée par JavaScript
        pi.getFormufle().getChampMulitple("idDevise").setVisible(false);
        pi.getFormufle().getChampMulitple("idbcfille").setVisible(false);
        pi.getFormufle().getChamp("tauxDeChange_0").setLibelle("Taux de change");
        pi.getFormufle().getChamp("unitelib_0").setLibelle("Unit&eacute;");
        pi.getFormufle().getChamp("designation_0").setLibelle("D&eacute;signation ");
        pi.getFormufle().getChampMulitple("tauxDeChange").setVisible(false);
        pi.getFormufle().getChampMulitple("compte").setVisible(false);
        pi.getFormufle().getChampMulitple("unite").setVisible(false);
        if(vente_details!=null && vente_details.length>0){
            pi.setDefautFille(vente_details);
            pi.getFormu().getChamp("idOrigine").setDefaut(request.getParameter("id"));
            pi.getFormu().getChamp("designation").setDefaut("Facturation de la livraison num "+request.getParameter("id"));
        }
        String[] order_form = new String[]{"daty","designation","idMagasin","idClient","remarque","idDevise","estPrevu","datyPrevu","idOrigine","etat","referencefact"};
        pi.getFormu().setOrdre(order_form);

        pi.preparerDataFormu();
        for(int i=0;i<nombreLigne;i++){
           // pi.getFormufle().getChamp("pu_"+i).setAutre("readonly");
            pi.getFormufle().getChamp("qte_"+i).setAutre("onChange='calculerMontant("+i+")'");
            pi.getFormufle().getChamp("remise_"+i).setAutre("onChange='calculerMontant("+i+")'");
            pi.getFormufle().getChamp("tva_"+i).setAutre("onChange='calculerMontant("+i+")'");
            // Utiliser la devise par défaut sélectionnée (pas AR en dur)
            pi.getFormufle().getChamp("idDevise_"+i).setDefaut(defautDeviseValue);
            // Utiliser le taux de la devise par défaut depuis le JSON
            pi.getFormufle().getChamp("tauxDeChange_"+i).setDefaut(String.valueOf(defautTauxChange));
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
            v.setDatyPrevu(null);
            Client c = (Client)new Client().getById(v.getIdClient(),"client",null);
            Calendar cal = Calendar.getInstance();
            cal.setTime(c.getDatecarte());
            cal.add(Calendar.YEAR, 1);
            Date dateCartePlusUnAn = cal.getTime();
            Date dateAujourdhui = new Date();
            carteExpiree = dateAujourdhui.compareTo(dateCartePlusUnAn) >= 0;
            BonDeCommandeFIlleCpl[] details = bc.getFilleBCLib();
            if (details != null && details.length > 0) {
                VenteDetailsLib[] lignes = new VenteDetailsLib[details.length];
                for (int i = 0; i < details.length; i++) {
                    BonDeCommandeFIlleCpl detail = details[i];
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
        pi.getFormufle().makeHtmlInsertTableauIndex();

        String titre = "Enregistrement d'une facture client";
        if(request.getParameter("acte")!=null){
            titre = "Modification de la facture client";
        }
%>
<div class="content-wrapper">
    <h1><%= titre %></h1>
    <div class="box-body">
        <form id="formId" class='container' action="<%=pi.getLien()%>?but=apresMultiple.jsp" method="post" onsubmit="return validerFormulaire()">
            <%

                out.println(pi.getFormu().getHtmlInsert());
            %>
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
        
        // Vérifier la date d'échéance par rapport à la devise après calcul
        setTimeout(function() {
            if (typeof verifierDateEcheanceDevise === 'function') {
                verifierDateEcheanceDevise();
            }
        }, 100);
    }
    

</script>

<!-- Script des devises JSON pour la vente -->
<script>
    var devisesData = <%= devisesJson %>;
    
    function extractCode(valeur) {
        if (!valeur) return 'AR';
        return valeur.split('|')[0];
    }
    
    function extractDateDebut(valeur) {
        if (!valeur) return null;
        var parts = valeur.split('|');
        return parts.length >= 2 ? parts[1] : null;
    }
    
    function extractDateFin(valeur) {
        if (!valeur) return null;
        var parts = valeur.split('|');
        return parts.length >= 3 ? parts[2] : null;
    }
    
    function getTauxDevise(valeur) {
        var codeDevise = extractCode(valeur);
        if (!devisesData || !Array.isArray(devisesData)) {
            return 1.0;
        }
        for (var i = 0; i < devisesData.length; i++) {
            var devise = devisesData[i];
            if (devise.code === codeDevise) {
                return devise.taux;
            }
        }
        return 1.0;
    }
    
    /**
     * Valide que la date d'échéance est dans la fourchette de validité de la devise
     * @param dateEcheanceStr Date d'échéance au format dd/MM/yyyy
     * @param deviseValue Valeur de la devise au format CODE|dateDebut|dateFin
     * @returns Object avec propriétés: valid (boolean), message (string si invalide)
     */
    function validerDateEcheanceDevise(dateEcheanceStr, deviseValue) {
        if (!dateEcheanceStr || !deviseValue) {
            return { valid: true };
        }
        
        var dateDebutStr = extractDateDebut(deviseValue);
        var dateFinStr = extractDateFin(deviseValue);
        var codeDevise = extractCode(deviseValue);
        
        // Si pas de dates de validité, pas de validation
        if (!dateDebutStr || !dateFinStr) {
            return { valid: true };
        }
        
        // Parser la date d'échéance (format dd/MM/yyyy)
        var partsEcheance = dateEcheanceStr.split('/');
        if (partsEcheance.length !== 3) {
            return { valid: true }; // Format invalide, laisser le serveur valider
        }
        var dateEcheance = new Date(partsEcheance[2], partsEcheance[1] - 1, partsEcheance[0]);
        
        // Parser les dates de validité (format yyyy-MM-dd)
        var partsDebut = dateDebutStr.split('-');
        var partsFin = dateFinStr.split('-');
        var dateDebut = new Date(partsDebut[0], partsDebut[1] - 1, partsDebut[2]);
        var dateFin = new Date(partsFin[0], partsFin[1] - 1, partsFin[2]);
        
        // Vérifier que la date d'échéance est dans la fourchette
        if (dateEcheance < dateDebut || dateEcheance > dateFin) {
            var formatDate = function(d) {
                return String(d.getDate()).padStart(2, '0') + '/' + 
                       String(d.getMonth() + 1).padStart(2, '0') + '/' + 
                       d.getFullYear();
            };
            return {
                valid: false,
                message: "La date d'échéance (" + dateEcheanceStr + ") n'est pas dans la période de validité de la devise " + 
                         codeDevise + " (du " + formatDate(dateDebut) + " au " + formatDate(dateFin) + ")"
            };
        }
        
        return { valid: true };
    }
    
    /**
     * Valide le formulaire avant soumission
     * Vérifie que la date d'échéance est dans la période de validité de la devise
     */
    function validerFormulaire() {
        var deviseValue = $('#idDevise').val();
        var dateEcheanceStr = $('#datyPrevu').val();
        
        // Valider la date d'échéance par rapport à la devise
        var validation = validerDateEcheanceDevise(dateEcheanceStr, deviseValue);
        if (!validation.valid) {
            if (typeof jAlert !== 'undefined') {
                jAlert(validation.message, 'Erreur de validation');
            } else {
                alert(validation.message);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Vérifie la validité de la date d'échéance par rapport à la devise
     * et affiche un avertissement si nécessaire
     */
    function verifierDateEcheanceDevise() {
        var deviseValue = $('#idDevise').val();
        var dateEcheanceStr = $('#datyPrevu').val();
        
        if (!dateEcheanceStr) return; // Pas de date, pas de vérification
        
        var validation = validerDateEcheanceDevise(dateEcheanceStr, deviseValue);
        if (!validation.valid) {
            if (typeof jAlert !== 'undefined') {
                jAlert(validation.message, 'Attention');
            } else {
                alert(validation.message);
            }
        }
    }
    
    function deviseModification() {
        var nombreLigne = parseInt($("#nombreLigne").val());
        var deviseValue = $('#idDevise').val();
        var codeDevise = extractCode(deviseValue);
        var taux = getTauxDevise(deviseValue);
        
        $("#deviseLibelle").html(codeDevise);
        
        // Vérifier la date d'échéance par rapport à la nouvelle devise
        verifierDateEcheanceDevise();
        
        for(let iL=0;iL<nombreLigne;iL++){
            $(function(){
                // Utiliser PRODUIT_LIB_MGA pour toutes les devises
                var tableToUse = "PRODUIT_LIB_MGA";
                
                // Passer la valeur complète (CODE|dateDebut|dateFin) pour la validation métier
                $("#idDevise_"+iL).val(deviseValue);
                $("#tauxDeChange_"+iL).val(taux);
                
                let autocompleteTriggered = false;
                $("#idProduit_"+iL+"libelle").autocomplete('destroy');
                $("#pu_"+iL).val('');
                
                $("#idProduit_"+iL+"libelle").autocomplete({
                    source: function(request, response) {
                        $("#idProduit_"+iL).val('');
                        if (autocompleteTriggered) {
                            // Sans taux dans le retour - le taux vient du JSON
                            fetchAutocomplete(request, response, "null", "id", "null", tableToUse, "annexe.ProduitLib", "true","puVente;puAchat;val;compte;compte");
                        }
                    },
                    select: function(event, ui) {
                        $("#idProduit_"+iL+"libelle").val(ui.item.label);
                        $("#idProduit_"+iL).val(ui.item.value);
                        $("#idProduit_"+iL).trigger('change');
                        $(this).autocomplete('disable');
                        // Retour: puVente;puAchat;val;compte;comptelibelle (index 0,1,2,3,4)
                        // Le tauxDeChange reste celui défini par deviseModification() depuis le JSON
                        var retourParts = ui.item.retour.split(';');
                        $('#pu_'+iL).val(retourParts[0]);              // puVente à index 0
                        // puAchat à index 1 (non utilisé ici)
                        $('#designation_'+iL).val(retourParts[2]);     // val/designation à index 2
                        $('#compte_'+iL).val(retourParts[3]);          // compte à index 3
                        $('#compte_'+iL+'libelle').val(retourParts[4]); // comptelibelle à index 4
                        autocompleteTriggered = false;
                        return false;
                    }
                }).autocomplete('disable');
                $("#idProduit_"+iL+"libelle").off('keydown');
                $("#idProduit_"+iL+"libelle").keydown(function(event) {
                    if (event.key === 'Tab') {
                        event.preventDefault();
                        autocompleteTriggered = true;
                        $(this).autocomplete('enable').autocomplete('search', $(this).val());
                    }
                });
                $("#idProduit_"+iL+"libelle").off('input');
                $("#idProduit_"+iL+"libelle").on('input', function() {
                    $("#idProduit_"+iL).val('');
                    autocompleteTriggered = false;
                    $(this).autocomplete('disable');
                });
                $("#idProduit_"+iL+"searchBtn").off('click');
                $("#idProduit_"+iL+"searchBtn").click(function() {
                    autocompleteTriggered = true;
                    $("#idProduit_"+iL+"libelle").autocomplete('enable').autocomplete('search', $("#idProduit_"+iL+"libelle").val());
                });
            });
        }
    }
    
    // Initialiser au chargement
    document.addEventListener('DOMContentLoaded', function() {
        var selectDevise = document.querySelector('select[name="idDevise"]');
        if (selectDevise) {
            var deviseValue = selectDevise.value;
            var codeDevise = extractCode(deviseValue);
            var taux = getTauxDevise(deviseValue);
            $("#deviseLibelle").html(codeDevise);
            
            var nombreLigne = parseInt($("#nombreLigne").val()) || 10;
            for(var iL=0; iL<nombreLigne; iL++){
                // Passer la valeur complète (CODE|dateDebut|dateFin) pour la validation métier
                $("#idDevise_"+iL).val(deviseValue);
                $("#tauxDeChange_"+iL).val(taux);
            }
        }
    });
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
    }
</script>
<%
	} catch (Exception e) {
		e.printStackTrace();
%>
    <script language="JavaScript">
        alert('<%=e.getMessage()%>');
        history.back();
    </script>
<% }%>