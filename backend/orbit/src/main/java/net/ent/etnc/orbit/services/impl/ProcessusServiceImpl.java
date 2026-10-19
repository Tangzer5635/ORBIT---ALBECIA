package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.repositories.ProcessusRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MasterService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.ProcessusService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProcessusServiceImpl extends AbstractService<Processus, ProcessusRepository> implements ProcessusService {

    private final MaterielService materielService;
    private final MasterService masterService;
    private final JournalisationService journalisationService;

    @Autowired
    public ProcessusServiceImpl(ProcessusRepository processusRepository, MaterielService materielService,
                                MasterService masterService, JournalisationService journalisationService) {
        super(processusRepository);
        this.materielService = materielService;
        this.masterService = masterService;
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional
    public Processus create(Processus processus) {
        if (repository.existsByNom(processus.getNom()))
            throw new ServiceException("Le processus " + processus.getNom() + " existe déjà");
        Processus cree = super.create(processus);
        journalisationService.journaliser(TypeAction.CREATION,
                "Processus « " + processus.getNom() + " » créé (liste " + processus.getListe() + ")");
        return cree;
    }

    @Override
    @Transactional
    public Processus changerListe(Long id, ListeProcessus liste) {
        Processus processus = repository.findById(id)
                .orElseThrow(() -> new ServiceException("Processus " + id + " introuvable"));
        if (processus.getListe() == liste)
            throw new ServiceException("Le processus " + processus.getNom() + " est déjà en liste " + liste.name().toLowerCase());
        if (liste == ListeProcessus.NOIRE) {
            // la liste blanche a toujours le dessus : un processus d'un master reste autorisé
            List<Master> masters = masterService.mastersAvec(id);
            if (!masters.isEmpty())
                throw new ServiceException("Le processus " + processus.getNom() + " fait partie du master "
                        + masters.stream().map(Master::getNom).collect(Collectors.joining(", "))
                        + " : retirez-le d'abord du master pour le passer en liste noire");
            materielService.desinstallerPartout(processus);
        }
        processus.setListe(liste);
        journalisationService.journaliser(TypeAction.MODIFICATION, "Processus « " + processus.getNom()
                + " » passé en liste " + liste.name().toLowerCase()
                + (liste == ListeProcessus.NOIRE ? " (désinstallé de toutes les UC)" : ""));
        return processus;
    }

    /** Le processus est d'abord retiré des masters et désinstallé de toutes les UC. */
    @Override
    @Transactional
    public void deleteById(Long id) {
        Processus processus = repository.findById(id)
                .orElseThrow(() -> new ServiceException("Processus " + id + " introuvable"));
        masterService.retirerProcessusPartout(processus);
        materielService.desinstallerPartout(processus);
        repository.delete(processus);
        journalisationService.journaliser(TypeAction.SUPPRESSION,
                "Processus « " + processus.getNom() + " » supprimé (retiré des masters et désinstallé de toutes les UC)");
    }
}