package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.entities.SurchargeProcessus;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Surcharges de la liste blanche / noire par les formateurs.
 *
 * <p>La liste globale (page Processus) s'applique partout. Un formateur peut la surcharger pour un processus :
 * sa surcharge vaut dans toutes ses salles, même pour un processus d'un master (il peut le passer en noire).
 * Dans une salle qui a plusieurs formateurs, la surcharge la plus récente l'emporte.
 * Un processus en liste noire dans la salle est arrêté automatiquement sur ses postes.
 */
public interface SurchargeProcessusService {

    /**
     * Un processus vu depuis une salle.
     *
     * @param liste     liste qui s'applique dans la salle (surcharge retenue, sinon liste globale)
     * @param retenue   surcharge qui s'applique dans la salle (null : liste globale)
     * @param maSurcharge surcharge de l'utilisateur connecté s'il est formateur (null sinon), même si elle n'est pas retenue
     */
    record ProcessusEnSalle(Processus processus, ListeProcessus liste, SurchargeProcessus retenue,
                            SurchargeProcessus maSurcharge) {}

    /** Ce qui est interdit sur la machine d'une UC (repérée par son adresse MAC). */
    record ListeNoireMachine(String nomMachine, String numPoste, Set<String> nomsInterdits) {}

    /** Tous les processus avec la liste qui s'applique dans cette salle, triés par nom. */
    List<ProcessusEnSalle> processusDeLaSalle(Long idSalle, Personnel utilisateur) throws ServiceException;

    /** Le formateur passe ce processus dans cette liste pour toutes ses salles. */
    SurchargeProcessus surcharger(Long idProcessus, ListeProcessus liste, Personnel formateur) throws ServiceException;

    /** Le formateur revient à la liste globale pour ce processus (bouton « Réinitialiser »). */
    void reinitialiser(Long idProcessus, Personnel formateur) throws ServiceException;

    /**
     * Noms (en minuscules) des processus en liste noire dans la salle du poste de cette UC.
     * Vide si la MAC ne correspond à aucune UC posée sur un poste.
     */
    Optional<ListeNoireMachine> listeNoireDeLaMachine(String mac) throws ServiceException;
}
