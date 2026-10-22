package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.SurchargeProcessus;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.repositories.SurchargeProcessusRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.ProcessusService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.SurchargeProcessusService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SurchargeProcessusServiceImpl implements SurchargeProcessusService {

    private final SurchargeProcessusRepository repository;
    private final ProcessusService processusService;
    private final SalleService salleService;
    private final PosteService posteService;
    private final MaterielService materielService;
    private final JournalisationService journalisationService;

    public SurchargeProcessusServiceImpl(SurchargeProcessusRepository repository, ProcessusService processusService,
                                         SalleService salleService, PosteService posteService,
                                         MaterielService materielService, JournalisationService journalisationService) {
        this.repository = repository;
        this.processusService = processusService;
        this.salleService = salleService;
        this.posteService = posteService;
        this.materielService = materielService;
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessusEnSalle> processusDeLaSalle(Long idSalle, Personnel utilisateur) throws ServiceException {
        Salle salle = salleService.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        if (utilisateur.getRole() == Role.FORMATEUR && salle.personnel(utilisateur.getId()).isEmpty())
            throw new ServiceException("La salle " + salle.getNumSalle() + " ne fait pas partie de vos salles");

        Map<Long, SurchargeProcessus> retenues = surchargesRetenues(salle);
        Map<Long, SurchargeProcessus> miennes = utilisateur.getRole() == Role.FORMATEUR
                ? repository.findDesFormateurs(List.of(utilisateur.getId())).stream()
                        .collect(Collectors.toMap(s -> s.getProcessus().getId(), s -> s))
                : Map.of();

        return tousLesProcessus().stream()
                .sorted(Comparator.comparing(Processus::getNom, String.CASE_INSENSITIVE_ORDER))
                .map(p -> {
                    SurchargeProcessus retenue = retenues.get(p.getId());
                    return new ProcessusEnSalle(p, retenue != null ? retenue.getListe() : p.getListe(),
                            retenue, miennes.get(p.getId()));
                })
                .toList();
    }

    /** Pas de contrôle « liste blanche du master » : le formateur peut passer en noire un processus de master. */
    @Override
    @Transactional
    public SurchargeProcessus surcharger(Long idProcessus, ListeProcessus liste, Personnel formateur) throws ServiceException {
        verifierFormateur(formateur);
        if (liste == null)
            throw new ServiceException("Choisissez la liste blanche ou la liste noire");
        Processus processus = trouverProcessus(idProcessus);

        SurchargeProcessus surcharge = repository.findByFormateur_IdAndProcessus_Id(formateur.getId(), idProcessus)
                .orElseGet(SurchargeProcessus::new);
        surcharge.setFormateur(formateur);
        surcharge.setProcessus(processus);
        surcharge.setListe(liste);
        surcharge.setDateModification(LocalDateTime.now());
        SurchargeProcessus enregistree = repository.save(surcharge);

        journalisationService.journaliser(TypeAction.MODIFICATION, "Processus « " + processus.getNom()
                + " » passé en liste " + liste.name().toLowerCase() + " dans les salles du formateur "
                + Libelles.personne(formateur) + " (liste globale : " + processus.getListe().name().toLowerCase() + ")");
        return enregistree;
    }

    @Override
    @Transactional
    public void reinitialiser(Long idProcessus, Personnel formateur) throws ServiceException {
        verifierFormateur(formateur);
        Processus processus = trouverProcessus(idProcessus);
        SurchargeProcessus surcharge = repository.findByFormateur_IdAndProcessus_Id(formateur.getId(), idProcessus)
                .orElseThrow(() -> new ServiceException("Le processus " + processus.getNom()
                        + " suit déjà la liste globale dans vos salles"));
        repository.delete(surcharge);
        journalisationService.journaliser(TypeAction.MODIFICATION, "Processus « " + processus.getNom()
                + " » revenu à la liste globale (" + processus.getListe().name().toLowerCase()
                + ") dans les salles du formateur " + Libelles.personne(formateur));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListeNoireMachine> listeNoireDeLaMachine(String mac) throws ServiceException {
        UC uc = materielService.ucParAdresseMac(mac).orElse(null);
        if (uc == null) return Optional.empty();
        Poste poste = posteService.posteDuMateriel(uc.getId()).orElse(null);
        if (poste == null) return Optional.empty(); // UC en stock : pas de salle, donc pas de règle à appliquer
        Salle salle = salleService.salleDuPoste(poste.getId()).orElse(null);
        if (salle == null) return Optional.empty();

        Map<Long, SurchargeProcessus> retenues = surchargesRetenues(salle);
        Set<String> interdits = tousLesProcessus().stream()
                .filter(p -> {
                    SurchargeProcessus retenue = retenues.get(p.getId());
                    return (retenue != null ? retenue.getListe() : p.getListe()) == ListeProcessus.NOIRE;
                })
                .map(p -> p.getNom().toLowerCase())
                .collect(Collectors.toSet());
        return Optional.of(new ListeNoireMachine(uc.getNomMachine(), poste.getNumPoste(), interdits));
    }

    /** Par processus (clé : son id), la surcharge la plus récente des formateurs de la salle. */
    private Map<Long, SurchargeProcessus> surchargesRetenues(Salle salle) {
        List<Long> formateurs = salle.getPersonnels().stream()
                .filter(p -> p.getRole() == Role.FORMATEUR)
                .map(Personnel::getId)
                .toList();
        Map<Long, SurchargeProcessus> retenues = new HashMap<>();
        if (formateurs.isEmpty()) return retenues;
        for (SurchargeProcessus s : repository.findDesFormateurs(formateurs)) {
            retenues.merge(s.getProcessus().getId(), s,
                    (a, b) -> a.getDateModification().isAfter(b.getDateModification()) ? a : b);
        }
        return retenues;
    }

    private List<Processus> tousLesProcessus() {
        return processusService.findAll(Pageable.unpaged()).getContent();
    }

    private Processus trouverProcessus(Long idProcessus) throws ServiceException {
        return processusService.findById(idProcessus)
                .orElseThrow(() -> new ServiceException("Processus " + idProcessus + " introuvable"));
    }

    private static void verifierFormateur(Personnel personnel) throws ServiceException {
        if (personnel == null || personnel.getRole() != Role.FORMATEUR)
            throw new ServiceException("Seul un formateur surcharge la liste d'un processus pour ses salles");
    }
}
