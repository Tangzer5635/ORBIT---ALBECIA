package net.ent.etnc.orbit.init;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class Init implements CommandLineRunner {

    private Personnel admin;
    private Personnel formateur;
    private Personnel stagiaire;

    private Materiel matos1;
    private Materiel matos2;
    private PersonnelService personnelService;
    private MaterielService materielService;

    public Init(PersonnelService personnelService, MaterielService materielService) {
        this.personnelService = personnelService;
        this.materielService = materielService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (personnelService.count() > 0) return;
        initUsers();
        initMateriels();
    }

    private void initUsers() throws ServiceException {
        admin = personnelService.create(buildUser("1000000001", "Admin", "Orbit", "admin", "admin1234", Role.ADMINISTRATEUR));
        formateur = personnelService.create(buildUser("3000000001", "Ledeme", "Kevin", "k.ledeme", "1324", Role.FORMATEUR));
        stagiaire = personnelService.create(buildUser("4000000001", "Le Buhé", "Tanguy", "t.lebuhe", "1595", Role.STAGIAIRE));
    }

    private void initMateriels() throws ServiceException {
        matos1 = materielService.create(buildMateriel("A1B2C3", "LBC1234", LocalDate.of(2026,1,20), LocalDate.of(2025,1,10)));
        matos2 = materielService.create(buildMateriel("Z9Y8X7", "AZE9876", LocalDate.of(2026,7,20), LocalDate.of(2026,1,13)));
    }

    private Personnel buildUser(String nid, String nom, String prenom, String username, String password, Role role) {
        Personnel u = new Personnel();
        u.setNid(nid);
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setLogin(username);
        u.setMotDePasse(password);
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    private Materiel buildMateriel(String numSerie, String modele, LocalDate dateFinGarantie, LocalDate dateAcquisition) {
        Materiel m = new Materiel();
        m.setNumSerie(numSerie);
        m.setModele(modele);
        m.setDateFinGarantie(dateFinGarantie);
        m.setDateAcquisition(dateAcquisition);
        return m;
    }
}