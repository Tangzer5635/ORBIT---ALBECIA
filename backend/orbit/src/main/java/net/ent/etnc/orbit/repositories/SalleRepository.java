package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalleRepository extends BaseRepository<Salle> {

    boolean existsByMateriels_Id(Long idMateriel);

    boolean existsByTypeAndMateriels_Id(TypeSalle type, Long idMateriel);

    Optional<Salle> findByMateriels_Id(Long idMateriel);

    Optional<Salle> findFirstByTypeOrderByIdAsc(TypeSalle type);

    @Query("select r from Batiment b join b.salles s join b.salles r where s.id = :idSalle and r.type = :type order by r.id")
    List<Salle> findSallesDuBatiment(@Param("idSalle") Long idSalle, @Param("type") TypeSalle type);

    @Query("select r from Batiment b join b.salles s join s.postes p join b.salles r where p.id = :idPoste and r.type = :type order by r.id")
    List<Salle> findSallesDuBatimentDuPoste(@Param("idPoste") Long idPoste, @Param("type") TypeSalle type);

}