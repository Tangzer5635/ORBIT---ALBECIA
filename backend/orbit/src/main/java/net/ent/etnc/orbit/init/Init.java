package net.ent.etnc.orbit.init;

import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class Init implements CommandLineRunner {

    private Personnel admin;
    private Personnel formateur;
    private Personnel gestionnaire;
    private Personnel stagiaire;

    private Materiel matos1;
    private Materiel matos2;

    private Salle salle1;
    private Salle salle2;

    private Batiment bat1;
    private PersonnelService personnelService;
    private MaterielService materielService;
    private BatimentService batimentService;
    private SalleService salleService;

    public Init(PersonnelService personnelService, MaterielService materielService, BatimentService batimentService, SalleService salleService) {
        this.personnelService = personnelService;
        this.materielService = materielService;
        this.batimentService = batimentService;
        this.salleService = salleService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (personnelService.count() > 0) return;
        initUsers();
        initMateriels();
        initSalles();
        initBatiments();
    }

    private void initUsers() throws ServiceException {
        admin = personnelService.create(buildUser("1000000001", "Admin", "Orbit", "admin", "admin1234", Role.ADMINISTRATEUR));
        formateur = personnelService.create(buildUser("2000000001", "Ledeme", "Kevin", "k.ledeme", "1324", Role.FORMATEUR));
        gestionnaire = personnelService.create(buildUser("3000000001", "Besnard", "Romain", "r.besnard", "12345", Role.GESTIONNAIRE));
        stagiaire = personnelService.create(buildUser("4000000001", "Le Buhé", "Tanguy", "t.lebuhe", "1595", Role.STAGIAIRE));
    }

    private void initMateriels() throws ServiceException {
        matos1 = materielService.create(buildMateriel("A1B2C3", "LBC1234", LocalDate.of(2026,1,20), LocalDate.of(2025,1,10), TypeMateriel.UC));
        matos2 = materielService.create(buildMateriel("Z9Y8X7", "AZE9876", LocalDate.of(2026,7,20), LocalDate.of(2026,1,13), TypeMateriel.Ecran));
    }

    private void initSalles() throws ServiceException {
        salle1 = salleService.create(buildSalle("201", "2", TypeSalle.SalleDeCours));
        salle2 = salleService.create(buildSalle("299", "2" , TypeSalle.SalleStockage));
    }

    private void initBatiments() throws ServiceException {
        bat1 = batimentService.create(buildBatiment("201", List.of(salle1, salle2)));
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

    private Materiel buildMateriel(String numSerie, String modele, LocalDate dateFinGarantie, LocalDate dateAcquisition, TypeMateriel type) {
        Materiel m = new Materiel();
        m.setNumSerie(numSerie);
        m.setModele(modele);
        m.setDateFinGarantie(dateFinGarantie);
        m.setDateAcquisition(dateAcquisition);
        m.setType(type);
        return m;
    }

    private Salle buildSalle(String numSalle, String Etage, TypeSalle type) {
        Salle s = new Salle();
        s.setNumSalle(numSalle);
        s.setEtage(Etage);
        s.setType(type);
        return s;
    }

    private Batiment buildBatiment(String numBatiment, List<Salle> salle) {
        Batiment b = new Batiment();
        b.setNumBatiment(numBatiment);
        for (Salle s: salle) {
            b.addSalle(s);
        }
        return b;
    }
}