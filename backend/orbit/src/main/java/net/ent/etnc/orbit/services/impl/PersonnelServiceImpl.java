package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.DefaillanceRepository;
import net.ent.etnc.orbit.repositories.PersonnelRepository;
import net.ent.etnc.orbit.repositories.PosteRepository;
import net.ent.etnc.orbit.repositories.SalleRepository;
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
    private final PosteRepository posteRepository;
    private final SalleRepository salleRepository;
    private final DefaillanceRepository defaillanceRepository;

    @Autowired
    public PersonnelServiceImpl(PersonnelRepository personnelRepository, PasswordEncoder passwordEncoder,
                                RefreshTokenService refreshTokenService, PosteRepository posteRepository,
                                SalleRepository salleRepository, DefaillanceRepository defaillanceRepository) {
        super(personnelRepository);
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.posteRepository = posteRepository;
        this.salleRepository = salleRepository;
        this.defaillanceRepository = defaillanceRepository;
    }

    @Override
    @Transactional
    public Personnel create(Personnel entity) throws ServiceException {
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {throw new ServiceException("RG-U03", "Le mot de passe est obligatoire");}
        entity.setMotDePasse(passwordEncoder.encode(entity.getPassword()));
        return super.create(entity);
    }

    /**
     * Modifie l'identité et le mot de passe (inchangé s'il n'est pas fourni). Le rôle, l'activation et
     * le verrouillage ne se changent que par {@link #changeRole}, {@link #activer}/{@link #desactiver}
     * et {@link #verrouiller}/{@link #deverrouiller}.
     */
    @Override
    @Transactional
    public Personnel update(Personnel entity) throws ServiceException {
        Personnel existant = trouver(entity.getId());
        entity.setRole(existant.getRole());
        entity.setActive(existant.isActive());
        entity.setVerrouille(existant.isVerrouille());
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {
            entity.setMotDePasse(existant.getPassword());
        } else {
            entity.setMotDePasse(passwordEncoder.encode(entity.getPassword()));
        }
        return super.update(entity);
    }

    /**
     * Supprime un compte et le retire de partout : son poste est libéré, il quitte ses salles et les
     * défaillances qu'il a traitées perdent leur technicien. On ne supprime ni son propre compte, ni le
     * dernier administrateur, ni un compte ayant signalé des défaillances (on garde leur trace).
     */
    @Override
    @Transactional
    public void deleteById(Long id) throws ServiceException {
        Personnel user = trouver(id);
        if (estUtilisateurCourant(id)) {
            throw new ServiceException("RG-U09", "Impossible de supprimer son propre compte.");
        }
        if (user.getRole() == Role.ADMINISTRATEUR && repository.countByRole(Role.ADMINISTRATEUR) <= 1) {
            throw new ServiceException("Impossible de supprimer le dernier administrateur.");
        }
        if (defaillanceRepository.existsBySignalePar_Id(id)) {
            throw new ServiceException("RG-U13", "Impossible de supprimer un compte ayant signalé des défaillances.");
        }

        posteRepository.liberer(id);
        salleRepository.findByPersonnels_Id(id).forEach(salle -> salle.removePersonnel(user));
        defaillanceRepository.retirerTechnicien(id);
        refreshTokenService.revokeAllForUser(user.getUsername());
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Personnel> findByUsername(String username) {
        return repository.findByUsername(username);
    }

    /** Désactive un compte : connexion refusée, jetons d'accès ignorés par le filtre JWT et sessions de refresh révoquées. */
    @Override
    @Transactional
    public Personnel desactiver(Long id) throws ServiceException {
        Personnel user = trouver(id);
        if (user.getRole() == Role.ADMINISTRATEUR) {
            throw new ServiceException("RG-U05", "Impossible de désactiver un administrateur.");
        }
        if (!user.isActive()) {
            throw new ServiceException("RG-U11", "Ce compte est déjà désactivé.");
        }
        user.setActive(false);
        refreshTokenService.revokeAllForUser(user.getUsername());
        return repository.save(user);
    }

    /** Active un compte, par exemple créé en amont pour le début d'un stage. */
    @Override
    @Transactional
    public Personnel activer(Long id) throws ServiceException {
        Personnel user = trouver(id);
        if (user.isActive()) {
            throw new ServiceException("RG-U12", "Ce compte est déjà actif.");
        }
        user.setActive(true);
        return repository.save(user);
    }

    /**
     * Verrouille un compte : connexion refusée (423), jetons d'accès en cours rejetés
     * par le filtre JWT et sessions de refresh révoquées. Un administrateur ne se verrouille pas.
     */
    @Override
    @Transactional
    public Personnel verrouiller(Long id) throws ServiceException {
        Personnel user = trouver(id);
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
        Personnel user = trouver(id);
        if (!user.isVerrouille()) {
            throw new ServiceException("RG-U08", "Ce compte n'est pas verrouillé.");
        }
        user.setVerrouille(false);
        return repository.save(user);
    }

    /**
     * Change le rôle d'un compte et révoque ses sessions de refresh : il doit se reconnecter.
     * On ne change pas son propre rôle, ni celui du dernier administrateur.
     */
    @Override
    @Transactional
    public Personnel changeRole(Long id, Role role) throws ServiceException {
        Personnel user = trouver(id);
        if (estUtilisateurCourant(id)) {
            throw new ServiceException("RG-U05", "Impossible de changer son propre rôle");
        }
        if (user.getRole() == Role.ADMINISTRATEUR && role != Role.ADMINISTRATEUR
                && repository.countByRole(Role.ADMINISTRATEUR) <= 1) {
            throw new ServiceException("RG-U10", "Impossible de changer le rôle du dernier administrateur.");
        }
        user.setRole(role);
        refreshTokenService.revokeAllForUser(user.getUsername());
        return repository.save(user);
    }

    private Personnel trouver(Long id) throws ServiceException {
        return repository.findById(id)
                .orElseThrow(() -> new ServiceException("RG-U02", "Utilisateur introuvable"));
    }

    private boolean estUtilisateurCourant(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Personnel courant && id.equals(courant.getId());
    }
}