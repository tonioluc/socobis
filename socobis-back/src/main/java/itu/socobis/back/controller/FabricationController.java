package itu.socobis.back.controller;

import itu.socobis.back.dto.*;
import itu.socobis.back.service.FabricationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fabrication")
@CrossOrigin(origins = "*")
public class FabricationController {

    @Autowired
    private FabricationService fabricationService;

    /**
     * GET /api/fabrication/historique - Récupère l'historique des fabrications (liste complète).
     */
    @GetMapping("/historique")
    public ResponseEntity<List<FabricationHistoriqueDTO>> getHistorique() {
        return ResponseEntity.ok(fabricationService.getHistoriqueFabrications());
    }

    /**
     * GET /api/fabrication/historique/page - Récupère l'historique avec pagination et filtres.
     */
    @GetMapping("/historique/page")
    public ResponseEntity<Page<FabricationHistoriqueDTO>> getHistoriquePage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateMin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(fabricationService.getHistoriqueFabricationsPage(dateMin, dateMax, page, size));
    }

    /**
     * GET /api/fabrication/formule/{produitId} - Récupère la formule d'un produit.
     */
    @GetMapping("/formule/{produitId}")
    public ResponseEntity<List<FormuleItemDTO>> getFormule(@PathVariable String produitId) {
        List<FormuleItemDTO> formule = fabricationService.getFormuleProduit(produitId);
        if (formule.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(formule);
    }

    /**
     * GET /api/fabrication/simuler - Simule une fabrication sans l'exécuter.
     */
    @GetMapping("/simuler")
    public ResponseEntity<SimulationFabricationDTO> simuler(
            @RequestParam String produitId,
            @RequestParam BigDecimal quantite) {
        return ResponseEntity.ok(fabricationService.simulerFabrication(produitId, quantite));
    }

    /**
     * POST /api/fabrication/creer - Crée une fabrication (état CRÉÉ).
     */
    @PostMapping("/creer")
    public ResponseEntity<?> creer(@RequestBody FabricationRequestDTO request) {
        try {
            FabricationHistoriqueDTO result = fabricationService.creerFabrication(
                    request.getProduitId(), 
                    request.getQuantite()
            );
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/fabrication/executer - Exécute une fabrication (création + terminaison directe).
     */
    @PostMapping("/executer")
    public ResponseEntity<?> executer(@RequestBody FabricationRequestDTO request) {
        try {
            FabricationHistoriqueDTO result = fabricationService.executerFabrication(
                    request.getProduitId(), 
                    request.getQuantite()
            );
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * PUT /api/fabrication/{id}/valider - Valide une fabrication (CRÉÉ -> VALIDÉ).
     */
    @PutMapping("/{id}/valider")
    public ResponseEntity<?> valider(@PathVariable String id) {
        try {
            FabricationHistoriqueDTO result = fabricationService.validerFabrication(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * PUT /api/fabrication/{id}/entamer - Entame une fabrication (VALIDÉ -> ENTAMÉ).
     */
    @PutMapping("/{id}/entamer")
    public ResponseEntity<?> entamer(@PathVariable String id) {
        try {
            FabricationHistoriqueDTO result = fabricationService.entamerFabrication(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * PUT /api/fabrication/{id}/bloquer - Bloque une fabrication (ENTAMÉ -> BLOQUÉ).
     */
    @PutMapping("/{id}/bloquer")
    public ResponseEntity<?> bloquer(@PathVariable String id) {
        try {
            FabricationHistoriqueDTO result = fabricationService.bloquerFabrication(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * PUT /api/fabrication/{id}/debloquer - Débloque une fabrication (BLOQUÉ -> ENTAMÉ).
     */
    @PutMapping("/{id}/debloquer")
    public ResponseEntity<?> debloquer(@PathVariable String id) {
        try {
            FabricationHistoriqueDTO result = fabricationService.debloquerFabrication(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * PUT /api/fabrication/{id}/terminer - Termine une fabrication (ENTAMÉ -> TERMINÉ).
     */
    @PutMapping("/{id}/terminer")
    public ResponseEntity<?> terminer(@PathVariable String id) {
        try {
            FabricationHistoriqueDTO result = fabricationService.terminerFabrication(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Classe interne pour les réponses d'erreur.
     */
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
