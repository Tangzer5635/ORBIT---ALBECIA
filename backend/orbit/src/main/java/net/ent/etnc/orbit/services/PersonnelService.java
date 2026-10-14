package net.ent.etnc.orbit.services;


import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.Optional;

public interface PersonnelService extends Service<Personnel, Long> {
    Optional<Personnel> findByUsername(String username);

    Personnel desactiver(Long id) throws ServiceException;

    Personnel activer(Long id) throws ServiceException;

    Personnel changeRole(Long id, Role role) throws ServiceException;

    Personnel verrouiller(Long id) throws ServiceException;

    Personnel deverrouiller(Long id) throws ServiceException;
}