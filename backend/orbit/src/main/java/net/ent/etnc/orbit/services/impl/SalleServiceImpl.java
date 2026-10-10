package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.SalleRepository;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SalleServiceImpl extends AbstractService<Salle, SalleRepository> implements SalleService {

    private final PosteService posteService;
    private final PersonnelService personnelService;

    @Autowired
    public SalleServiceImpl(SalleRepository salleRepository, PosteService posteService, PersonnelService personnelService) {
        super(salleRepository);
        this.posteService = posteService;
        this.personnelService = personnelService;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Salle salle = repository.findById(id)
                .orElseThrow(() -> new ServiceException("Salle " + id + " introuvable"));

        List<Materiel> aRanger = new ArrayList<>(salle.getMateriels());
        salle.getPostes().forEach(p -> aRanger.addAll(p.getMateriels()));

        if (!aRanger.isEmpty() && salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("Vide la salle de stockage avant de la supprimer");

        Salle stockage = aRanger.isEmpty() ? null : trouverStockage(id);

        for (Materiel m : List.copyOf(salle.getMateriels())) salle.removeMateriel(m);
        for (Poste p : salle.getPostes())
            for (Materiel m : List.copyOf(p.getMateriels())) p.removeMateriel(m);
        repository.flush(); // détache d'abord, sinon Hibernate peut réécrire la FK dans le désordre

        aRanger.forEach(stockage::addMateriel);

        for (Poste p : List.copyOf(salle.getPostes())) {
            salle.removePoste(p);
            posteService.delete(p);
        }
        repository.delete(salle);
    }

    @Override
    @Transactional
    public Salle changerGestionnaire(Long idSalle, Long idGestionnaire) {
        Salle salle = repository.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        Personnel gestionnaire = personnelService.findById(idGestionnaire)
                .orElseThrow(() -> new ServiceException("Personnel " + idGestionnaire + " introuvable"));
        if (gestionnaire.getRole() != Role.GESTIONNAIRE && gestionnaire.getRole() != Role.ADMINISTRATEUR)
            throw new ServiceException("Ce personnel n'est pas gestionnaire");
        salle.setGestionnaire(gestionnaire);
        return salle;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estEnStock(Long idMateriel) {
        return repository.existsByTypeAndMateriels_Id(TypeSalle.SalleStockage, idMateriel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Salle> salleDuMateriel(Long idMateriel) {
        return repository.findByMateriels_Id(idMateriel);
    }

    @Override
    @Transactional(readOnly = true)
    public Salle trouverStockage(Long idSalle) {
        return premiereReserve(repository.findSallesDuBatiment(idSalle, TypeSalle.SalleStockage));
    }

    @Override
    @Transactional(readOnly = true)
    public Salle trouverStockageDuPoste(Long idPoste) {
        return premiereReserve(repository.findSallesDuBatimentDuPoste(idPoste, TypeSalle.SalleStockage));
    }

    private Salle premiereReserve(List<Salle> reservesDuBatiment) {
        return reservesDuBatiment.stream().findFirst()
                .or(() -> repository.findFirstByTypeOrderByIdAsc(TypeSalle.SalleStockage))
                .orElseThrow(() -> new ServiceException("Aucune salle de stockage n'existe"));
    }
}