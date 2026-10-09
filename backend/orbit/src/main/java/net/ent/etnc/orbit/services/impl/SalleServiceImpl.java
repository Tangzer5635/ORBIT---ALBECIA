package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.repositories.SalleRepository;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SalleServiceImpl extends AbstractService<Salle, SalleRepository> implements SalleService {

    @Autowired
    public SalleServiceImpl(SalleRepository salleRepository) {
        super(salleRepository);
    }
}