package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.*;
import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.SalleService;
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
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Page<SalleResponseDto>> getAll(
            @PageableDefault(size = 30, sort = {"numSalle", "id"}) Pageable pageable
    ) {
        return ResponseEntity.ok(salleService.findAllAvecDetails(pageable)
                .map(salleAssembler::toDto));
    }

    /** Page « Mes salles » du formateur : uniquement les salles qui lui sont affectées. */
    @GetMapping("/mes-salles/")
    @PreAuthorize("hasRole('FORMATEUR')")
    public ResponseEntity<List<SalleCarteDto>> getMesSalles(@AuthenticationPrincipal Personnel utilisateur) {
        return ResponseEntity.ok(salleService.findCartesDuFormateur(utilisateur.getId()));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<SalleResponseDto> getById(@PathVariable Long id, @AuthenticationPrincipal Personnel utilisateur) {
        salleService.verifierAcces(id, utilisateur);
        return salleService.findByIdAvecDetails(id)
                .map(salle -> {
                    SalleResponseDto dto = salleAssembler.toDto(salle);
                    dto.setAnneau(salleService.anneauDe(id));
                    dto.setFormateurs(salleAssembler.formateurs(salle));
                    salleAssembler.completerStage(dto, salle);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Page /salles?batiment=XX : cartes des salles du bâtiment (pas de pagination). */
    @GetMapping(value = "/", params = "batiment")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
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

    /** « Modifier la salle » : nombre de postes stagiaires, de vidéoprojecteurs et de tableaux interactifs. */
    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleEquipeeResponseDto> put(@PathVariable Long id, @Valid @RequestBody SalleModificationRequestDto dto) {
        return ResponseEntity.ok(salleService.modifierComposition(id, dto));
    }

    @PatchMapping("/{id}/geler/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> geler(@PathVariable Long id, @Valid @RequestBody GelRequestDto dto) {
        return ResponseEntity.ok(salleAssembler.toDto(salleService.geler(id, dto.getMotif(), dto.getNote(), dto.getJusqu())));
    }

    @PatchMapping("/{id}/degeler/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> degeler(@PathVariable Long id) {
        return ResponseEntity.ok(salleAssembler.toDto(salleService.degeler(id)));
    }

    /** « Déplacer la salle » : tout son contenu (postes, matériel, stage, formateurs, master) va dans la salle cible. */
    @PatchMapping(value = "/{id}/deplacer/", params = "salleCibleId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> deplacer(@PathVariable Long id, @RequestParam Long salleCibleId) {
        return ResponseEntity.ok(salleAssembler.toDto(salleService.deplacer(id, salleCibleId)));
    }

    /** Salle de cours vide (ni stage, ni stagiaire, ni formateur) → salle de stockage. */
    @PatchMapping("/{id}/convertir-stockage/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> convertirEnStockage(@PathVariable Long id) {
        return ResponseEntity.ok(salleAssembler.toDto(salleService.convertirEnStockage(id)));
    }

    /** Salle de stockage → salle de cours : le matériel autre que VP/TBI part dans stockageCibleId. */
    @PatchMapping("/{id}/convertir-cours/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> convertirEnSalleDeCours(@PathVariable Long id,
                                                                    @RequestParam(required = false) Long stockageCibleId) {
        return ResponseEntity.ok(salleAssembler.toDto(salleService.convertirEnSalleDeCours(id, stockageCibleId)));
    }

    /** Numéro et étage d'une salle de stockage. */
    @PatchMapping("/{id}/stockage/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> modifierStockage(@PathVariable Long id,
                                                             @Valid @RequestBody StockageModificationRequestDto dto) {
        return ResponseEntity.ok(salleAssembler.toDto(salleService.modifierStockage(id, dto.getNumSalle(), dto.getEtage())));
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