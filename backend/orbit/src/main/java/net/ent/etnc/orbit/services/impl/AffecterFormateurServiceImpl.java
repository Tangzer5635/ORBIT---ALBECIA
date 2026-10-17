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

import java.util.Objects;
import java.util.Optional;

@Service
public class AffecterFormateurServiceImpl implements AffecterFormateurService {

    private final SalleService salleService;
    private final PersonnelService personnelService;
    private final JournalisationService journalisationService;

    public AffecterFormateurServiceImpl(SalleService salleService, PersonnelService personnelService,
                                        JournalisationService journalisationService) {
        this.salleService = salleService;
        this.personnelService = personnelService;
        this.journalisationService = journalisationService;
    }

    /** Un formateur peut avoir plusieurs salles, et une salle plusieurs formateurs. */
    @Override
    @Transactional
    public Salle affecter(Long idSalle, Long idFormateur) {
        Salle salle = trouverSalle(idSalle);
        Personnel formateur = personnelService.findById(idFormateur)
                .orElseThrow(() -> new ServiceException("Personnel " + idFormateur + " introuvable"));

        if (salle.getType() != TypeSalle.SalleDeCours)
            throw new ServiceException("Seule une salle de cours a des formateurs");
        if (formateur.getRole() != Role.FORMATEUR)
            throw new ServiceException(formateur.getPrenom() + " " + formateur.getNom() + " n'est pas formateur");
        if (affecte(salle, idFormateur).isPresent())
            throw new ServiceException("Ce formateur est déjà affecté à la salle " + salle.getNumSalle());

        salle.addPersonnel(formateur);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                "Formateur " + Libelles.personne(formateur) + " attribué à la " + Libelles.salle(salle));
        return salle;
    }

    @Override
    @Transactional
    public Salle retirer(Long idSalle, Long idFormateur) {
        Salle salle = trouverSalle(idSalle);
        Personnel formateur = affecte(salle, idFormateur)
                .orElseThrow(() -> new ServiceException("Ce formateur n'est pas affecté à la salle " + salle.getNumSalle()));

        salle.removePersonnel(formateur);
        journalisationService.journaliser(TypeAction.RETRAIT,
                "Formateur " + Libelles.personne(formateur) + " retiré de la " + Libelles.salle(salle));
        return salle;
    }

    private Salle trouverSalle(Long idSalle) {
        return salleService.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
    }

    private static Optional<Personnel> affecte(Salle salle, Long idFormateur) {
        return salle.getPersonnels().stream().filter(p -> Objects.equals(p.getId(), idFormateur)).findFirst();
    }
}