package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.PosteAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.PosteRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MonPosteResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * CRUD des postes. Un poste est créé dans une salle de cours ; à sa suppression,
 * son matériel retourne dans la réserve du bâtiment.
 */
@RestController
@RequestMapping("/api/v1/postes")
public class PosteController {

    private final PosteService posteService;
    private final SalleService salleService;
    private final PosteAssembler posteAssembler;

    @Autowired
    public PosteController(PosteService posteService, SalleService salleService, PosteAssembler posteAssembler) {
        this.posteService = posteService;
        this.salleService = salleService;
        this.posteAssembler = posteAssembler;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<Page<PosteResponseDto>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(posteService.findAll(pageable).map(posteAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<PosteResponseDto> getById(@PathVariable Long id) {
        return posteService.findById(id)
                .map(poste -> ResponseEntity.ok(posteAssembler.toDto(poste)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/")
    @PreAuthorize("hasRole('STAGIAIRE')")
    public ResponseEntity<MonPosteResponseDto> getMyPoste(@AuthenticationPrincipal Personnel utilisateur) {
        return posteService.findMyPoste(utilisateur.getId())
                .map(poste -> ResponseEntity.ok(posteAssembler.toDtoMonPoste(poste)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<PosteResponseDto> post(@Valid @RequestBody PosteRequestDto posteDto) {
        if (posteDto.getIdSalle() == null)
            throw new ServiceException("idSalle est obligatoire pour créer un poste");
        Poste cree = salleService.creerPoste(posteDto.getIdSalle(), posteAssembler.toEntity(posteDto));
        return ResponseEntity.status(HttpStatus.CREATED).body(posteAssembler.toDto(cree));
    }

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<PosteResponseDto> put(@PathVariable Long id, @Valid @RequestBody PosteRequestDto posteDto) {
        Poste poste = posteAssembler.toEntity(posteDto);
        poste.setId(id);
        return ResponseEntity.ok(posteAssembler.toDto(posteService.update(poste)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salleService.supprimerPoste(id);
        return ResponseEntity.noContent().build();
    }
}
