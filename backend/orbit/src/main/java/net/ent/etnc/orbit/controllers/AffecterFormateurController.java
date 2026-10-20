package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.services.AffecterFormateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/salles/{idSalle}")
public class AffecterFormateurController {

    private final AffecterFormateurService affecterFormateurService;
    private final SalleAssembler salleAssembler;

    @Autowired
    public AffecterFormateurController(AffecterFormateurService affecterFormateurService, SalleAssembler salleAssembler) {
        this.affecterFormateurService = affecterFormateurService;
        this.salleAssembler = salleAssembler;
    }

    /** POST /salles/{idSalle}/?formateurId=4 */
    @PostMapping(value = "/", params = "formateurId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> affecterFormateur(@PathVariable Long idSalle, @RequestParam Long formateurId) {
        return ResponseEntity.ok(dto(affecterFormateurService.affecter(idSalle, formateurId)));
    }

    @DeleteMapping(value = "/", params = "formateurId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> retirerFormateur(@PathVariable Long idSalle, @RequestParam Long formateurId) {
        return ResponseEntity.ok(dto(affecterFormateurService.retirer(idSalle, formateurId)));
    }

    private SalleResponseDto dto(Salle salle) {
        SalleResponseDto dto = salleAssembler.toDto(salle);
        dto.setFormateurs(salleAssembler.formateurs(salle));
        return dto;
    }
}