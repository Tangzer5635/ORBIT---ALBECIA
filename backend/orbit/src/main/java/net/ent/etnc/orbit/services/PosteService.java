package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.services.commons.Service;

import java.util.Optional;

public interface PosteService extends Service<Poste, Long> {

    Optional<Poste> posteDuMateriel(Long idMateriel);
}
