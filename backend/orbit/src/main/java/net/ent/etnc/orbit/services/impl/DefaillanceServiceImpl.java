package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Defaillance;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.repositories.DefaillanceRepository;
import net.ent.etnc.orbit.services.DefaillanceService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DefaillanceServiceImpl extends AbstractService<Defaillance, DefaillanceRepository> implements DefaillanceService {

    private final MaterielService materielService;
    private final PosteService posteService;
    private final SalleService salleService;

    @Autowired
    public DefaillanceServiceImpl(DefaillanceRepository repository, MaterielService materielService, PosteService posteService, SalleService salleService) {
        super(repository);
        this.materielService = materielService;
        this.posteService = posteService;
        this.salleService = salleService;
    }

    @Override
    @Transactional
    public Defaillance signaler(Long idMateriel, String description, Personnel auteur) throws ServiceException {
        Materiel materiel = materielService.findById(idMateriel).orElseThrow(() -> new ServiceException("RG-D01", "Matériel " + idMateriel + " introuvable"));
        if (!peutSignaler(auteur, idMateriel)) {
            throw new AccessDeniedException("Ce matériel n'est ni sur votre poste ni sur un poste de vos salles");
        }
        if (repository.existsByMateriel_IdAndStatut(idMateriel, StatutDefaillance.SIGNALEE)) {
            throw new ServiceException("RG-D03", "Ce matériel a déjà une défaillance signalée");
        }
        Defaillance d = new Defaillance();
        d.setMateriel(materiel);
        d.setSignalePar(auteur);
        d.setDescription(description);
        d.setDateSignalement(LocalDateTime.now());
        d.setStatut(StatutDefaillance.SIGNALEE);
        return super.create(d);
    }

    @Override
    @Transactional
    public Defaillance resoudre(Long id, String noteSolution, Personnel technicien) throws ServiceException {
        Defaillance d = repository.findById(id).orElseThrow(() -> new ServiceException("RG-D04", "Défaillance " + id + " introuvable"));
        if (d.isResolue()) {
            throw new ServiceException("RG-D05", "Défaillance déjà résolue");
        }
        if (!peutTraiter(technicien, d.getMateriel().getId())) {
            throw new AccessDeniedException("Cette défaillance n'est pas dans vos salles");
        }
        d.resoudre(technicien, noteSolution);
        return repository.saveAndFlush(d);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Defaillance> findByMateriel(Long idMateriel) throws ServiceException {
        return repository.findByMateriel_IdOrderByDateSignalementDesc(idMateriel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Defaillance> findBySignalePar(Long idPersonnel) {
        return repository.findBySignalePar_IdOrderByDateSignalementDesc(idPersonnel);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean aUneDefaillanceOuverte(Long idMateriel) {
        return repository.existsByMateriel_IdAndStatut(idMateriel, StatutDefaillance.SIGNALEE);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean aDesDefaillances(Long idMateriel) {
        return repository.existsByMateriel_Id(idMateriel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Defaillance> findByStatut(StatutDefaillance statut, Pageable pageable) throws ServiceException {
        return repository.findByStatut(statut, pageable);
    }

    @Override
    public Defaillance create(Defaillance entity) throws ServiceException {
        throw new ServiceException("RG-D06", "Utiliser signaler()");
    }

    @Override
    public Defaillance update(Defaillance entity) throws ServiceException {
        throw new ServiceException("RG-D07", "Une défaillance ne se modifie pas, elle se résout");
    }

    /**
     * DCU « Remonter une défaillance » : le stagiaire signale sur son poste ;
     * le formateur sur son propre poste ou sur un poste d'une salle qui lui est affectée.
     * Le matériel de salle (hors poste) et le matériel en stock ne se signalent pas.
     */
    private boolean peutSignaler(Personnel user, Long idMateriel) {
        Optional<Poste> poste = posteService.posteDuMateriel(idMateriel);
        // l'occupant peut signaler le matériel de son poste
        if (poste.isPresent() && memePersonne(poste.get().getOccupant(), user)) {
            return true;
        }
        // un formateur peut signaler le matériel de ses salles : sur un poste ou posé dans la salle
        return user.getRole().hasRole(Role.FORMATEUR) && dansSesSalles(user, idMateriel, poste);
    }

    private boolean peutTraiter(Personnel user, Long idMateriel) {
        if (user.getRole().hasRole(Role.GESTIONNAIRE)) {
            return true;
        }
        return user.getRole() == Role.FORMATEUR
                && dansSesSalles(user, idMateriel, posteService.posteDuMateriel(idMateriel));
    }

    private boolean dansSesSalles(Personnel user, Long idMateriel, Optional<Poste> poste) {
        Optional<Salle> salle = poste.isPresent()
                ? salleService.salleDuPoste(poste.get().getId())
                : salleService.salleDuMateriel(idMateriel);
        return salle.map(s -> s.getPersonnels().stream().anyMatch(p -> memePersonne(p, user)))
                .orElse(false);
    }

    private static boolean memePersonne(Personnel a, Personnel b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }
}