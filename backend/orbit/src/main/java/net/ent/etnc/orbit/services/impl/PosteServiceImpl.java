package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.repositories.PosteRepository;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosteServiceImpl extends AbstractService<Poste, PosteRepository> implements PosteService {

    @Autowired
    public PosteServiceImpl(PosteRepository posteRepository) {
        super(posteRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contientMateriel(Long idMateriel) {
        return repository.existsByMateriels_Id(idMateriel);
    }
}