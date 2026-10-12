package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.DefaillanceAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.DefaillanceRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.ResoudreDefaillanceRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.DefaillanceResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.services.DefaillanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/defaillances")
public class DefaillanceController {

    private final DefaillanceService defaillanceService;
    private final DefaillanceAssembler defaillanceAssembler;

    @Autowired
    public DefaillanceController(DefaillanceService defaillanceService, DefaillanceAssembler defaillanceAssembler) {
        this.defaillanceService = defaillanceService;
        this.defaillanceAssembler = defaillanceAssembler;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<Page<DefaillanceResponseDto>> getAll(
            @RequestParam(required = false) StatutDefaillance statut,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<DefaillanceResponseDto> page = (statut == null
                ? defaillanceService.findAll(pageable)
                : defaillanceService.findByStatut(statut, pageable))
                .map(defaillanceAssembler::toDto);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<DefaillanceResponseDto> getById(@PathVariable Long id) {
        return defaillanceService.findById(id)
                .map(d -> ResponseEntity.ok(defaillanceAssembler.toDto(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/materiel/{idMateriel}/")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<List<DefaillanceResponseDto>> getByMateriel(@PathVariable Long idMateriel) {
        return ResponseEntity.ok(defaillanceService.findByMateriel(idMateriel).stream()
                .map(defaillanceAssembler::toDto)
                .toList());
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<DefaillanceResponseDto> signaler(
            @Valid @RequestBody DefaillanceRequestDto dto,
            @AuthenticationPrincipal Personnel user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(defaillanceAssembler.toDto(
                defaillanceService.signaler(dto.getIdMateriel(), dto.getDescription(), user)));
    }

    @PatchMapping("/{id}/resoudre/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<DefaillanceResponseDto> resoudre(
            @PathVariable Long id,
            @Valid @RequestBody ResoudreDefaillanceRequestDto dto,
            @AuthenticationPrincipal Personnel user
    ) {
        return ResponseEntity.ok(defaillanceAssembler.toDto(
                defaillanceService.resoudre(id, dto.getNoteSolution(), user)));
    }
}
