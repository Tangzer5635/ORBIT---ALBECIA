package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.User;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User> {

    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(String username);
    long countByRole(Role role);
}