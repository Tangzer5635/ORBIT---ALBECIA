package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.PersonnelRepository;
import net.ent.etnc.orbit.services.PersonnelService;
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

    @Autowired
    public PersonnelServiceImpl(PersonnelRepository personnelRepository, PasswordEncoder passwordEncoder) {
        super(personnelRepository);
        this.passwordEncoder = passwordEncoder;
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
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {
            String currentPassword = repository.findById(entity.getId())
                    .map(Personnel::getPassword)
                    .orElseThrow(() -> new ServiceException("RG-U02", "Utilisateur introuvable"));
            entity.setMotDePasse(currentPassword);
        } else {
            entity.setMotDePasse(passwordEncoder.encode(entity.getPassword()));
        }
        return super.update(entity);
    }

    @Override
    @Transactional
    public void deleteById(Long id) throws ServiceException {
        Personnel user = repository.findById(id)
                .orElseThrow(() ->
                        new ServiceException("Utilisateur introuvable")
                );
        if (user.getRole() == Role.ADMIN) {
            long nombreAdmins = repository.countByRole(Role.ADMIN);
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
        if (user.getRole() == Role.ADMIN) {
            throw new ServiceException("RG-U05", "Impossible de désactiver un administrateur.");
        }
        user.setActive(false);
        return repository.save(user);
    }

    @Override
    @Transactional
    public Personnel changeRole(Long id, Role role) throws ServiceException {
        Personnel user = repository.findById(id)
                .orElseThrow(() -> new ServiceException("RG-U02", "Utilisateur introuvable"));

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