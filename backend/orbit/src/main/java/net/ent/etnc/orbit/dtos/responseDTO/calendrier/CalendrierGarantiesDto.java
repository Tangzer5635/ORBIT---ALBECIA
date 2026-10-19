package net.ent.etnc.orbit.dtos.responseDTO.calendrier;

import java.util.List;

/** Fins de garantie mois par mois pour une année (janvier à décembre) + le matériel déjà hors garantie*/
public record CalendrierGarantiesDto(int annee, long expirees, List<MoisGarantieDto> mois) {
}
