package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.repositories.ProcessusRepository;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.ProcessusService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessusServiceImpl extends AbstractService<Processus, ProcessusRepository> implements ProcessusService {

    private final MaterielService materielService;

    @Autowired
    public ProcessusServiceImpl(ProcessusRepository processusRepository, MaterielService materielService) {
        super(processusRepository);
        this.materielService = materielService;
    }

    @Override
    @Transactional
    public Processus create(Processus processus) {
        if (repository.existsByNom(processus.getNom()))
            throw new ServiceException("Le processus " + processus.getNom() + " existe déjà");
        return super.create(processus);
    }

    /** Le processus est d'abord désinstallé de toutes les UC. */
    @Override
    @Transactional
    public void deleteById(Long id) {
        Processus processus = repository.findById(id)
                .orElseThrow(() -> new ServiceException("Processus " + id + " introuvable"));
        materielService.desinstallerPartout(processus);
        repository.delete(processus);
    }
}