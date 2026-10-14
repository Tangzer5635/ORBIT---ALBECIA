package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import net.ent.etnc.orbit.repositories.projections.MonPosteLigne;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PosteRepository extends BaseRepository<Poste> {

    Optional<Poste> findByMateriels_Id(Long idMateriel);

    boolean existsByNumPoste(String numPoste);

    boolean existsByNumPosteAndIdNot(String numPoste, Long id);

    /** Charge les matériels des postes dans le contexte de persistance : une requête pour tous les postes. */
    @Query("select distinct p from Poste p left join fetch p.materiels where p.id in :ids")
    List<Poste> chargerMateriels(@Param("ids") Collection<Long> ids);

    /** Poste d'un occupant avec sa salle, son bâtiment et ses matériels : une seule requête. */
    @Query("select " + MonPosteLigne.NEW + "(p.id, p.numPoste, s.id, s.numSalle, s.etage, b.id, b.numBatiment, m,"
            + " case when exists (select d.id from Defaillance d where d.materiel = m"
            + " and d.statut = net.ent.etnc.orbit.models.enums.StatutDefaillance.SIGNALEE) then true else false end)"
            + " from Batiment b join b.salles s join s.postes p left join p.materiels m"
            + " where p.occupant.id = :idOccupant order by m.id")
    List<MonPosteLigne> lignesMonPoste(@Param("idOccupant") Long idOccupant);

}
