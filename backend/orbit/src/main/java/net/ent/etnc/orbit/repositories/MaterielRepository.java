package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import net.ent.etnc.orbit.repositories.projections.MaterielStock;
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
    List<UC> findUcAvecProcessus(@Param("idProcessus") Long idProcessus);

    /** UC posées sur les postes de cette salle. */
    @Query("select u from UC u where u.id in "
            + "(select m.id from Salle s join s.postes p join p.materiels m where s.id = :idSalle)")
    List<UC> findUcDeLaSalle(@Param("idSalle") Long idSalle);

    /** UC en service portant cette adresse MAC (normalisée, ex. 00:0C:29:AB:CD:EF). */
    @Query("select u from UC u where u.adresseMac = :mac and u.archive = false")
    List<UC> findUcParAdresseMac(@Param("mac") String mac);

    /** UC posées sur ce poste. */
    @Query("select u from UC u where u.id in "
            + "(select m.id from Poste p join p.materiels m where p.id = :idPoste)")
    List<UC> findUcDuPoste(@Param("idPoste") Long idPoste);

    /** Dates de fin de garantie du matériel en service, entre debut (inclus) et fin (exclu). */

    @Query("""
            SELECT m.dateFinGarantie FROM Materiel m
            WHERE m.archive = false 
            AND m.dateFinGarantie >= :debut AND m.dateFinGarantie < :fin
            """)
    List<LocalDate> datesFinGarantie(LocalDate debut,LocalDate fin);

    /** Matériel en service dont la garantie est déjà terminée avant cette date. */
    long countByArchiveFalseAndDateFinGarantieBefore(LocalDate date);

    /** Nombre de matériels (archivés compris) d'un type ajouté par l'admin. */
    @Query("select count(a) from AutreMateriel a where a.typeMateriel.id = :idType")
    long countDeType(@Param("idType") Long idType);

    /** Tout le stock (rangé dans une salle de stockage) avec sa réserve et son bâtiment : une seule requête. */
    @Query("select " + MaterielStock.NEW + "(m, s.numSalle, b.numBatiment)"
            + " from Batiment b join b.salles s join s.materiels m"
            + " where s.type = net.ent.etnc.orbit.models.enums.TypeSalle.SalleStockage and m.archive = false"
            + " order by b.numBatiment, m.id")
    List<MaterielStock> findStock();
}