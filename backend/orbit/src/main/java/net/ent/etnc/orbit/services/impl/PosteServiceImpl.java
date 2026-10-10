package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.repositories.PosteRepository;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosteServiceImpl extends AbstractService<Poste, PosteRepository> implements PosteService {

    private final MaterielService materielService;

    @Autowired
    public PosteServiceImpl(PosteRepository posteRepository, MaterielService materielService) {
        super(posteRepository);
        this.materielService = materielService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contientMateriel(Long idMateriel) {
        return repository.existsByMateriels_Id(idMateriel);
    }

    @Override
    @Transactional
    public Poste affecterMateriel(Long idPoste, Long idMateriel) {
        Poste poste = repository.findById(idPoste)
                .orElseThrow(() -> new ServiceException("Poste " + idPoste + " introuvable"));
        Materiel materiel = materielService.findById(idMateriel)
                .orElseThrow(() -> new ServiceException("Matériel " + idMateriel + " introuvable"));
        if (materiel.getEtat() != EtatMateriel.DISPONIBLE) throw new ServiceException("Seul un matériel en stock peut être affecté à un poste");
        poste.addMateriel(materiel);
        materiel.setEtat(EtatMateriel.NORMAL);
        return poste;
    }

    @Override
    @Transactional
    public void remettreEnStock(Long idPoste, Long idMateriel) {
        Poste poste = repository.findById(idPoste)
                .orElseThrow(() -> new ServiceException("Poste " + idPoste + " introuvable"));
        Materiel materiel = poste.getMateriels().stream()
                .filter(m -> m.getId().equals(idMateriel))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Ce matériel n'est pas sur ce poste"));
        poste.removeMateriel(materiel);
        materiel.setEtat(EtatMateriel.DISPONIBLE);
    }
}