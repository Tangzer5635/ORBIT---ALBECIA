package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonnelRepository extends BaseRepository<Personnel> {

    @Query("SELECT u FROM Personnel u WHERE u.login = :username")

    Optional<Personnel> findByUsername(String username);

    Optional<Personnel> findByNid(String nid);

    long countByRole(Role role);
    Page<Personnel> findByRole(Role role, Pageable pageable);

    boolean existsByStage_Id(Long idStage);

    /** Stagiaires actifs du stage qui n'occupent aucun poste. */
    @Query("select p from Personnel p where p.stage.id = :idStage and p.active = true"
            + " and not exists (select po.id from Poste po where po.occupant = p) order by p.nom, p.prenom")
    List<Personnel> stagiairesSansPoste(@Param("idStage") Long idStage);

    boolean existsByLogin(String login);
}