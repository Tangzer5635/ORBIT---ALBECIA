package net.ent.etnc.orbit.init;

import net.ent.etnc.orbit.models.entities.*;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.*;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class Init implements CommandLineRunner {

    private final PersonnelService personnelService;
    private final MaterielService materielService;
    private final BatimentService batimentService;
    private final SalleService salleService;
    private final PosteService posteService;
    private final AffecterMaterielService affecterMaterielService;

    public Init(PersonnelService personnelService, MaterielService materielService, BatimentService batimentService,
                SalleService salleService, PosteService posteService, AffecterMaterielService affecterMaterielService) {
        this.personnelService = personnelService;
        this.materielService = materielService;
        this.batimentService = batimentService;
        this.salleService = salleService;
        this.posteService = posteService;
        this.affecterMaterielService = affecterMaterielService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (personnelService.count() > 0) return;

        // ---------- Personnels ----------
        personnelService.create(buildUser("1000000001", "Admin", "Orbit", "admin", "admin1234", Role.ADMINISTRATEUR));
        Personnel besnard = personnelService.create(buildUser("3000000001", "Besnard", "Romain", "r.besnard", "12345", Role.GESTIONNAIRE));
        Personnel martin = personnelService.create(buildUser("3000000002", "Martin", "Alice", "a.martin", "12345", Role.GESTIONNAIRE));
        Personnel ledeme = personnelService.create(buildUser("2000000001", "Ledeme", "Kevin", "k.ledeme", "1324", Role.FORMATEUR));
        Personnel durand = personnelService.create(buildUser("2000000002", "Durand", "Sophie", "s.durand", "1324", Role.FORMATEUR));
        Personnel lebuhe = personnelService.create(buildUser("4000000001", "Le Buhé", "Tanguy", "t.lebuhe", "1595", Role.STAGIAIRE));
        Personnel bernard = personnelService.create(buildUser("4000000002", "Bernard", "Marc", "m.bernard", "1595", Role.STAGIAIRE));
        Personnel petit = personnelService.create(buildUser("4000000003", "Petit", "Julie", "j.petit", "1595", Role.STAGIAIRE));
        Personnel inactif = personnelService.create(buildUser("4000000004", "Inactif", "Paul", "p.inactif", "1595", Role.STAGIAIRE));
        personnelService.deactivate(inactif.getId());

        // ---------- Bâtiments + salles (l'ordre fixe les ids utilisés dans materiel-tests.http) ----------
        Batiment b201 = batimentService.create(buildBatiment("B201"));
        Batiment b305 = batimentService.create(buildBatiment("B305"));

        Salle s101 = batimentService.creerSalle(b201.getId(), buildSalle("101", "1", TypeSalle.SalleDeCours));   // 1
        Salle s102 = batimentService.creerSalle(b201.getId(), buildSalle("102", "1", TypeSalle.SalleDeCours));   // 2
        Salle r201 = batimentService.creerSalle(b201.getId(), buildSalle("199", "1", TypeSalle.SalleStockage));  // 3 : réserve B201
        Salle s305 = batimentService.creerSalle(b305.getId(), buildSalle("101", "1", TypeSalle.SalleDeCours));   // 4 : même numéro, autre bâtiment
        Salle r305 = batimentService.creerSalle(b305.getId(), buildSalle("199", "0", TypeSalle.SalleStockage));  // 5 : réserve B305
        salleService.create(buildSalle("999", "0", TypeSalle.SalleStockage));                                    // 6 : réserve sans bâtiment

        salleService.changerGestionnaire(s101.getId(), besnard.getId());
        salleService.changerGestionnaire(s102.getId(), besnard.getId());
        salleService.changerGestionnaire(r201.getId(), besnard.getId());
        salleService.changerGestionnaire(s305.getId(), martin.getId());
        salleService.changerGestionnaire(r305.getId(), martin.getId());

        s101.addPersonnel(ledeme);
        s102.addPersonnel(ledeme);
        s305.addPersonnel(durand);

        // ---------- Postes ----------
        Poste p1 = creerPoste("P101-01", s101, lebuhe);   // 1
        Poste p2 = creerPoste("P101-02", s101, bernard);  // 2
        Poste p3 = creerPoste("P102-01", s102, petit);    // 3
        creerPoste("P305-01", s305, null);                // 4 : poste vide

        // ---------- Matériels (tous créés en stock dans la réserve B201) ----------
        Materiel uc1 = creerMateriel("UC-0001", "Dell OptiPlex 7010", 2027, TypeMateriel.UC, r201);                   // 1
        Materiel ec1 = creerMateriel("EC-0001", "Dell P2422H", 2027, TypeMateriel.Ecran, r201);                       // 2
        Materiel uc2 = creerMateriel("UC-0002", "Dell OptiPlex 7010", 2027, TypeMateriel.UC, r201);                   // 3
        Materiel ec2 = creerMateriel("EC-0002", "Dell P2422H", 2027, TypeMateriel.Ecran, r201);                       // 4
        Materiel uc3 = creerMateriel("UC-0003", "HP EliteDesk 800", 2025, TypeMateriel.UC, r201);                     // 5 : garantie expirée -> A_REMPLACER
        Materiel tbi = creerMateriel("TBI-0001", "Promethean ActivPanel", 2028, TypeMateriel.TableauInteractif, r201); // 6
        Materiel vp1 = creerMateriel("VP-0001", "Epson EB-W51", 2027, TypeMateriel.VideoProjecteur, r201);            // 7
        creerMateriel("UC-0004", "Dell OptiPlex 7010", 2028, TypeMateriel.UC, r201);                                  // 8  : stock
        creerMateriel("EC-0003", "Dell P2422H", 2028, TypeMateriel.Ecran, r201);                                      // 9  : stock
        creerMateriel("IMP-0001", "HP LaserJet M404", 2027, TypeMateriel.Imprimante, r201);                           // 10 : stock
        creerMateriel("VP-0002", "Epson EB-W51", 2028, TypeMateriel.VideoProjecteur, r201);                           // 11 : stock
        Materiel vieux = creerMateriel("UC-0099", "Dell OptiPlex 3020", 2020, TypeMateriel.UC, r201);                 // 12 : archivé

        // ---------- Affectations ----------
        affecterMaterielService.affecterAPoste(p1.getId(), uc1.getId());
        affecterMaterielService.affecterAPoste(p1.getId(), ec1.getId());
        affecterMaterielService.affecterAPoste(p2.getId(), uc2.getId());
        affecterMaterielService.affecterAPoste(p2.getId(), ec2.getId());
        affecterMaterielService.affecterAPoste(p3.getId(), uc3.getId());
        affecterMaterielService.affecterASalle(s101.getId(), tbi.getId());
        affecterMaterielService.affecterASalle(s102.getId(), vp1.getId());

        materielService.archiver(vieux.getId());
    }

    private Poste creerPoste(String numPoste, Salle salle, Personnel occupant) throws ServiceException {
        Poste p = new Poste();
        p.setNumPoste(numPoste);
        p.setOccupant(occupant);
        Poste cree = posteService.create(p);
        salle.addPoste(cree);
        return cree;
    }

    private Materiel creerMateriel(String numSerie, String modele, int anneeFinGarantie, TypeMateriel type, Salle stockage) throws ServiceException {
        Materiel m = new Materiel();
        m.setNumSerie(numSerie);
        m.setModele(modele);
        m.setDateAcquisition(LocalDate.of(anneeFinGarantie - 3, 9, 1));
        m.setDateFinGarantie(LocalDate.of(anneeFinGarantie, 9, 1));
        m.setType(type);
        return materielService.creerEnStock(m, stockage.getId());
    }

    private Personnel buildUser(String nid, String nom, String prenom, String login, String password, Role role) {
        Personnel u = new Personnel();
        u.setNid(nid);
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setLogin(login);
        u.setMotDePasse(password);
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    private Salle buildSalle(String numSalle, String etage, TypeSalle type) {
        Salle s = new Salle();
        s.setNumSalle(numSalle);
        s.setEtage(etage);
        s.setType(type);
        return s;
    }

    private Batiment buildBatiment(String numBatiment) {
        Batiment b = new Batiment();
        b.setNumBatiment(numBatiment);
        return b;
    }
}