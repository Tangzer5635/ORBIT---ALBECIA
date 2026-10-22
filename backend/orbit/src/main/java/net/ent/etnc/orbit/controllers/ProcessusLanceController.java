package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.agent.ProcessusPosteDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.services.ProcessusLanceService;
import net.ent.etnc.orbit.services.SalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Processus lancés sur la machine d'un poste (via l'Agent ORBIT) : les voir et les tuer.
 * Ouvert au rôle technique (formateur, gestionnaire, administrateur) ;
 * un formateur n'agit que sur les postes des salles qui lui sont affectées.
 */
@RestController
@RequestMapping("/api/v1/processus-lances")
public class ProcessusLanceController {

    private final ProcessusLanceService processusLanceService;
    private final SalleService salleService;

    @Autowired
    public ProcessusLanceController(ProcessusLanceService processusLanceService, SalleService salleService) {
        this.processusLanceService = processusLanceService;
        this.salleService = salleService;
    }

    /** GET /processus-lances/?posteId=12 */
    @GetMapping(value = "/", params = "posteId")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<ProcessusPosteDto> lister(@RequestParam Long posteId,
                                                    @AuthenticationPrincipal Personnel utilisateur) {
        verifierAcces(posteId, utilisateur);
        return ResponseEntity.ok(processusLanceService.lister(posteId));
    }

    /** DELETE /processus-lances/?posteId=12&nom=notepad.exe : tue toutes les instances, renvoie leur nombre. */
    @DeleteMapping(value = "/", params = {"posteId", "nom"})
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<Integer> tuer(@RequestParam Long posteId, @RequestParam String nom,
                                        @AuthenticationPrincipal Personnel utilisateur) {
        verifierAcces(posteId, utilisateur);
        return ResponseEntity.ok(processusLanceService.tuer(posteId, nom));
    }

    /** Un formateur ne touche qu'aux postes des salles qui lui sont affectées. */
    private void verifierAcces(Long idPoste, Personnel utilisateur) {
        salleService.salleDuPoste(idPoste).ifPresent(salle -> salleService.verifierAcces(salle.getId(), utilisateur));
    }
}