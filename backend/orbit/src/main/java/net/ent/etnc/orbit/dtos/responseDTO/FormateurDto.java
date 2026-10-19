package net.ent.etnc.orbit.dtos.responseDTO;

import java.time.LocalDate;

/** Formateur attribué à une salle, avec sa date limite d'accès (null : sans fin) et si elle est dépassée. */
public record FormateurDto(Long id, String prenom, String nom, LocalDate jusqu, boolean expire) {
}