package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonnelRepository extends BaseRepository<Personnel> {

    @Query("SELECT u FROM Personnel u WHERE u.login = :username")
    Optional<Personnel> findByUsername(String username);
    long countByRole(Role role);
}