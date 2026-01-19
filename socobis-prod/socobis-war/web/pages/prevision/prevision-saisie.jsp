<%@page import="prevision.Prevision" %>
<%@page import="prevision.DeviseJson" %>
<%@page import="caisse.Caisse" %>
<%@page import="affichage.*" %>
<%@page import="user.*" %>
<%@page import="utils.*" %>
<%@page import="java.util.List" %>

<%
    try{

        UserEJB user = (UserEJB) session.getValue("u");
        String lien = (String) session.getValue("lien");
        Prevision prevision = new Prevision();
        PageInsert pageInsert = new PageInsert(prevision, request, user);
        pageInsert.setLien(lien);
        
        // Récupérer le chemin de base pour le fichier JSON des devises
        String basePath = application.getRealPath("/");
        String dateJour = utilitaire.Utilitaire.dateDuJour();
        
        // Récupérer TOUTES les devises depuis le JSON (pour afficher toutes les périodes)
        List<DeviseJson> toutesDevises = DeviseJson.getAllDevises(basePath);
        String devisesJson = DeviseJson.getDevisesJsonForJs(basePath);
        
        // Construire les tableaux pour la liste déroulante des devises
        // S'assurer qu'il y a au moins une devise par défaut
        int nbDevises = toutesDevises.size();
        if(nbDevises == 0) {
            // Ajouter AR par défaut si aucune devise trouvée
            toutesDevises.add(new DeviseJson("AR", "Ariary", 1.0, "2025-01-01", "2035-12-31"));
            nbDevises = 1;
        }
        
        String[] libellesDevises = new String[nbDevises];
        String[] codesDevises = new String[nbDevises];
        for(int i = 0; i < nbDevises; i++) {
            DeviseJson d = toutesDevises.get(i);
            // Valeur: code|dateDebut|dateFin pour validation côté serveur
            codesDevises[i] = d.getCode() + "|" + d.getDateDebut() + "|" + d.getDateFin();
            // Affichage: code + dates de validité
            libellesDevises[i] = d.getCode() + " (" + d.getDateDebut() + " au " + d.getDateFin() + ")";
        }
        
        // Utiliser la liste des devises JSON au lieu de la table DB
        // Format: new Liste(nomChamp, libelles[], valeurs[])
        Liste[] liste = new Liste[1];
        liste[0] = new Liste("idDevise", libellesDevises, codesDevises);
        
        Caisse c = new Caisse();
        c.setIdPoint(ConstanteStation.getFichierCentre());
                
        pageInsert.getFormu().changerEnChamp(liste);
        pageInsert.getFormu().getChamp("designation").setDefaut("Prevision du " + dateJour);
        pageInsert.getFormu().getChamp("designation").setLibelle("D&eacute;signation");
        pageInsert.getFormu().getChamp("idDevise").setLibelle("Devise");
        pageInsert.getFormu().getChamp("debit").setLibelle("d&eacute;pense");
        pageInsert.getFormu().getChamp("credit").setLibelle("recette");
        
        // Devise par défaut AR - trouver la première entrée AR
        String defautDeviseValue = "AR|2025-01-01|2035-12-31";
        for(DeviseJson d : toutesDevises) {
            if(d.getCode().equals("AR")) {
                defautDeviseValue = d.getCode() + "|" + d.getDateDebut() + "|" + d.getDateFin();
                break;
            }
        }
        pageInsert.getFormu().getChamp("idDevise").setDefaut(defautDeviseValue);
        double tauxDefaut = DeviseJson.getTauxByCodeAndDate(basePath, "AR", dateJour);
        pageInsert.getFormu().getChamp("taux").setDefaut(String.valueOf(tauxDefaut));
        pageInsert.getFormu().getChamp("taux").setAutre("readonly");
        
        pageInsert.getFormu().getChamp("compte").setLibelle("Compte de regroupement");
        pageInsert.getFormu().getChamp("debit").setVisible(true);
        pageInsert.getFormu().getChamp("idCaisse").setVisible(false);
        pageInsert.getFormu().getChamp("idVirement").setVisible(false);
        pageInsert.getFormu().getChamp("idVenteDetail").setVisible(false);
        pageInsert.getFormu().getChamp("idOp").setVisible(false);
        pageInsert.getFormu().getChamp("etat").setVisible(false);
        pageInsert.getFormu().getChamp("idOrigine").setVisible(false);
        pageInsert.getFormu().getChamp("daty").setLibelle("Date");
        pageInsert.getFormu().getChamp("idTiers").setPageAppelComplete("pertegain.Tiers","id","tiers");
        pageInsert.getFormu().getChamp("idTiers").setLibelle("Tiers");
        pageInsert.getFormu().getChamp("idFacture").setVisible(false);

        String classe = "prevision.Prevision";
        String nomTable = "PREVISION";
        String butApresPost = "prevision/prevision-fiche.jsp";
        String[] champOrdre={"daty","designation","debit","credit","idDevise","taux","idTiers","compte","idCaisse","idVirement","idVenteDetail","idOp","idOrigine","idFacture","etat"};
        pageInsert.getFormu().setOrdre(champOrdre);
        pageInsert.preparerDataFormu();
        pageInsert.getFormu().makeHtmlInsertTabIndex();


%>



    <div class="content-wrapper">
        <h1 align="center">Saisie d'une pr&eacute;vision </h1>
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
<jsp:include page='../taux.jsp'/>
