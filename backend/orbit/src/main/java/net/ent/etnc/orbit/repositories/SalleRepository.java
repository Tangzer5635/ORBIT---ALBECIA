package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalleRepository extends BaseRepository<Salle> {

    boolean existsByMateriels_Id(Long idMateriel);

}