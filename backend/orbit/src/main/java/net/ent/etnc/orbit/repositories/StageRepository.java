package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import net.ent.etnc.orbit.repositories.projections.SalleDuStage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StageRepository extends BaseRepository<Stage> {

    boolean existsByNom(String nom);

    Optional<Stage> findByNom(String nom);

    boolean existsByNomAndIdNot(String nom, Long id);

    boolean existsByFormateur_Id(Long idFormateur);

    /** Salle et bâtiment de chacun de ces stages : une requête pour tous les stages. */
    @Query("select " + SalleDuStage.NEW + "(s.stage.id, s.id, s.numSalle, b.numBatiment)"
            + " from Batiment b join b.salles s where s.stage.id in :ids")
    List<SalleDuStage> sallesDesStages(@Param("ids") Collection<Long> ids);

    @Query("select count(s) from Salle s where s.stage.id = :idStage")
    long nbSalles(@Param("idStage") Long idStage);
}