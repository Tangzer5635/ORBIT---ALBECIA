package net.ent.etnc.orbit.services;


import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PersonnelService extends Service<Personnel, Long> {
    Optional<Personnel> findByUsername(String username);

    /** Personnel par son NID (10 chiffres), utilisé par l'import des stages. */
    Optional<Personnel> findByNid(String nid);

    Personnel desactiver(Long id) throws ServiceException;

    Personnel activer(Long id) throws ServiceException;

    Personnel changeRole(Long id, Role role) throws ServiceException;

    Personnel verrouiller(Long id) throws ServiceException;

    Personnel deverrouiller(Long id) throws ServiceException;

    Page<Personnel> findByRole(Role role, Pageable pageable) throws ServiceException;

    boolean existeDansStage(Long idStage) throws ServiceException;

    /** Stagiaires actifs du stage qui n'ont pas encore de poste. */
    List<Personnel> stagiairesSansPoste(Long idStage);
}