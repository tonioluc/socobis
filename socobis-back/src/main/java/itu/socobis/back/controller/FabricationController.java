package itu.socobis.back.controller;

import itu.socobis.back.dto.*;
import itu.socobis.back.service.FabricationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/fabrication")
@CrossOrigin(origins = "*")
public class FabricationController {

    @Autowired
    private FabricationService fabricationService;

    /**
     * GET /api/fabrication/historique - Récupère l'historique des fabrications.
     */
    @GetMapping("/historique")
    public ResponseEntity<List<FabricationHistoriqueDTO>> getHistorique() {
        return ResponseEntity.ok(fabricationService.getHistoriqueFabrications());
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
     * POST /api/fabrication/executer - Exécute une fabrication.
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
