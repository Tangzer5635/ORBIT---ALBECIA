package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BatimentServiceImpl extends AbstractService<Batiment, BatimentRepository> implements BatimentService {

    @Autowired
    public BatimentServiceImpl(BatimentRepository batimentRepository) {
        super(batimentRepository);
    }
}