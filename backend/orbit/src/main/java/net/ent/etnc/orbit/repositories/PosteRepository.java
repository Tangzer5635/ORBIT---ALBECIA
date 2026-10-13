package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PosteRepository extends BaseRepository<Poste> {

    boolean existsByMateriels_Id(Long idMateriel);

    Optional<Poste> findByMateriels_Id(Long idMateriel);

    boolean existsByNumPoste(String numPoste);

    boolean existsByNumPosteAndIdNot(String numPoste, Long id);

    @Query("""
SELECT p FROM Poste p
LEFT JOIN p.occupant u
WHERE u.id = :id
""")
    Optional<Poste> findByIdUtilisateur(Long id);
}
