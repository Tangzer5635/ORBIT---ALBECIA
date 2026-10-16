package net.ent.etnc.orbit.services.impl;

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

import java.util.List;

@Service
public class AffecterMaterielServiceImpl implements AffecterMaterielService {

    private final MaterielService materielService;
    private final SalleService salleService;
    private final PosteService posteService;

    public AffecterMaterielServiceImpl(MaterielService materielService, SalleService salleService, PosteService posteService) {
        this.materielService = materielService;
        this.salleService = salleService;
        this.posteService = posteService;
    }

    @Override
    @Transactional
    public Materiel affecterASalle(Long idSalle, Long idMateriel) {
        Salle salle = salleService.findById(idSalle).orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("On n'affecte pas un matériel à une salle de stockage");

        Materiel materiel = materielService.findById(idMateriel).orElseThrow(() -> new ServiceException("Matériel " + idMateriel + " introuvable"));
        sortirDuStock(materiel);
        salle.addMateriel(materiel);
        return materiel;
    }

    @Override
    @Transactional
    public Materiel affecterAPoste(Long idPoste, Long idMateriel) {
        Poste poste = posteService.findById(idPoste).orElseThrow(() -> new ServiceException("Poste " + idPoste + " introuvable"));
        Materiel materiel = materielService.findById(idMateriel).orElseThrow(() -> new ServiceException("Matériel " + idMateriel + " introuvable"));
        sortirDuStock(materiel);
        poste.addMateriel(materiel);
        return materiel;
    }

    @Override
    @Transactional
    public Materiel retirerDeSalle(Long idSalle, Long idMateriel) {
        Salle salle = salleService.findById(idSalle).orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("Ce matériel est déjà en stock");
        Materiel materiel = chercher(salle.getMateriels(), idMateriel);
        if (materiel == null)
            throw new ServiceException("Ce matériel n'est pas dans cette salle");
        salle.removeMateriel(materiel);
        Salle stockage = salleService.trouverStockage(idSalle);
        stockage.addMateriel(materiel);
        return materiel;
    }

    @Override
    @Transactional
    public Materiel retirerDuPoste(Long idPoste, Long idMateriel) {
        Poste poste = posteService.findById(idPoste).orElseThrow(() -> new ServiceException("Poste " + idPoste + " introuvable"));
        Materiel materiel = chercher(poste.getMateriels(), idMateriel);
        if (materiel == null)
            throw new ServiceException("Ce matériel n'est pas sur ce poste");
        poste.removeMateriel(materiel);
        Salle stockage = salleService.trouverStockageDuPoste(idPoste);
        stockage.addMateriel(materiel);
        return materiel;
    }

    /** Une seule transaction : si l'affectation échoue, le retrait est annulé (le matériel ne reste pas en stock). */
    @Override
    @Transactional
    public Materiel deplacerVersSalle(Long idSalle, Long idMateriel) {
        remettreEnStock(idMateriel);
        return affecterASalle(idSalle, idMateriel);
    }

    @Override
    @Transactional
    public Materiel deplacerVersPoste(Long idPoste, Long idMateriel) {
        remettreEnStock(idMateriel);
        return affecterAPoste(idPoste, idMateriel);
    }

    /** Retire le matériel de son poste ou de sa salle de cours actuelle. */
    private void remettreEnStock(Long idMateriel) {
        Poste poste = posteService.posteDuMateriel(idMateriel).orElse(null);
        if (poste != null) {
            retirerDuPoste(poste.getId(), idMateriel);
            return;
        }
        Salle salle = salleService.salleDuMateriel(idMateriel)
                .orElseThrow(() -> new ServiceException("Ce matériel n'est affecté à aucune salle"));
        retirerDeSalle(salle.getId(), idMateriel);
    }

    private void sortirDuStock(Materiel materiel) {
        if (materiel.isArchive())
            throw new ServiceException("Un matériel archivé ne peut pas être affecté");
        Salle salle = salleService.salleDuMateriel(materiel.getId()).orElse(null);
        if (salle == null || salle.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("Seul un matériel en stock peut être affecté");
        salle.removeMateriel(materiel);
    }

    private Materiel chercher(List<Materiel> materiels, Long idMateriel) {
        for (Materiel m : materiels) {
            if (m.getId().equals(idMateriel)) {
                return m;
            }
        }
        return null;
    }
}