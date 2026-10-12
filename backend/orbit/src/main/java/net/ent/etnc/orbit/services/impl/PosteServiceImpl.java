package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.repositories.PosteRepository;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.NotFoundException;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

    @Autowired
    public PosteServiceImpl(PosteRepository posteRepository) {
        super(posteRepository);
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
                .orElseThrow(() -> new NotFoundException("Poste " + poste.getId() + " introuvable"));
        if (repository.existsByNumPosteAndIdNot(poste.getNumPoste(), poste.getId()))
            throw new ServiceException("Le poste " + poste.getNumPoste() + " existe déjà");
        existant.setNumPoste(poste.getNumPoste());
        return existant;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Poste> posteDuMateriel(Long idMateriel) {
        return repository.findByMateriels_Id(idMateriel);
    }
}
