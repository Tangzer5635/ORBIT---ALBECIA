package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.services.commons.Service;

public interface MaterielService extends Service<Materiel, Long> {
    Materiel creerEnStock(Materiel materiel, Long idStockage);
    Materiel archiver(Long id);
    EtatMateriel etatDe(Materiel materiel);
}