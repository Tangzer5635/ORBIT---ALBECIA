package net.ent.etnc.orbit.services.impl;

import org.springframework.transaction.annotation.Transactional;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BatimentServiceImpl extends AbstractService<Batiment, BatimentRepository> implements BatimentService {

    private final SalleService salleService;

    @Autowired
    public BatimentServiceImpl(BatimentRepository batimentRepository, SalleService salleService) {
        super(batimentRepository);
        this.salleService = salleService;
    }

    @Override
    @Transactional
    public Salle creerSalle(Long idBatiment, Salle salle) {
        Batiment batiment = repository.findById(idBatiment)
                .orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
        if (batiment.getSalles().stream().anyMatch(s -> s.getNumSalle().equals(salle.getNumSalle())))
            throw new ServiceException("Une salle " + salle.getNumSalle() + " existe déjà dans ce bâtiment");

        Salle creee = salleService.create(salle);
        batiment.addSalle(creee);
        return creee;
    }

    @Override
    @Transactional
    public Batiment affecterSalle(Long idBatiment, Long idSalle) {
        Batiment batiment = repository.findById(idBatiment)
                .orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
        Salle salle = salleService.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));

        if (repository.existsBySalles_Id(idSalle))
            throw new ServiceException("La salle est déjà affectée à un bâtiment");
        if (batiment.getSalles().stream().anyMatch(s -> s.getNumSalle().equals(salle.getNumSalle())))
            throw new ServiceException("Une salle " + salle.getNumSalle() + " existe déjà dans ce bâtiment");

        batiment.addSalle(salle);
        return batiment;
    }

    @Override
    @Transactional
    public void retirerSalle(Long idBatiment, Long idSalle) {
        Batiment batiment = repository.findById(idBatiment)
                .orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
        Salle salle = batiment.getSalles().stream()
                .filter(s -> s.getId().equals(idSalle))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Cette salle n'appartient pas à ce bâtiment"));
        batiment.removeSalle(salle);
    }
}