package net.ent.etnc.orbit.services.impl;

import jakarta.persistence.EntityManager;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.AffecterMaterielService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AffecterMaterielServiceImpl implements AffecterMaterielService {

    private final MaterielService materielService;
    private final SalleService salleService;
    private final PosteService posteService;
    private final EntityManager em;

    public AffecterMaterielServiceImpl(MaterielService materielService, SalleService salleService,
                                       PosteService posteService, EntityManager em) {
        this.materielService = materielService;
        this.salleService = salleService;
        this.posteService = posteService;
        this.em = em;
    }

    @Override
    @Transactional
    public Materiel affecterASalle(Long idSalle, Long idMateriel) {
        Salle salle = salle(idSalle);
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("On n'affecte pas un matériel à une salle de stockage");
        Materiel materiel = materiel(idMateriel);
        sortirDuStock(materiel);
        salle.addMateriel(materiel);
        return materiel;
    }

    @Override
    @Transactional
    public Materiel affecterAPoste(Long idPoste, Long idMateriel) {
        Poste poste = poste(idPoste);
        Materiel materiel = materiel(idMateriel);
        sortirDuStock(materiel);
        poste.addMateriel(materiel);
        return materiel;
    }

    @Override
    @Transactional
    public Materiel retirerDeSalle(Long idSalle, Long idMateriel) {
        Salle salle = salle(idSalle);
        Materiel materiel = salle.getMateriels().stream()
                .filter(m -> m.getId().equals(idMateriel))
                .findFirst()
                .filter(m -> salle.getType() != TypeSalle.SalleStockage)
                .orElseThrow(() -> new ServiceException("Ce matériel n'est pas dans cette salle"));
        salle.removeMateriel(materiel);
        em.flush();
        salleService.trouverStockage(idSalle).addMateriel(materiel);
        return materiel;
    }

    @Override
    @Transactional
    public Materiel retirerDuPoste(Long idPoste, Long idMateriel) {
        Poste poste = poste(idPoste);
        Materiel materiel = poste.getMateriels().stream()
                .filter(m -> m.getId().equals(idMateriel))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Ce matériel n'est pas sur ce poste"));
        poste.removeMateriel(materiel);
        em.flush();
        salleService.trouverStockageDuPoste(idPoste).addMateriel(materiel);
        return materiel;
    }

    /** Retire le matériel de sa réserve ; refuse s'il n'est pas en stock. */
    private void sortirDuStock(Materiel materiel) {
        Salle stockage = salleService.salleDuMateriel(materiel.getId())
                .filter(s -> s.getType() == TypeSalle.SalleStockage && !materiel.isArchive())
                .orElseThrow(() -> new ServiceException("Seul un matériel en stock peut être affecté"));
        stockage.removeMateriel(materiel);
        em.flush(); // libère la FK avant de la réécrire vers la nouvelle salle / le nouveau poste
    }

    private Materiel materiel(Long id) {
        return materielService.findById(id)
                .orElseThrow(() -> new ServiceException("Matériel " + id + " introuvable"));
    }

    private Salle salle(Long id) {
        return salleService.findById(id)
                .orElseThrow(() -> new ServiceException("Salle " + id + " introuvable"));
    }

    private Poste poste(Long id) {
        return posteService.findById(id)
                .orElseThrow(() -> new ServiceException("Poste " + id + " introuvable"));
    }
}