<%@page import="utils.ConstanteStation"%>
<%@page import="affichage.*"%>
<%@page import="caisse.MvtCaisse"%>
<%@page import="caisse.Caisse"%>
<%@page import="user.*"%>
<%@ page import="ristourne.Ristourne" %>
<%@ page import="vente.Vente" %>
<%@ page import="utilitaire.Utilitaire" %>
<%@ page import="vente.VenteLib" %>
<%@ page import="bean.TypeObjet" %>
<%@ page import="prevision.DeviseJson" %>
<%@ page import="java.util.List" %>

<%


    try{

        String lien = (String) session.getValue("lien");

        UserEJB user = (UserEJB) session.getValue("u");
        MvtCaisse mouvement = new MvtCaisse();
        PageInsert pageInsert = new PageInsert( mouvement, request, user );
        pageInsert.setLien(lien);
        String  montant=request.getParameter("montant");
        String idOrigine=request.getParameter("idOrigine");
        String devise=request.getParameter("devise");
        String tiers=request.getParameter("tiers");
        String tau=request.getParameter("taux");
        String id = "";
        VenteLib v = null;
        System.err.println(request.getParameter("id"));
        if(request.getParameter("id")!=null && request.getParameter("id")!="" && request.getParameter("id").startsWith("RIS")){
            id = request.getParameter("id");
            Ristourne r = new Ristourne();
            r.setId(id);
            System.err.println(r);
            v = r.getVente(null);
            System.err.println(v);
        }

        affichage.Champ[] liste = new affichage.Champ[2];
        // affichage.Champ[] liste = new affichage.Champ[1];
//	liste[0] = new Liste("idDevise",new caisse.Devise(),"val","id");
        Caisse c = new Caisse();
        liste[0] = new Liste("idModePaiement",new TypeObjet("MODEPAIEMENT"),"val","id");
        liste[1] = new Liste("idDevise", new TypeObjet("DEVISE"), "val" ,"id");
        pageInsert.getFormu().changerEnChamp(liste);
        pageInsert.getFormu().getChamp("designation").setDefaut("Paiement de la facture : "+idOrigine);
        pageInsert.getFormu().getChamp("idCaisse").setVisible(false);
        pageInsert.getFormu().getChamp("idDevise").setLibelle("Devise");
        pageInsert.getFormu().getChamp("daty").setLibelle("Date");
        pageInsert.getFormu().getChamp("idDevise").setDefaut(devise);
        // pageInsert.getFormu().getChamp("idDevise").setAutre("readonly");
        pageInsert.getFormu().getChamp("taux").setDefaut(tau);
        pageInsert.getFormu().getChamp("taux").setAutre("readonly");
        pageInsert.getFormu().getChamp("idVirement").setVisible(false);
        pageInsert.getFormu().getChamp("idVenteDetail").setVisible(false);
        pageInsert.getFormu().getChamp("idOp").setVisible(false);
        pageInsert.getFormu().getChamp("idOrigine").setVisible(false);
        pageInsert.getFormu().getChamp("credit").setDefaut(montant);
        pageInsert.getFormu().getChamp("credit").setLibelle("Cr&eacute;dit");
        pageInsert.getFormu().getChamp("designation").setLibelle("d&eacute;signation");
        //pageInsert.getFormu().getChamp("idtraite").setLibelle("ID Traite");

        pageInsert.getFormu().getChamp("idtraite").setVisible(false);
        pageInsert.getFormu().getChamp("etatversement").setVisible(false);

        pageInsert.getFormu().getChamp("etat").setVisible(false);
        pageInsert.getFormu().getChamp("idOrigine").setDefaut(idOrigine);
        pageInsert.getFormu().getChamp("idOrigine").setVisible(false);
        pageInsert.getFormu().getChamp("debit").setVisible(false);
        pageInsert.getFormu().getChamp("idTiers").setDefaut(tiers);
        pageInsert.getFormu().getChamp("idTiers").setVisible(false);
        pageInsert.getFormu().getChamp("idPrevision").setLibelle("Pr&eacute;vision");
        pageInsert.getFormu().getChamp("idModePaiement").setLibelle("Mode de paiement");
        pageInsert.getFormu().getChamp("idPrevision").setPageAppelComplete("prevision.Prevision", "id", "PREVISION");
        pageInsert.getFormu().getChamp("compte").setLibelle("Compte de regroupement");
        if(v!=null){
            pageInsert.getFormu().getChamp("designation").setDefaut("Paiement du ristourne "+id);
            pageInsert.getFormu().getChamp("idTiers").setDefaut(v.getTiers());
            pageInsert.getFormu().getChamp("credit").setDefaut(v.getMontantttc()+"");
            pageInsert.getFormu().getChamp("idOrigine").setDefaut(v.getId());
            // Chercher AR dans les devises JSON
            for(DeviseJson d : toutesDevises) {
                if(d.getCode().equals("AR")) {
                    pageInsert.getFormu().getChamp("idDevise").setDefaut(d.getCode() + "|" + d.getDateDebut() + "|" + d.getDateFin());
                    break;
                }
            }
            pageInsert.getFormu().getChamp("taux").setDefaut(v.getTauxdechange()+"");
            pageInsert.getFormu().getChamp("taux").setAutre("readonly");
        }
        String classe = "caisse.MvtCaisse";
        String nomTable = "MOUVEMENTCAISSE";
        String butApresPost = "caisse/mvt/mvtCaisse-fiche.jsp";
        String[] order_form = {"daty","designation","idModePaiement","credit","idDevise","taux","compte","idPrevision","idVirement","idVenteDetail","idOp","idOrigine","debit","idTiers","etat"};
        pageInsert.getFormu().setOrdre(order_form);
        pageInsert.preparerDataFormu();
        pageInsert.getFormu().makeHtmlInsertTabIndex();

%>

    <div class="content-wrapper">
        <h1 align="center">Paiement</h1>
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

<script type="text/javascript">
    // Données des devises chargées depuis le JSON
    var devisesData = <%= devisesJson %>;
    
    // Fonction pour extraire le code devise depuis la valeur composite "CODE|dateDebut|dateFin"
    function extractCode(deviseValue) {
        if (!deviseValue) return 'AR';
        var parts = deviseValue.split('|');
        return parts[0];
    }
    
    // Fonction pour extraire les dates depuis la valeur composite
    function extractDates(deviseValue) {
        if (!deviseValue || !deviseValue.includes('|')) return null;
        var parts = deviseValue.split('|');
        if (parts.length < 3) return null;
        return { dateDebut: parts[1], dateFin: parts[2] };
    }
    
    // Fonction pour trouver le taux d'une devise selon sa période
    function getTauxDevise(deviseValue) {
        var codeDevise = extractCode(deviseValue);
        var dates = extractDates(deviseValue);
        
        for (var i = 0; i < devisesData.length; i++) {
            var devise = devisesData[i];
            // Matcher par code ET par dates de période
            if (devise.code === codeDevise) {
                if (dates && devise.dateDebut === dates.dateDebut && devise.dateFin === dates.dateFin) {
                    return devise.taux;
                }
            }
        }
        
        // Si pas trouvé avec dates exactes, chercher juste par code
        for (var i = 0; i < devisesData.length; i++) {
            var devise = devisesData[i];
            if (devise.code === codeDevise) {
                return devise.taux;
            }
        }
        
        return 1.0; // Taux par défaut
    }
    
    // Mettre à jour le taux quand la devise change
    function updateTaux() {
        var selectDevise = document.querySelector('select[name="idDevise"]');
        var inputTaux = document.querySelector('input[name="taux"]');
        
        if (selectDevise && inputTaux) {
            var deviseValue = selectDevise.value;
            
            if (deviseValue) {
                var taux = getTauxDevise(deviseValue);
                inputTaux.value = taux;
            }
        }
    }
    
    // Attacher les événements une fois le DOM chargé
    document.addEventListener('DOMContentLoaded', function() {
        var selectDevise = document.querySelector('select[name="idDevise"]');
        
        if (selectDevise) {
            selectDevise.addEventListener('change', updateTaux);
        }
        
        // Initialiser le taux au chargement
        updateTaux();
    });
</script>

<%

    }catch(Exception e){
    
        e.printStackTrace();
    }

%>
<jsp:include page='../../taux.jsp'/>