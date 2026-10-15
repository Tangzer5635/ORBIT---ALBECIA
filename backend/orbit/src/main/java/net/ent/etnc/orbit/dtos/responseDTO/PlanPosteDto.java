package net.ent.etnc.orbit.dtos.responseDTO;

import net.ent.etnc.orbit.models.enums.EtatPoste;

public record PlanPosteDto(String numPoste, EtatPoste etat) {}