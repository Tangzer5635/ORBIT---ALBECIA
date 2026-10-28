package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.TypeMateriel;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeMaterielRepository extends BaseRepository<TypeMateriel> {

    Optional<TypeMateriel> findByLibelleIgnoreCase(String libelle);

    boolean existsByLibelleIgnoreCase(String libelle);

    List<TypeMateriel> findAllByOrderByLibelleAsc();
}