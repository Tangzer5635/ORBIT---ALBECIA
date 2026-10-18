package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaterielRepository extends BaseRepository<Materiel> {

    boolean existsByNumSerie(String numSerie);

    boolean existsByNumSerieAndIdNot(String numSerie, Long id);

    @Query("select u from UC u join u.processus p where p.id = :idProcessus")
    List<UC> findUcAvecProcessus(Long idProcessus);

    /** Dates de fin de garantie du matériel en service, entre debut (inclus) et fin (exclu). */

    @Query("""
            SELECT m.dateFinGarantie FROM Materiel m
            WHERE m.archive = false 
            AND m.dateFinGarantie >= :debut AND m.dateFinGarantie < :fin
            """)
    List<LocalDate> datesFinGarantie(LocalDate debut,LocalDate fin);

    /** Matériel en service dont la garantie est déjà terminée avant cette date. */
    long countByArchiveFalseAndDateFinGarantieBefore(LocalDate date);
}