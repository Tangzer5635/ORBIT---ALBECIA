package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.MasterRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MasterService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.ProcessusService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MasterServiceImpl extends AbstractService<Master, MasterRepository> implements MasterService {

    private final ProcessusService processusService;
    private final SalleService salleService;
    private final MaterielService materielService;
    private final JournalisationService journalisationService;

    /** @Lazy : ProcessusService dépend aussi de MasterService (suppression d'un processus). */
    @Autowired
    public MasterServiceImpl(MasterRepository masterRepository, @Lazy ProcessusService processusService,
                             SalleService salleService, MaterielService materielService,
                             JournalisationService journalisationService) {
        super(masterRepository);
        this.processusService = processusService;
        this.salleService = salleService;
        this.materielService = materielService;
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional
    public Master creer(Master master, List<Long> processusIds) {
        if (repository.existsByNom(master.getNom()))
            throw new ServiceException("Un master « " + master.getNom() + " » existe déjà");
        remplirProcessus(master, processusIds);
        Master cree = super.create(master);
        journalisationService.journaliser(TypeAction.CREATION, libelle(cree) + " créé ("
                + cree.getProcessus().size() + " processus)");
        return cree;
    }

    @Override
    @Transactional
    public Master modifier(Long id, Master donnees, List<Long> processusIds) {
        Master master = trouver(id);
        if (repository.existsByNomAndIdNot(donnees.getNom(), id))
            throw new ServiceException("Un master « " + donnees.getNom() + " » existe déjà");
        master.setNom(donnees.getNom());
        master.setVersionMaster(donnees.getVersionMaster());
        remplirProcessus(master, processusIds);

        // les salles où il est plaqué suivent la nouvelle image
        List<Salle> salles = salleService.sallesDuMaster(id);
        salles.forEach(s -> materielService.installerImage(s.getId(), master.getProcessus()));
        journalisationService.journaliser(TypeAction.MODIFICATION, libelle(master) + " modifié ("
                + master.getProcessus().size() + " processus" + (salles.isEmpty() ? "" : ", réinstallé sur "
                                                                                         + salles.size() + " salle(s)") + ")");
        return master;
    }

    /** Pas de retour à l'ancien master : les salles concernées n'ont plus de master ni de processus. */
    @Override
    @Transactional
    public void deleteById(Long id) {
        Master master = trouver(id);
        List<Salle> salles = salleService.sallesDuMaster(id);
        for (Salle s : salles) {
            s.setMaster(null);
            materielService.installerImage(s.getId(), List.of());
        }
        repository.delete(master);
        journalisationService.journaliser(TypeAction.SUPPRESSION, libelle(master) + " supprimé"
                + (salles.isEmpty() ? "" : " (retiré de " + salles.size() + " salle(s), processus désinstallés)"));
    }

    @Override
    @Transactional
    public Salle plaquer(Long idSalle, Long idMaster) {
        Master master = trouver(idMaster);
        Salle salle = salleService.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        if (salle.getType() != TypeSalle.SalleDeCours)
            throw new ServiceException("Un master ne se plaque que sur une salle de cours");
        if (salle.isSuspendu())
            throw new ServiceException("La salle " + salle.getNumSalle() + " est gelée : dégelez-la avant d'y plaquer un master");

        salle.setMaster(master);
        int nbUc = materielService.installerImage(idSalle, master.getProcessus());
        journalisationService.journaliser(TypeAction.AFFECTATION, libelle(master) + " plaqué sur la "
                + Libelles.salle(salle) + " (" + nbUc + " UC)");
        return salle;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Master> mastersAvec(Long idProcessus) {
        return repository.findByProcessus_Id(idProcessus);
    }

    @Override
    @Transactional
    public void retirerProcessusPartout(Processus processus) {
        repository.findByProcessus_Id(processus.getId()).forEach(m -> m.removeProcessus(processus));
    }

    /** Remplace les processus du master ; un processus en liste noire ne peut pas faire partie d'une image. */
    private void remplirProcessus(Master master, List<Long> processusIds) {
        master.viderProcessus();
        for (Long idProcessus : processusIds == null ? List.<Long>of() : processusIds) {
            Processus p = processusService.findById(idProcessus)
                    .orElseThrow(() -> new ServiceException("Processus " + idProcessus + " introuvable"));
            if (p.getListe() == ListeProcessus.NOIRE)
                throw new ServiceException("Le processus « " + p.getNom() + " » est en liste noire : il ne peut pas faire partie d'un master");
            master.addProcessus(p);
        }
    }

    private Master trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ServiceException("Master " + id + " introuvable"));
    }

    /** "Master « Dev » (version 1.2)" */
    private static String libelle(Master m) {
        return "Master « " + m.getNom() + " » (version " + m.getVersionMaster() + ")";
    }
}