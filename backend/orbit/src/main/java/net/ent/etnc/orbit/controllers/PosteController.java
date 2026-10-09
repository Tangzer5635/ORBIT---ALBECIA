package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.requestDTO.PosteRequestDto;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.dtos.assemblers.PosteAssembler;
import net.ent.etnc.orbit.services.PosteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/postes")
public class PosteController {

    private final PosteService posteService;
    private final PosteAssembler posteAssembler;

    @Autowired
    public PosteController(PosteService posteService, PosteAssembler posteAssembler) {
        this.posteService = posteService;
        this.posteAssembler = posteAssembler;
    }

    @GetMapping
    @PreAuthorize("hasRole('')")
    public ResponseEntity<Page<PosteResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(posteService.findAll(pageable)
                .map(posteAssembler::toDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('')")
    public ResponseEntity<PosteResponseDto> getById(@PathVariable Long id) {
        return posteService.findById(id)
                .map(poste -> ResponseEntity.ok(posteAssembler.toDto(poste)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<PosteResponseDto> post(@RequestBody PosteRequestDto posteDto) {
        return ResponseEntity.ok(
                posteAssembler.toDto(
                        posteService.create(
                                posteAssembler.toEntity(posteDto))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FORMATEUR')")
    public ResponseEntity<PosteResponseDto> put(@PathVariable Long id, @RequestBody PosteRequestDto posteDto) {
        if (!posteService.existsById(id)) return ResponseEntity.notFound().build();
        Poste poste = posteAssembler.toEntity(posteDto);
        poste.setId(id);
        return ResponseEntity.ok(
                posteAssembler.toDto(
                        posteService.update(
                                poste)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!posteService.existsById(id)) return ResponseEntity.notFound().build();
        posteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}