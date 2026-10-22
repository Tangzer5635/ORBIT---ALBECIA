package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.ProcessusAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.ProcessusSalleResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.services.SurchargeProcessusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Liste blanche / noire surchargée par un formateur pour ses salles.
 * GET ?salleId= : les processus et la liste qui s'applique dans la salle (rôle technique).
 * PUT ?processusId=&liste= / DELETE ?processusId= : le formateur connecté surcharge ou revient à la liste globale.
 */
@RestController
@RequestMapping("/api/v1/surcharges-processus")
public class SurchargeProcessusController {

    private final SurchargeProcessusService surchargeProcessusService;
    private final ProcessusAssembler processusAssembler;

    @Autowired
    public SurchargeProcessusController(SurchargeProcessusService surchargeProcessusService,
                                        ProcessusAssembler processusAssembler) {
        this.surchargeProcessusService = surchargeProcessusService;
        this.processusAssembler = processusAssembler;
    }

    @GetMapping(value = "/", params = "salleId")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<List<ProcessusSalleResponseDto>> processusDeLaSalle(@RequestParam Long salleId,
                                                                              @AuthenticationPrincipal Personnel user) {
        return ResponseEntity.ok(surchargeProcessusService.processusDeLaSalle(salleId, user).stream()
                .map(processusAssembler::toDto)
                .toList());
    }

    @PutMapping(value = "/", params = {"processusId", "liste"})
    @PreAuthorize("hasRole('FORMATEUR')")
    public ResponseEntity<Void> surcharger(@RequestParam Long processusId, @RequestParam ListeProcessus liste,
                                           @AuthenticationPrincipal Personnel user) {
        surchargeProcessusService.surcharger(processusId, liste, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/", params = "processusId")
    @PreAuthorize("hasRole('FORMATEUR')")
    public ResponseEntity<Void> reinitialiser(@RequestParam Long processusId, @AuthenticationPrincipal Personnel user) {
        surchargeProcessusService.reinitialiser(processusId, user);
        return ResponseEntity.noContent().build();
    }
}
