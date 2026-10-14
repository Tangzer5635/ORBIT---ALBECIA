package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessusRepository extends BaseRepository<Processus> {

    boolean existsByNom(String nom);
}