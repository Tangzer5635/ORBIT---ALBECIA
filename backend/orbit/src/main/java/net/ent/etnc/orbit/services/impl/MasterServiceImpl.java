package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.repositories.MasterRepository;
import net.ent.etnc.orbit.services.MasterService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MasterServiceImpl extends AbstractService<Master, MasterRepository> implements MasterService {

    @Autowired
    public MasterServiceImpl(MasterRepository masterRepository) {
        super(masterRepository);
    }
}