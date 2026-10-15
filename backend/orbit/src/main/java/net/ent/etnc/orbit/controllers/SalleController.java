package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.SalleEquipeeRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.SalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salles")
public class SalleController {

    private final SalleService salleService;
    private final BatimentService batimentService;
    private final SalleAssembler salleAssembler;

    @Autowired
    public SalleController(SalleService salleService, BatimentService batimentService, SalleAssembler salleAssembler) {
        this.salleService = salleService;
        this.batimentService = batimentService;
        this.salleAssembler = salleAssembler;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<Page<SalleResponseDto>> getAll(
            @PageableDefault(size = 30, sort = {"numSalle", "id"}) Pageable pageable
    ) {
        return ResponseEntity.ok(salleService.findAllAvecDetails(pageable)
                .map(salleAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<SalleResponseDto> getById(@PathVariable Long id) {
        return salleService.findByIdAvecDetails(id)
                .map(salle -> {
                    SalleResponseDto dto = salleAssembler.toDto(salle);
                    dto.setAnneau(salleService.anneauDe(id));
                    dto.setFormateurs(salleAssembler.formateurs(salle));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Page /salles?batiment=XX : cartes des salles du bâtiment (pas de pagination). */
    @GetMapping(value = "/", params = "batiment")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<List<SalleCarteDto>> getByBatiment(@RequestParam("batiment") Long idBatiment) {
        if (!batimentService.existsById(idBatiment)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(salleService.findCartesDuBatiment(idBatiment));
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> post(@Valid @RequestBody SalleRequestDto salleDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salleAssembler.toDto(
                salleService.create(salleDto.getIdBatiment(), salleAssembler.toEntity(salleDto))));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!salleService.existsById(id)) return ResponseEntity.notFound().build();
        salleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/equipee/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleEquipeeResponseDto> postEquipee(@Valid @RequestBody SalleEquipeeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salleService.creerSalleEquipee(dto));
    }
}