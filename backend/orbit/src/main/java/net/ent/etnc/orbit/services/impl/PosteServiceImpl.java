package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.enums.EtatPoste;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.repositories.PosteRepository;
import net.ent.etnc.orbit.repositories.projections.MonPosteLigne;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service des postes.
 *
 * <p>Un poste appartient toujours à une salle : sa création et sa suppression passent
 * par {@link net.ent.etnc.orbit.services.SalleService#creerPoste} et
 * {@link net.ent.etnc.orbit.services.SalleService#supprimerPoste}, qui gèrent le lien
 * avec la salle et la remise en stock du matériel. Ce service ne gère que le poste lui-même.
 */
@Service
public class PosteServiceImpl extends AbstractService<Poste, PosteRepository> implements PosteService {

    private final JournalisationService journalisationService;

    @Autowired
    public PosteServiceImpl(PosteRepository posteRepository, JournalisationService journalisationService) {
        super(posteRepository);
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional
    public Poste create(Poste poste) throws ServiceException {
        if (repository.existsByNumPoste(poste.getNumPoste()))
            throw new ServiceException("Le poste " + poste.getNumPoste() + " existe déjà");
        return super.create(poste);
    }

    /**
     * Seul le numéro est modifiable. On met à jour l'entité existante plutôt que d'enregistrer
     * celle reçue, sinon sa liste de matériels (vide) détacherait tout le matériel du poste.
     */
    @Override
    @Transactional
    public Poste update(Poste poste) throws ServiceException {
        Poste existant = repository.findById(poste.getId())
                .orElseThrow(() -> new ServiceException("Poste " + poste.getId() + " introuvable"));
        if (repository.existsByNumPosteAndIdNot(poste.getNumPoste(), poste.getId()))
            throw new ServiceException("Le poste " + poste.getNumPoste() + " existe déjà");
        String ancien = existant.getNumPoste();
        existant.setNumPoste(poste.getNumPoste());
        journalisationService.journaliser(TypeAction.MODIFICATION,
                "Poste " + ancien + " renommé en " + poste.getNumPoste());
        return existant;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Poste> posteDuMateriel(Long idMateriel) throws ServiceException {
        return repository.findByMateriels_Id(idMateriel);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean occupeUnPoste(Long idPersonnel) throws ServiceException {
        return repository.existsByOccupant_Id(idPersonnel);
    }

    @Override
    @Transactional
    public void libererPosteDe(Long idPersonnel) throws ServiceException {
         repository.liberer(idPersonnel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Poste> findAllAvecDetails(Pageable pageable) throws ServiceException {
        Page<Poste> page = repository.findAll(pageable);
        if (page.hasContent())
            repository.chargerMateriels(page.getContent().stream().map(Poste::getId).toList());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Poste> findByIdAvecDetails(Long id) throws ServiceException {
        Optional<Poste> poste = repository.findById(id);
        poste.ifPresent(p -> repository.chargerMateriels(List.of(p.getId())));
        return poste;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonPosteLigne> lignesMonPoste(Long idOccupant) throws ServiceException {
        return repository.lignesMonPoste(idOccupant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Poste> findBySalleAvecDetails(Long idSalle) throws ServiceException {
        List<Poste> postes = repository.findBySalle(idSalle);
        if (!postes.isEmpty())
            repository.chargerMateriels(postes.stream().map(Poste::getId).toList());
        return postes;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, EtatPoste> etats(List<Poste> postes) throws ServiceException {
        List<Long> idsMateriels = postes.stream()
                .flatMap(p -> p.getMateriels().stream()).map(Materiel::getId).toList();
        Set<Long> defectueux = idsMateriels.isEmpty() ? Set.of() : repository.materielsDefectueux(idsMateriels);
        return postes.stream().collect(Collectors.toMap(Poste::getId, p -> p.calculerEtat(defectueux)));
    }
}