package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Defaillance;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefaillanceRepository extends BaseRepository<Defaillance> {

    boolean existsByMateriel_IdAndStatut(Long idMateriel, StatutDefaillance statut);

    List<Defaillance> findByMateriel_IdOrderByDateSignalementDesc(Long idMateriel);

    Page<Defaillance> findByStatut(StatutDefaillance statut, Pageable pageable);
}
