package net.ent.etnc.orbit.services.impl;

import org.springframework.transaction.annotation.Transactional;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.SalleRepository;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalleServiceImpl extends AbstractService<Salle, SalleRepository> implements SalleService {

    private final MaterielService materielService;
    private final PosteService posteService;
    private final PersonnelService personnelService;

    @Autowired
    public SalleServiceImpl(SalleRepository salleRepository, MaterielService materielService, PosteService posteService, PersonnelService personnelService) {
        super(salleRepository);
        this.materielService = materielService;
        this.posteService = posteService;
        this.personnelService = personnelService;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Salle salle = repository.findById(id)
                .orElseThrow(() -> new ServiceException("Salle " + id + " introuvable"));

        for (Materiel m : List.copyOf(salle.getMateriels())) {
            salle.removeMateriel(m);
            m.setEtat(EtatMateriel.DISPONIBLE);
        }

        for (Poste p : List.copyOf(salle.getPostes())) {
            for (Materiel m : List.copyOf(p.getMateriels())) {
                p.removeMateriel(m);
                m.setEtat(EtatMateriel.DISPONIBLE);
            }
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
    @Transactional
    public Salle affecterMateriel(Long idSalle, Long idMateriel) {
        Salle salle = repository.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        Materiel materiel = materielService.findById(idMateriel)
                .orElseThrow(() -> new ServiceException("Matériel " + idMateriel + " introuvable"));

        if (materiel.getEtat() == EtatMateriel.ARCHIVE)
            throw new ServiceException("Un matériel archivé ne peut pas être affecté");
        if (repository.existsByMateriels_Id(idMateriel) || posteService.contientMateriel(idMateriel))
            throw new ServiceException("Le matériel est déjà affecté");

        salle.addMateriel(materiel);
        materiel.setEtat(EtatMateriel.NORMAL);
        return salle;
    }

    @Override
    @Transactional
    public void remettreEnStock(Long idSalle, Long idMateriel) {
        Salle salle = repository.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        Materiel materiel = salle.getMateriels().stream()
                .filter(m -> m.getId().equals(idMateriel))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Ce matériel n'est pas dans cette salle"));
        salle.removeMateriel(materiel);
        materiel.setEtat(EtatMateriel.DISPONIBLE);
    }
}