package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.repositories.MaterielRepository;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterielServiceImpl extends AbstractService<Materiel, MaterielRepository> implements MaterielService {

    @Autowired
    public MaterielServiceImpl(MaterielRepository materielRepository) {
        super(materielRepository);
    }

    @Override
    @Transactional
    public Materiel create(Materiel materiel) {
        materiel.setEtat(EtatMateriel.DISPONIBLE);
        return super.create(materiel);
    }

    @Override
    @Transactional
    public Materiel update(Materiel materiel) {
        Materiel existant = trouver(materiel.getId());
        if (existant.getEtat() == EtatMateriel.ARCHIVE) throw new ServiceException("Un matériel archivé ne peut pas être modifié");
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
        if (estAffecte(materiel))
            throw new ServiceException("Un matériel affecté ne peut pas être supprimé, remets-le en stock d'abord");
        repository.delete(materiel);
    }

    @Override
    @Transactional
    public Materiel archiver(Long id) {
        Materiel materiel = trouver(id);
        if (materiel.getEtat() == EtatMateriel.ARCHIVE)
            throw new ServiceException("Ce matériel est déjà archivé");
        if (estAffecte(materiel))
            throw new ServiceException("Un matériel affecté ne peut pas être archivé, remets-le en stock d'abord");
        materiel.setEtat(EtatMateriel.ARCHIVE);
        return materiel;
    }

    private Materiel trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ServiceException("Matériel " + id + " introuvable"));
    }

    private boolean estAffecte(Materiel materiel) {
        return materiel.getEtat() != EtatMateriel.DISPONIBLE && materiel.getEtat() != EtatMateriel.ARCHIVE;
    }
}