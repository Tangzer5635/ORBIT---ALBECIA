package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.services.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** « Plaquer un master sur un poste » : l'UC du poste reçoit les processus du master (poste en erreur). */
@RestController
@RequestMapping("/api/v1/postes/{idPoste}")
public class AffecterMasterPosteController {

    private final MasterService masterService;

    @Autowired
    public AffecterMasterPosteController(MasterService masterService) {
        this.masterService = masterService;
    }

    @PostMapping(value = "/", params = "masterId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> plaquer(@PathVariable Long idPoste, @RequestParam Long masterId) {
        masterService.plaquerSurPoste(idPoste, masterId);
        return ResponseEntity.noContent().build();
    }
}
