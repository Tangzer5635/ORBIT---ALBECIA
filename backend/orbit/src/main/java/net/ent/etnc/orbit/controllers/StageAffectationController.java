package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.UserAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.UserResponseDto;
import net.ent.etnc.orbit.services.StageAffectationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/personnels/{idStagiaire}")
public class StageAffectationController {

    private final StageAffectationService stageAffectationService;
    private final UserAssembler userAssembler;

    @Autowired
    public StageAffectationController(StageAffectationService stageAffectationService, UserAssembler userAssembler) {
        this.stageAffectationService = stageAffectationService;
        this.userAssembler = userAssembler;
    }

    @PostMapping(value = "/", params = "stageId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<UserResponseDto> ajouterAStage(@PathVariable Long idStagiaire, @RequestParam Long stageId) {
        return ResponseEntity.ok(userAssembler.toDto(stageAffectationService.ajouterAStage(stageId, idStagiaire)));
    }

    @DeleteMapping(value = "/", params = "stageId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<UserResponseDto> supprimerAStage(@PathVariable Long idStagiaire, @RequestParam Long stageId) {
        return ResponseEntity.ok(userAssembler.toDto(stageAffectationService.supprimerAStage(stageId, idStagiaire)));
    }
}