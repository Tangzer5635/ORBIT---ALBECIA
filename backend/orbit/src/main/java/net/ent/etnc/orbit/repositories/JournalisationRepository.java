package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Journalisation;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

/** Les recherches passent par findAll(Specification, Pageable) hérité de BaseRepository. */
@Repository
public interface JournalisationRepository extends BaseRepository<Journalisation> {
}