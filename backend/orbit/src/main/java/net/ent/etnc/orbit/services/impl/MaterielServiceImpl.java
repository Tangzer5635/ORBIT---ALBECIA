package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.MaterielRepository;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class MaterielServiceImpl extends AbstractService<Materiel, MaterielRepository> implements MaterielService {

    private final SalleService salleService;

    @Autowired
    public MaterielServiceImpl(MaterielRepository materielRepository, SalleService salleService) {
        super(materielRepository);
        this.salleService = salleService;
    }

    @Override
    @Transactional
    public Materiel creerEnStock(Materiel materiel, Long idStockage) {
        if (idStockage == null)
            throw new ServiceException("La salle de stockage est obligatoire");
        Salle stockage = salleService.findById(idStockage)
                .orElseThrow(() -> new ServiceException("Salle " + idStockage + " introuvable"));
        if (stockage.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("Un matériel se crée dans une salle de stockage");
        materiel.setArchive(false);
        Materiel cree = create(materiel);
        stockage.addMateriel(cree);
        return cree;
    }

    @Override
    @Transactional
    public Materiel update(Materiel materiel) {
        Materiel existant = trouver(materiel.getId());
        if (existant.isArchive())
            throw new ServiceException("Un matériel archivé ne peut pas être modifié");
        existant.setNumSerie(materiel.getNumSerie());
        existant.setModele(materiel.getModele());
        existant.setDateFinGarantie(materiel.getDateFinGarantie());
        existant.setDateAcquisition(materiel.getDateAcquisition());
        existant.setType(materiel.getType());
        try {
            return repository.saveAndFlush(existant);
        } catch (Exception e) {
            throw new ServiceException("Erreur lors de la mise à jour", e);
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Materiel materiel = trouver(id);
        if (!materiel.isArchive() && !salleService.estEnStock(id))
            throw new ServiceException("Un matériel affecté ne peut pas être supprimé, remets-le en stock d'abord");
        salleService.salleDuMateriel(id).ifPresent(s -> s.removeMateriel(materiel));
        repository.delete(materiel);
    }

    @Override
    @Transactional
    public Materiel archiver(Long id) {
        Materiel materiel = trouver(id);
        if (materiel.isArchive())
            throw new ServiceException("Ce matériel est déjà archivé");
        Salle stockage = salleService.salleDuMateriel(id)
                .filter(s -> s.getType() == TypeSalle.SalleStockage)
                .orElseThrow(() -> new ServiceException("Un matériel affecté ne peut pas être archivé, remets-le en stock d'abord"));
        stockage.removeMateriel(materiel);
        materiel.setArchive(true);
        materiel.setDateArchivage(LocalDate.now());
        return materiel;
    }

    @Override
    @Transactional(readOnly = true)
    public EtatMateriel etatDe(Materiel materiel) {
        return materiel.calculerEtat(!materiel.isArchive() && salleService.estEnStock(materiel.getId()));
    }

    private Materiel trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ServiceException("Matériel " + id + " introuvable"));
    }
}