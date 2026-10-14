package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.repositories.projections.MonPosteLigne;
import net.ent.etnc.orbit.services.commons.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PosteService extends Service<Poste, Long> {

    Optional<Poste> posteDuMateriel(Long idMateriel);

    /** Page de postes avec matériels chargés en 1 requête : pas de N+1 à l'assemblage du DTO. */
    Page<Poste> findAllAvecDetails(Pageable pageable);

    /** Un poste avec ses matériels chargés. */
    Optional<Poste> findByIdAvecDetails(Long id);

    /** Poste de l'occupant, sa salle, son bâtiment et ses matériels en une requête (vide si aucun poste). */
    List<MonPosteLigne> lignesMonPoste(Long idOccupant);
}
