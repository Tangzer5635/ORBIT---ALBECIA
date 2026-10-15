package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.MaterielRepository;
import net.ent.etnc.orbit.services.DefaillanceService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class MaterielServiceImpl extends AbstractService<Materiel, MaterielRepository> implements MaterielService {

    private final SalleService salleService;
    private final PosteService posteService;
    /** @Lazy : DefaillanceService dépend déjà de MaterielService (dépendance circulaire sinon). */
    private final DefaillanceService defaillanceService;

    @Autowired
    public MaterielServiceImpl(MaterielRepository materielRepository, SalleService salleService,
                               PosteService posteService, @Lazy DefaillanceService defaillanceService) {
        super(materielRepository);
        this.salleService = salleService;
        this.posteService = posteService;
        this.defaillanceService = defaillanceService;
    }

    @Override
    @Transactional
    public Materiel creerEnStock(Materiel materiel, Long idStockage) {
        if (idStockage == null)
            throw new ServiceException("La salle de stockage est obligatoire");
        if (repository.existsByNumSerie(materiel.getNumSerie()))
            throw new ServiceException("Le numéro de série " + materiel.getNumSerie() + " existe déjà");
        Salle stockage = salleService.findById(idStockage).orElseThrow(() -> new ServiceException("Salle " + idStockage + " introuvable"));
        if (stockage.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("Un matériel se crée dans une salle de stockage");
        materiel.setArchive(false);
        Materiel cree = repository.save(materiel);
        stockage.addMateriel(cree);
        return cree;
    }

    @Override
    @Transactional
    public Materiel update(Materiel materiel) {
        Materiel existant = trouver(materiel.getId());
        //TODO a voir avec l'équipe
        if (existant.isArchive())
            throw new ServiceException("Un matériel archivé ne peut pas être modifié");
        if (!existant.getType().equals(materiel.getType()))
            throw new ServiceException("Le type d'un matériel ne peut pas être modifié");

        if (repository.existsByNumSerieAndIdNot(materiel.getNumSerie(), materiel.getId()))
            throw new ServiceException("Le numéro de série " + materiel.getNumSerie() + " existe déjà");
        existant.setNumSerie(materiel.getNumSerie());
        existant.setModele(materiel.getModele());
        existant.setDateFinGarantie(materiel.getDateFinGarantie());
        existant.setDateAcquisition(materiel.getDateAcquisition());
        if (existant instanceof UC uc && materiel instanceof UC modifie)
            uc.setNomMachine(modifie.getNomMachine());
        return repository.save(existant);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Salle> salleDe(Materiel materiel) {
        if (materiel.isArchive()) return Optional.empty();
        return salleService.localiser(materiel.getId());
    }

    @Override
    @Transactional
    public void desinstallerPartout(Processus processus) {
        repository.findUcAvecProcessus(processus.getId()).forEach(uc -> uc.removeProcessus(processus));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Materiel materiel = trouver(id);
        if (defaillanceService.aDesDefaillances(id))
            throw new ServiceException("Ce matériel a un historique de défaillances : archive-le au lieu de le supprimer");
        // affecté ou en stock : on le sort d'abord de son poste ou de sa salle
        posteService.posteDuMateriel(id).ifPresent(poste -> poste.removeMateriel(materiel));
        salleService.salleDuMateriel(id).ifPresent(salle -> salle.removeMateriel(materiel));
        repository.delete(materiel);
    }

    @Override
    @Transactional
    public Materiel archiver(Long id) {
        Materiel materiel = trouver(id);
        if (materiel.isArchive())
            throw new ServiceException("Ce matériel est déjà archivé");
        Salle salle = salleService.salleDuMateriel(id).orElse(null);
        if (salle == null || salle.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("Un matériel affecté ne peut pas être archivé, remets-le en stock d'abord");
        salle.removeMateriel(materiel);
        materiel.setArchive(true);
        materiel.setDateArchivage(LocalDate.now());
        return materiel;
    }

    @Override
    @Transactional(readOnly = true)
    public EtatMateriel etatDe(Materiel materiel) {
        if (materiel.isArchive()) return materiel.calculerEtat(false, false);
        boolean enStock = salleService.estEnStock(materiel.getId());
        boolean defaillanceOuverte = defaillanceService.aUneDefaillanceOuverte(materiel.getId());
        return materiel.calculerEtat(enStock, defaillanceOuverte);
    }

    private Materiel trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ServiceException("Matériel " + id + " introuvable"));
    }
}