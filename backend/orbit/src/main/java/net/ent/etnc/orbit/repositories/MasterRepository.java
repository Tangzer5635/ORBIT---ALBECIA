package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterRepository extends BaseRepository<Master> {

    boolean existsByNom(String nom);

    boolean existsByNomAndIdNot(String nom, Long id);

    /** Masters qui contiennent ce processus. */
    List<Master> findByProcessus_Id(Long idProcessus);
}