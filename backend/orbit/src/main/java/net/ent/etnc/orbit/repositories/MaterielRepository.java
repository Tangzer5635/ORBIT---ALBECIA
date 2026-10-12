package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterielRepository extends BaseRepository<Materiel> {

    boolean existsByNumSerie(String numSerie);

    boolean existsByNumSerieAndIdNot(String numSerie, Long id);
}