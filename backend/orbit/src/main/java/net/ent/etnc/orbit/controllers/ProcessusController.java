package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.ProcessusAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.ProcessusRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.ProcessusResponseDto;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.services.ProcessusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/processus")
public class ProcessusController {

    private final ProcessusService processusService;
    private final ProcessusAssembler processusAssembler;

    @Autowired
    public ProcessusController(ProcessusService processusService, ProcessusAssembler processusAssembler) {
        this.processusService = processusService;
        this.processusAssembler = processusAssembler;
    }

    /** Lecture ouverte au gestionnaire aussi : il compose les masters avec ces processus. */
    @GetMapping("/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<Page<ProcessusResponseDto>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(processusService.findAll(pageable).map(processusAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<ProcessusResponseDto> getById(@PathVariable Long id) {
        return processusService.findById(id)
                .map(p -> ResponseEntity.ok(processusAssembler.toDto(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    @PreAuthorize("hasAnyRole('FORMATEUR','ADMINISTRATEUR')")
    public ResponseEntity<ProcessusResponseDto> post(@Valid @RequestBody ProcessusRequestDto dto) {
        return ResponseEntity.ok(processusAssembler.toDto(processusService.create(processusAssembler.toEntity(dto))));
    }

    /** Passe le processus en liste blanche ou noire (?liste=BLANCHE ou ?liste=NOIRE). */
    @PatchMapping(value = "/{id}/", params = "liste")
    @PreAuthorize("hasAnyRole('FORMATEUR','ADMINISTRATEUR')")
    public ResponseEntity<ProcessusResponseDto> changerListe(@PathVariable Long id, @RequestParam ListeProcessus liste) {
        return ResponseEntity.ok(processusAssembler.toDto(processusService.changerListe(id, liste)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasAnyRole('FORMATEUR','ADMINISTRATEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!processusService.existsById(id)) return ResponseEntity.notFound().build();
        processusService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}