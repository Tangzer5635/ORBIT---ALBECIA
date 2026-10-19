package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.AffecterFormateurService;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Service
public class AffecterFormateurServiceImpl implements AffecterFormateurService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SalleService salleService;
    private final PersonnelService personnelService;
    private final JournalisationService journalisationService;

    public AffecterFormateurServiceImpl(SalleService salleService, PersonnelService personnelService,
                                        JournalisationService journalisationService) {
        this.salleService = salleService;
        this.personnelService = personnelService;
        this.journalisationService = journalisationService;
    }

    /**
     * Un formateur peut avoir plusieurs salles, et une salle plusieurs formateurs.
     */
    @Override
    @Transactional
    public Salle affecter(Long idSalle, Long idFormateur, LocalDate jusqu) throws ServiceException {
        verifierDate(jusqu);
        Salle salle = trouverSalle(idSalle);
        Personnel formateur = personnelService.findById(idFormateur)
                .orElseThrow(() -> new ServiceException("Personnel " + idFormateur + " introuvable"));

        if (salle.getType() != TypeSalle.SalleDeCours)
            throw new ServiceException("Seule une salle de cours a des formateurs");
        if (formateur.getRole() != Role.FORMATEUR)
            throw new ServiceException(formateur.getPrenom() + " " + formateur.getNom() + " n'est pas formateur");
        if (salle.personnel(idFormateur).isPresent())
            throw new ServiceException("Ce formateur est déjà affecté à la salle " + salle.getNumSalle()
                    + " : prolongez son accès");

        salle.addPersonnel(formateur, jusqu);
        journalisationService.journaliser(TypeAction.AFFECTATION, "Formateur " + Libelles.personne(formateur)
                + " attribué à la " + Libelles.salle(salle) + " jusqu'au " + jusqu.format(DATE_FR));
        return salle;
    }

    @Override
    @Transactional
    public Salle prolonger(Long idSalle, Long idFormateur, LocalDate jusqu) {
        verifierDate(jusqu);
        Salle salle = trouverSalle(idSalle);
        Personnel formateur = salle.personnel(idFormateur)
                .orElseThrow(() -> new ServiceException("Ce formateur n'est pas affecté à la salle " + salle.getNumSalle()));

        salle.addPersonnel(formateur, jusqu); // remplace l'ancienne date
        journalisationService.journaliser(TypeAction.MODIFICATION, "Accès du formateur " + Libelles.personne(formateur)
                + " à la " + Libelles.salle(salle) + " jusqu'au " + jusqu.format(DATE_FR));
        return salle;
    }

    @Override
    @Transactional
    public Salle retirer(Long idSalle, Long idFormateur) throws ServiceException {
        Salle salle = trouverSalle(idSalle);
        Personnel formateur = salle.personnel(idFormateur)
                .orElseThrow(() -> new ServiceException("Ce formateur n'est pas affecté à la salle " + salle.getNumSalle()));

        salle.removePersonnel(formateur);
        journalisationService.journaliser(TypeAction.RETRAIT,
                "Formateur " + Libelles.personne(formateur) + " retiré de la " + Libelles.salle(salle));
        return salle;
    }

    /**
     * Date limite obligatoire, aujourd'hui au plus tôt (l'accès dure jusqu'au soir de cette date).
     */
    private static void verifierDate(LocalDate jusqu) {
        if (jusqu == null)
            throw new ServiceException("La date limite d'accès est obligatoire");
        if (jusqu.isBefore(LocalDate.now()))
            throw new ServiceException("La date limite d'accès est déjà passée");
    }

    private Salle trouverSalle(Long idSalle) throws ServiceException {
        return salleService.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
    }

    private static Optional<Personnel> affecte(Salle salle, Long idFormateur) throws ServiceException {
        return salle.getPersonnels().stream().filter(p -> Objects.equals(p.getId(), idFormateur)).findFirst();
    }
}