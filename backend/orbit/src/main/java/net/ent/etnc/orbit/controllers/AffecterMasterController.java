package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.services.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** « Plaquer un master sur une salle » : les UC de ses postes reçoivent les processus du master. */
@RestController
@RequestMapping("/api/v1/salles/{idSalle}")
public class AffecterMasterController {

    private final MasterService masterService;
    private final SalleAssembler salleAssembler;

    @Autowired
    public AffecterMasterController(MasterService masterService, SalleAssembler salleAssembler) {
        this.masterService = masterService;
        this.salleAssembler = salleAssembler;
    }

    @PostMapping(value = "/", params = "masterId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> plaquer(@PathVariable Long idSalle, @RequestParam Long masterId) {
        return ResponseEntity.ok(salleAssembler.toDto(masterService.plaquer(idSalle, masterId)));
    }
}