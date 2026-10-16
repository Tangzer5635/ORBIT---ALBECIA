package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.services.commons.Service;

import java.util.Optional;

public interface MaterielService extends Service<Materiel, Long> {
    Materiel creerEnStock(Materiel materiel, Long idStockage);
    Materiel archiver(Long id);
    EtatMateriel etatDe(Materiel materiel);
    /** Salle où se trouve le matériel (vide s'il est archivé). */
    Optional<Salle> salleDe(Materiel materiel);

    /** Fiche d'un matériel : le formateur ne lit que le matériel de ses salles (sur un poste ou posé dans la salle). */
    Optional<Materiel> findPourUtilisateur(Long id, Personnel utilisateur);
    void desinstallerPartout(Processus processus);
}