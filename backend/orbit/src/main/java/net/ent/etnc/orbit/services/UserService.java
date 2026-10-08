package net.ent.etnc.orbit.services;


import net.ent.etnc.orbit.models.entities.User;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.Optional;

public interface UserService extends Service<User, Long> {
    Optional<User> findByUsername(String username);

    User deactivate(Long id) throws ServiceException;

    User changeRole(Long id, Role role) throws ServiceException;
}