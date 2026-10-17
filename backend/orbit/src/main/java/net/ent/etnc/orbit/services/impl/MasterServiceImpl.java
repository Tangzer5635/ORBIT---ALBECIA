package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.repositories.MasterRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MasterService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MasterServiceImpl extends AbstractService<Master, MasterRepository> implements MasterService {

    private final JournalisationService journalisationService;

    @Autowired
    public MasterServiceImpl(MasterRepository masterRepository, JournalisationService journalisationService) {
        super(masterRepository);
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional
    public Master create(Master master) {
        Master cree = super.create(master);
        journalisationService.journaliser(TypeAction.CREATION, libelle(master) + " créé");
        return cree;
    }

    @Override
    @Transactional
    public Master update(Master master) {
        Master modifie = super.update(master);
        journalisationService.journaliser(TypeAction.MODIFICATION, libelle(master) + " modifié");
        return modifie;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Optional<Master> master = repository.findById(id); // lu avant la suppression, pour le journal
        super.deleteById(id);
        master.ifPresent(m -> journalisationService.journaliser(TypeAction.SUPPRESSION, libelle(m) + " supprimé"));
    }

    /** "Master « Dev » (version 1.2)" */
    private static String libelle(Master m) {
        return "Master « " + m.getNom() + " » (version " + m.getVersionMaster() + ")";
    }
}