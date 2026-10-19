package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.commons.RsqlFilterUtils;
import net.ent.etnc.orbit.dtos.assemblers.JournalisationAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.JournalisationResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.commons.PageResponse;
import net.ent.etnc.orbit.models.entities.Journalisation;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.repositories.specifications.JournalisationSpecifications;
import net.ent.etnc.orbit.services.JournalisationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Consultation du journal ({@code /api/v1/logs/}), réservée à l'Administrateur.
 * Lecture seule : aucun POST, PUT, PATCH ni DELETE.
 */
@RestController
@RequestMapping("/api/v1/logs")
public class JournalisationController {

    /** Champs utilisables dans le filtre RSQL (?filter=) : sert aussi de liste blanche. */
    private static final Map<String, String> FILTERABLE = Map.of(
            "detail", "detail",
            "auteurId", "auteurId");

    private final JournalisationService journalisationService;
    private final JournalisationAssembler journalisationAssembler;
    private final RsqlFilterUtils rsqlFilterUtils;

    public JournalisationController(JournalisationService journalisationService,
                                    JournalisationAssembler journalisationAssembler,
                                    RsqlFilterUtils rsqlFilterUtils) {
        this.journalisationService = journalisationService;
        this.journalisationAssembler = journalisationAssembler;
        this.rsqlFilterUtils = rsqlFilterUtils;
    }

    /**
     * Une page du journal, la plus récente d'abord (même principe que GET /personnels/).
     * Ex. : /logs/?action=SIGNALEMENT&debut=2026-10-01T00:00:00&fin=2026-10-17T23:59:59&filter=(detail=ilike="12",auteurId==12)
     */
    @GetMapping("/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<PageResponse<JournalisationResponseDto>> getAll(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) TypeAction action, // absent = tous les types
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @PageableDefault(size = 50, sort = "dateHeure", direction = Sort.Direction.DESC) Pageable pageable) {
        List<Specification<Journalisation>> specs = new ArrayList<>();
        Specification<Journalisation> rsql = rsqlFilterUtils.build(filter, FILTERABLE);
        if (rsql != null) specs.add(rsql);
        if (action != null) specs.add(JournalisationSpecifications.action(action));

        // allOf d'une liste vide = aucun filtre
        return ResponseEntity.ok(PageResponse.of(
                journalisationService.rechercher(Specification.allOf(specs), debut, fin, pageable),
                journalisationAssembler::toDto));
    }
}