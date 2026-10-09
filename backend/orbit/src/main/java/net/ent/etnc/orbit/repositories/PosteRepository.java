package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosteRepository extends BaseRepository<Poste> {

    boolean existsByMateriels_Id(Long idMateriel);

}