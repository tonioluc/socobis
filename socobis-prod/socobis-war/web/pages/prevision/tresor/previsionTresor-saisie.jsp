<%@page import="java.time.LocalDate" %>
<%@page import="java.time.format.DateTimeFormatter" %>

<%
    try{
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String defaultDaty1 = LocalDate.now().minusDays(1).format(formatter);
%>

<div class="content-wrapper">
    <section class="content-header">
        <h1>Saisie des dates pour Pr&eacute;visions Tr&eacute;sorerie</h1>
    </section>
    <section class="content">
        <form action="<%= (String) session.getValue("lien") %>?but=prevision/tresor/previsionTresor.jsp" method="post" name="saisie" id="saisie" onsubmit="return validateDates()">
            <div class="box-body">
                <div class="form-group">
                    <label for="daty1">Date D&eacute;but</label>
                    <input type="text" class="form-control" id="daty1" name="daty1" value="<%= defaultDaty1 %>" placeholder="dd/MM/yyyy" onmouseover="datepicker('daty1')" required>
                </div>
                <div class="form-group">
                    <label for="daty2">Date Fin</label>
                    <input type="text" class="form-control" id="daty2" name="daty2" placeholder="dd/MM/yyyy" onmouseover="datepicker('daty2')" required>
                </div>
            </div>
            <div class="box-footer">
                <button type="submit" class="btn btn-primary">Afficher</button>
            </div>
        </form>
        <script>
            function validateDates() {
                var daty1 = document.getElementById('daty1').value;
                var daty2 = document.getElementById('daty2').value;
                
                // Parse dd/MM/yyyy format
                function parseDate(dateStr) {
                    var parts = dateStr.split('/');
                    if (parts.length === 3) {
                        return new Date(parts[2], parts[1] - 1, parts[0]);
                    }
                    return null;
                }
                
                if (daty1 && daty2) {
                    var date1 = parseDate(daty1);
                    var date2 = parseDate(daty2);
                    if (date1 && date2 && date1 >= date2) {
                        alert('La date de fin doit être après la date de début.');
                        return false;
                    }
                }
                return true;
            }
        </script>
    </section>
</div>

<% }catch(Exception e){
    e.printStackTrace();
}
%>