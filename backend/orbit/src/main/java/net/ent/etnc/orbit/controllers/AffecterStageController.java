package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.services.AffecterStageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/salles/{idSalle}")
public class AffecterStageController {

    private final AffecterStageService affecterStageService;
    private final SalleAssembler salleAssembler;

    @Autowired
    public AffecterStageController(AffecterStageService affecterStageService, SalleAssembler salleAssembler) {
        this.affecterStageService = affecterStageService;
        this.salleAssembler = salleAssembler;
    }

    @PostMapping(value = "/", params = "stageId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> affecterStage(@PathVariable Long idSalle, @RequestParam Long stageId) {
        return ResponseEntity.ok(salleAssembler.toDto(affecterStageService.affecterASalle(idSalle, stageId)));
    }

    @DeleteMapping(value = "/", params = "stageId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> retirerStage(@PathVariable Long idSalle, @RequestParam Long stageId) {
        return ResponseEntity.ok(salleAssembler.toDto(affecterStageService.retirerDeSalle(idSalle, stageId)));
    }
}
