package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.services.commons.Service;
import org.springframework.data.domain.Range;

import java.util.Optional;

public interface PosteService extends Service<Poste, Long> {

    Optional<Poste> posteDuMateriel(Long idMateriel);

    Optional<Poste> findMyPoste(Long id);
}
