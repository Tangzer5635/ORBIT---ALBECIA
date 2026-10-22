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

    /**
     * Modifie l'identité (nom, prénom, login) et le mot de passe d'un compte. Le NID n'est changé que s'il est
     * fourni ; le rôle, l'activation, le verrouillage et le stage sont conservés (ils ont leurs propres endpoints).
     * {@code donnees} vient du formulaire de modification : elle est incomplète, donc non validée comme une entité.
     */
    Personnel modifier(Long id, Personnel donnees) throws ServiceException;

    Personnel desactiver(Long id) throws ServiceException;

    Personnel activer(Long id) throws ServiceException;

    Personnel changeRole(Long id, Role role) throws ServiceException;

    Personnel verrouiller(Long id) throws ServiceException;

    Personnel deverrouiller(Long id) throws ServiceException;

    Page<Personnel> findByRole(Role role, Pageable pageable) throws ServiceException;

    boolean existeDansStage(Long idStage) throws ServiceException;

    /** Stagiaires inscrits dans ce stage. */
    List<Personnel> stagiairesDuStage(Long idStage) throws ServiceException;

    /** Stagiaires actifs du stage qui n'ont pas encore de poste. */
    List<Personnel> stagiairesSansPoste(Long idStage);
}