package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import net.ent.etnc.orbit.repositories.projections.ComptesEtats;
import net.ent.etnc.orbit.repositories.projections.IdCompte;
import net.ent.etnc.orbit.repositories.projections.SalleBatiment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalleRepository extends BaseRepository<Salle> {

    boolean existsByMateriels_Id(Long idMateriel);

    boolean existsByTypeAndMateriels_Id(TypeSalle type, Long idMateriel);

    Optional<Salle> findByMateriels_Id(Long idMateriel);

    Optional<Salle> findByPostes_Id(Long idPoste);

    /** Salles attribuées à ce personnel, accès expiré compris (suppression du personnel). */
    @Query("select s from Salle s join s.personnels f where key(f).id = :idPersonnel")
    List<Salle> findByPersonnels_Id(@Param("idPersonnel") Long idPersonnel);

    /** Salles où l'accès du formateur est en cours (le jour de la date limite compte encore). */
    @Query("select s from Salle s join s.personnels f where key(f).id = :idFormateur"
            + " and (value(f) is null or value(f) >= current_date)")
    List<Salle> sallesActivesDuFormateur(@Param("idFormateur") Long idFormateur);

    List<Salle> findByMaster_Id(Long idMaster);

    /** Le formateur a un accès en cours à cette salle. */
    @Query("select case when count(s) > 0 then true else false end from Salle s join s.personnels f"
            + " where s.id = :idSalle and key(f).id = :idPersonnel"
            + " and (value(f) is null or value(f) >= current_date)")
    boolean existsByIdAndPersonnels_Id(@Param("idSalle") Long idSalle, @Param("idPersonnel") Long idPersonnel);

    /** Bâtiment de chacune de ces salles : une requête pour toutes les salles. */
    @Query("select " + SalleBatiment.NEW + "(s.id, b.numBatiment) from Batiment b join b.salles s where s.id in :ids")
    List<SalleBatiment> batimentsDesSalles(@Param("ids") Collection<Long> ids);

    /** Charge les matériels des salles dans le contexte de persistance : une requête pour toutes les salles. */
    @Query("select distinct s from Salle s left join fetch s.materiels where s.id in :ids")
    List<Salle> chargerMateriels(@Param("ids") Collection<Long> ids);

    /** Charge les postes des salles dans le contexte de persistance : une requête pour toutes les salles. */
    @Query("select distinct s from Salle s left join fetch s.postes where s.id in :ids")
    List<Salle> chargerPostes(@Param("ids") Collection<Long> ids);

    /** Matériel posé dans ces salles (réserves), compté par état. */
    @Query("select " + ComptesEtats.NEW + "(s.id, " + ComptesEtats.COLONNES + ")"
            + " from Salle s join s.materiels m where s.id in :ids group by s.id")
    List<ComptesEtats> comptesEtats(@Param("ids") Collection<Long> ids, @Param("seuil") LocalDate seuil);

    @Query("select s from Batiment b join b.salles s where b.id = :idBatiment order by s.type, s.numSalle")
    List<Salle> findByBatiment(@Param("idBatiment") Long idBatiment);

    @Query("select " + IdCompte.NEW + "(s.id, count(p)) from Salle s join s.postes p"
            + " where s.id in :ids group by s.id")
    List<IdCompte> nbPostes(@Param("ids") Collection<Long> ids);

    @Query("""
            select r
            from Batiment b
            join b.salles s
            join b.salles r
            where s.id = :idSalle
            and r.type = :type
            order by r.id
            """)
    List<Salle> findSallesDuBatiment(@Param("idSalle") Long idSalle, @Param("type") TypeSalle type);

    @Query("""
            select r
            from Batiment b
            join b.salles s
            join s.postes p
            join b.salles r
            where p.id = :idPoste
            and r.type = :type
            order by r.id
            """)
    List<Salle> findSallesDuBatimentDuPoste(@Param("idPoste") Long idPoste, @Param("type") TypeSalle type);

}