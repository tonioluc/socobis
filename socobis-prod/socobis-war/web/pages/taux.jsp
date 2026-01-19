<script>
function updateTaux() {
    var dateField = document.querySelector('input[name="daty"]');
    var deviseField = document.querySelector('select[name="idDevise"]');
    var tauxField = document.querySelector('input[name="taux"]');

    if(dateField && deviseField && tauxField) {
        var date = dateField.value;
        var devise = deviseField.value;

        console.log("Date:", date, "Devise:", devise);

        if(date && devise && devise !== '') {
            tauxField.style.opacity = '0.5';

            fetch('<%= request.getContextPath() %>/tauxServlet?daty=' + encodeURIComponent(date) + '&idDevise=' + encodeURIComponent(devise))
            .then(response => {
                if(!response.ok) {
                    throw new Error('Erreur HTTP: ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                if(data.taux !== undefined) {
                    tauxField.value = data.taux;
                    tauxField.style.opacity = '1';
                    console.log("Taux mis à jour:", data.taux);
                } else if(data.error) {
                    console.error('Erreur:', data.error);
                    tauxField.style.opacity = '1';
                }
            })
            .catch(error => {
                console.error('Erreur lors de la récupération du taux:', error);
                tauxField.style.opacity = '1';
            });
        }
    } else {
        console.log("Champs non trouvés:", {
            dateField: !!dateField,
            deviseField: !!deviseField,
            tauxField: !!tauxField
        });
    }
}

// Attendre que le DOM soit chargé
document.addEventListener('DOMContentLoaded', function() {
    console.log("DOM chargé, ajout des écouteurs d'événements");

    var dateField = document.querySelector('input[name="daty"]');
    var deviseField = document.querySelector('select[name="idDevise"]');

    if(dateField) {
        dateField.addEventListener('change', function() {
            console.log("Changement de date détecté");
            updateTaux();
        });
        console.log("Écouteur ajouté pour la date");
    } else {
        console.log("Champ date non trouvé");
    }

    if(deviseField) {
        deviseField.addEventListener('change', function() {
            console.log("Changement de devise détecté");
            updateTaux();
        });
        console.log("Écouteur ajouté pour la devise");
    } else {
        console.log("Champ devise non trouvé");
    }
});
</script>