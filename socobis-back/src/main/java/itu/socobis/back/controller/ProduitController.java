package itu.socobis.back.controller;

import itu.socobis.back.dto.ProduitDTO;
import itu.socobis.back.service.IngredientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produits")
@CrossOrigin(origins = "*")
public class ProduitController {

    @Autowired
    private IngredientService ingredientService;

    /**
     * GET /api/produits - Récupère tous les produits.
     */
    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getAllProduits() {
        return ResponseEntity.ok(ingredientService.getAllProduits());
    }

    /**
     * GET /api/produits/page - Récupère les produits avec pagination.
     */
    @GetMapping("/page")
    public ResponseEntity<Page<ProduitDTO>> getProduitsPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ingredientService.getProduitsPage(search, type, page, size));
    }

    /**
     * GET /api/produits/autocomplete - Autocomplete pour recherche de produits.
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<ProduitDTO>> autocomplete(@RequestParam String q) {
        return ResponseEntity.ok(ingredientService.autocomplete(q));
    }

    /**
     * GET /api/produits/{id} - Récupère un produit par ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProduitDTO> getProduitById(@PathVariable String id) {
        return ingredientService.getProduitById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/produits/finis - Récupère tous les produits finis.
     */
    @GetMapping("/finis")
    public ResponseEntity<List<ProduitDTO>> getProduitsFinis() {
        return ResponseEntity.ok(ingredientService.getProduitsFinis());
    }

    /**
     * GET /api/produits/intermediaires - Récupère tous les produits intermédiaires.
     */
    @GetMapping("/intermediaires")
    public ResponseEntity<List<ProduitDTO>> getProduitsIntermediaires() {
        return ResponseEntity.ok(ingredientService.getProduitsIntermediaires());
    }

    /**
     * GET /api/produits/matieres-premieres - Récupère toutes les matières premières.
     */
    @GetMapping("/matieres-premieres")
    public ResponseEntity<List<ProduitDTO>> getMatierePremieres() {
        return ResponseEntity.ok(ingredientService.getMatierePremieres());
    }
}
