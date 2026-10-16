package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Defaillance;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefaillanceRepository extends BaseRepository<Defaillance> {

    boolean existsByMateriel_IdAndStatut(Long idMateriel, StatutDefaillance statut);

    boolean existsByMateriel_Id(Long idMateriel);

    List<Defaillance> findByMateriel_IdOrderByDateSignalementDesc(Long idMateriel);

    List<Defaillance> findBySignalePar_IdOrderByDateSignalementDesc(Long idPersonnel);

    Page<Defaillance> findByStatut(StatutDefaillance statut, Pageable pageable);

    /**
     * Défaillances du matériel des salles d'un formateur : matériel d'un poste de la salle ou posé dans la salle.
     * Statut absent = signalées et résolues.
     */
    @Query("""
            select d from Defaillance d
            where (:statut is null or d.statut = :statut)
              and (d.materiel.id in (select m.id from Salle s join s.personnels p join s.postes po join po.materiels m
                                     where p.id = :idFormateur)
                or d.materiel.id in (select m.id from Salle s join s.personnels p join s.materiels m
                                     where p.id = :idFormateur))
            """)
    Page<Defaillance> findDansLesSallesDe(@Param("idFormateur") Long idFormateur,
                                          @Param("statut") StatutDefaillance statut, Pageable pageable);

    /** Suppression d'un personnel : les défaillances qu'il a traitées restent, sans technicien. */
    @Modifying(flushAutomatically = true)
    @Query("update Defaillance d set d.traitePar = null where d.traitePar.id = :idPersonnel")
    int retirerTechnicien(@Param("idPersonnel") Long idPersonnel);

    boolean existsBySignalePar_Id(Long idPersonnel);
}