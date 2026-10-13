package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.PersonnelRepository;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.RefreshTokenService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PersonnelServiceImpl extends AbstractService<Personnel, PersonnelRepository> implements PersonnelService {

    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public PersonnelServiceImpl(PersonnelRepository personnelRepository, PasswordEncoder passwordEncoder,
                                RefreshTokenService refreshTokenService) {
        super(personnelRepository);
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public Personnel create(Personnel entity) throws ServiceException {
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {throw new ServiceException("RG-U03", "Le mot de passe est obligatoire");}
        entity.setMotDePasse(passwordEncoder.encode(entity.getPassword()));
        return super.create(entity);
    }

    @Override
    @Transactional
    public Personnel update(Personnel entity) throws ServiceException {
        Personnel existant = repository.findById(entity.getId())
                .orElseThrow(() -> new ServiceException("RG-U02", "Utilisateur introuvable"));
        // le verrouillage ne se change que par verrouiller()/deverrouiller()
        entity.setVerrouille(existant.isVerrouille());
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {
            entity.setMotDePasse(existant.getPassword());
        } else {
            entity.setMotDePasse(passwordEncoder.encode(entity.getPassword()));
        }
        return super.update(entity);
    }

    @Override
    @Transactional
    public void deleteById(Long id) throws ServiceException {
        Personnel user = repository.findById(id).orElseThrow(() -> new ServiceException("Utilisateur introuvable"));
        if (user.getRole() == Role.ADMINISTRATEUR) {
            long nombreAdmins = repository.countByRole(Role.ADMINISTRATEUR);
            if (nombreAdmins <= 1) {
                throw new ServiceException("Impossible de supprimer le dernier administrateur.");
            }
        }

        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Personnel> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public Personnel deactivate(Long id) throws ServiceException {
        Personnel user = repository.findById(id)
                .orElseThrow(() -> new ServiceException("RG-U02", "Utilisateur introuvable"));
        if (user.getRole() == Role.ADMINISTRATEUR) {
            throw new ServiceException("RG-U05", "Impossible de désactiver un administrateur.");
        }
        user.setActive(false);
        return repository.save(user);
    }

    /**
     * Verrouille un compte : connexion refusée (423), jetons d'accès en cours rejetés
     * par le filtre JWT et sessions de refresh révoquées. Un administrateur ne se verrouille pas.
     */
    @Override
    @Transactional
    public Personnel verrouiller(Long id) throws ServiceException {
        Personnel user = repository.findById(id)
                .orElseThrow(() -> new ServiceException("RG-U02", "Utilisateur introuvable"));
        if (user.getRole() == Role.ADMINISTRATEUR) {
            throw new ServiceException("RG-U06", "Impossible de verrouiller un administrateur.");
        }
        if (user.isVerrouille()) {
            throw new ServiceException("RG-U07", "Ce compte est déjà verrouillé.");
        }
        user.setVerrouille(true);
        refreshTokenService.revokeAllForUser(user.getUsername());
        return repository.save(user);
    }

    @Override
    @Transactional
    public Personnel deverrouiller(Long id) throws ServiceException {
        Personnel user = repository.findById(id)
                .orElseThrow(() -> new ServiceException("RG-U02", "Utilisateur introuvable"));
        if (!user.isVerrouille()) {
            throw new ServiceException("RG-U08", "Ce compte n'est pas verrouillé.");
        }
        user.setVerrouille(false);
        return repository.save(user);
    }

    @Override
    @Transactional
    public Personnel changeRole(Long id, Role role) throws ServiceException {
        Personnel user = repository.findById(id).orElseThrow(() -> new ServiceException("RG-U02", "Utilisateur introuvable"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Personnel currentUser = auth.getPrincipal() instanceof Personnel ? (Personnel) auth.getPrincipal() : null;
            if (currentUser != null && currentUser.getId().equals(user.getId())) {
                throw new ServiceException("RG-U05", "Impossible de changer son propre rôle");
            }
        }
        user.setRole(role);
        return repository.save(user);
    }
}