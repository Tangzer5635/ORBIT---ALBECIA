package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.SurchargeProcessus;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurchargeProcessusRepository extends BaseRepository<SurchargeProcessus> {

    Optional<SurchargeProcessus> findByFormateur_IdAndProcessus_Id(Long idFormateur, Long idProcessus);

    /** Surcharges de ces formateurs, avec le formateur et le processus chargés (une seule requête). */
    @Query("select s from SurchargeProcessus s join fetch s.formateur join fetch s.processus"
            + " where s.formateur.id in :idsFormateurs")
    List<SurchargeProcessus> findDesFormateurs(@Param("idsFormateurs") Collection<Long> idsFormateurs);
}
