package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.*;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.RefreshTokenService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PersonnelServiceImpl extends AbstractService<Personnel, PersonnelRepository> implements PersonnelService {

    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PosteRepository posteRepository;
    private final SalleRepository salleRepository;
    private final DefaillanceRepository defaillanceRepository;
    private final StageRepository stageRepository;

    @Autowired
    public PersonnelServiceImpl(PersonnelRepository personnelRepository, PasswordEncoder passwordEncoder,
                                RefreshTokenService refreshTokenService, PosteRepository posteRepository,
                                SalleRepository salleRepository, DefaillanceRepository defaillanceRepository, StageRepository stageRepository) {
        super(personnelRepository);
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.posteRepository = posteRepository;
        this.salleRepository = salleRepository;
        this.defaillanceRepository = defaillanceRepository;
        this.stageRepository = stageRepository;
    }

    @Override
    @Transactional
    public Personnel create(Personnel entity) throws ServiceException {
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {
            throw new ServiceException("RG-U03", "Le mot de passe est obligatoire");
        }
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
        // Le formulaire de modification ne transmet pas le NID : ne jamais écraser
        // cette donnée persistée par null lors de la sauvegarde.
        if (entity.getNid() == null || entity.getNid().isBlank()) {
            entity.setNid(existant.getNid());
        }
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
     * Applique le formulaire de modification sur le compte existant (le formulaire n'a ni NID ni rôle : on ne peut
     * pas passer par {@link #update}, dont l'entité est validée dès l'entrée de la méthode).
     */
    @Override
    @Transactional
    public Personnel modifier(Long id, Personnel donnees) throws ServiceException {
        Personnel user = trouver(id);
        if (donnees.getNid() != null && !donnees.getNid().isBlank()) {
            user.setNid(donnees.getNid());
        }
        user.setNom(donnees.getNom());
        user.setPrenom(donnees.getPrenom());
        user.setLogin(donnees.getLogin());
        if (donnees.getPassword() != null && !donnees.getPassword().isBlank()) {
            user.setMotDePasse(passwordEncoder.encode(donnees.getPassword()));
        }
        try {
            return repository.saveAndFlush(user);
        } catch (Exception e) {
            throw new ServiceException("Erreur lors de la mise à jour", e);
        }
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
        if (stageRepository.existsByFormateur_Id(id)) {
            throw new ServiceException("Impossible de supprimer un formateur qui gère un stage.");
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

    /**
     * Désactive un compte : connexion refusée, jetons d'accès ignorés par le filtre JWT et sessions de refresh révoquées.
     */
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

    /**
     * Active un compte, par exemple créé en amont pour le début d'un stage.
     */
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
        if (user.getRole() == Role.FORMATEUR && role != Role.FORMATEUR && stageRepository.existsByFormateur_Id(id)) {
            throw new ServiceException("Impossible de changer le rôle d'un formateur qui gère un stage.");
        }
        user.setRole(role);
        if (role != Role.STAGIAIRE) user.setStage(null); // seul un stagiaire est dans un stage
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

    @Override
    @Transactional(readOnly = true)
    public Page<Personnel> findByRole(Role role, Pageable pageable) {
        return repository.findByRole(role, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeDansStage(Long idStage) {
        return repository.existsByStage_Id(idStage);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Personnel> findByNid(String nid) {
        return repository.findByNid(nid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Personnel> stagiairesSansPoste(Long idStage) {
        return repository.stagiairesSansPoste(idStage);
    }

}