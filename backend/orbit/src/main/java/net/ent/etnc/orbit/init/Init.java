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

    public Init(PersonnelService personnelService, MaterielService materielService,
                BatimentService batimentService, SalleService salleService,
                PosteService posteService, AffecterMaterielService affecterMaterielService) {
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
        // Évite de recréer toutes les données à chaque démarrage.
        if (personnelService.count() > 0) return;

        personnelService.create(buildUser("1000000001", "Admin", "Orbit",
                "admin", "admin1234", Role.ADMINISTRATEUR));
        personnelService.create(buildUser("3000000001", "Besnard", "Romain",
                "r.besnard", "12345", Role.GESTIONNAIRE));
        personnelService.create(buildUser("3000000002", "Martin", "Alice",
                "a.martin", "12345", Role.GESTIONNAIRE));

        Personnel formateur1 = personnelService.create(buildUser("2000000001", "Ledeme",
                "Kevin", "k.ledeme", "1324", Role.FORMATEUR));
        Personnel formateur2 = personnelService.create(buildUser("2000000002", "Durand",
                "Sophie", "s.durand", "1324", Role.FORMATEUR));

        Personnel stagiaire1 = personnelService.create(buildUser("4000000001", "Le Buhé",
                "Tanguy", "t.lebuhe", "1595", Role.STAGIAIRE));
        Personnel stagiaire2 = personnelService.create(buildUser("4000000002", "Bernard",
                "Marc", "m.bernard", "1595", Role.STAGIAIRE));
        Personnel stagiaire3 = personnelService.create(buildUser("4000000003", "Petit",
                "Julie", "j.petit", "1595", Role.STAGIAIRE));
        Personnel inactif = personnelService.create(buildUser("4000000004", "Inactif",
                "Paul", "p.inactif", "1595", Role.STAGIAIRE));
        Personnel verrouille = personnelService.create(buildUser("4000000005", "Verrouille",
                "Paul", "p.verrouille", "1595", Role.STAGIAIRE));
        personnelService.deactivate(inactif.getId());
        verrouille.setVerrouille(true);

        Personnel[] occupants = {stagiaire1, stagiaire2, stagiaire3, null, null};
        Personnel[] formateurs = {formateur1, formateur2};

        // Même liste de 15 bâtiments que dans l'initialisation précédente.
        String[] numerosBatiments = {"351", "352", "215", "216", "208",
                "209", "213", "214", "207", "206", "205", "204", "203", "202", "201"};

        int numeroMateriel = 1;
        int numeroPosteGlobal = 1;

        for (int b = 0; b < numerosBatiments.length; b++) {
            Batiment batiment = batimentService.create(buildBatiment(numerosBatiments[b]));
            Salle stockage = stockageDe(batiment);

            // 15 salles de cours par bâtiment. La salle de stockage automatique
            // du bâtiment est conservée en plus de ces 15 salles.
            for (int s = 1; s <= 15; s++) {
                String numSalle = String.format("%s%02d", numerosBatiments[b], s);
                Salle salle = batimentService.create(batiment.getId(),
                        buildSalle(numSalle, Integer.toString((s - 1) / 5 + 1),
                                TypeSalle.SalleDeCours));

                if (s % 2 == 1) {
                    salle.addPersonnel(formateurs[(s + b) % formateurs.length]);
                }

                // 2 postes par salle = 30 postes par bâtiment.
                for (int p = 1; p <= 30; p++) {
                    String numPoste = String.format("B%s-S%02d-P%02d",
                            numerosBatiments[b], s, p);
                    Personnel occupant = occupants[(b + s + p) % occupants.length];
                    Poste poste = creerPoste(numPoste, salle, occupant);

                    // Varie les dates de garantie pour tester les états calculés
                    // par l'application : garantie expirée, proche ou encore valide.
                    int anneeFinGarantie;
                    int variante = (numeroPosteGlobal + p + s) % 4;
                    if (variante == 0) anneeFinGarantie = 2025;
                    else if (variante == 1) anneeFinGarantie = 2027;
                    else if (variante == 2) anneeFinGarantie = 2028;
                    else anneeFinGarantie = 2030;

                    Materiel uc = creerMateriel(
                            String.format("UC-%05d", numeroMateriel++),
                            "Dell OptiPlex 7010", anneeFinGarantie, TypeMateriel.UC, stockage);
                    Materiel ecran = creerMateriel(
                            String.format("EC-%05d", numeroMateriel++),
                            "Dell P2422H", anneeFinGarantie, TypeMateriel.Ecran, stockage);

                    affecterMaterielService.affecterAPoste(poste.getId(), uc.getId());
                    affecterMaterielService.affecterAPoste(poste.getId(), ecran.getId());
                    numeroPosteGlobal++;
                }
            }
        }

        // Matériel disponible en stock pour tester les affectations.
        Salle stockagePrincipal = salleService.create(
                buildSalle("999", "0", TypeSalle.SalleStockage));
        creerMateriel("STOCK-UC-0001", "Dell OptiPlex 7010", 2028,
                TypeMateriel.UC, stockagePrincipal);
        creerMateriel("STOCK-EC-0001", "Dell P2422H", 2028,
                TypeMateriel.Ecran, stockagePrincipal);
        creerMateriel("STOCK-VP-0001", "Epson EB-W51", 2028,
                TypeMateriel.VideoProjecteur, stockagePrincipal);
    }

    private Poste creerPoste(String numPoste, Salle salle, Personnel occupant)
            throws ServiceException {
        Poste poste = new Poste();
        poste.setNumPoste(numPoste);
        poste.setOccupant(occupant);
        Poste cree = posteService.create(poste);
        salle.addPoste(cree);
        return cree;
    }

    private Materiel creerMateriel(String numSerie, String modele, int anneeFinGarantie,
                                   TypeMateriel type, Salle stockage) throws ServiceException {
        Materiel materiel = new Materiel();
        materiel.setNumSerie(numSerie);
        materiel.setModele(modele);
        materiel.setDateAcquisition(LocalDate.of(anneeFinGarantie - 3, 9, 1));
        materiel.setDateFinGarantie(LocalDate.of(anneeFinGarantie, 9, 1));
        materiel.setType(type);
        return materielService.creerEnStock(materiel, stockage.getId());
    }

    private Personnel buildUser(String nid, String nom, String prenom, String login,
                                String password, Role role) {
        Personnel utilisateur = new Personnel();
        utilisateur.setNid(nid);
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setLogin(login);
        utilisateur.setMotDePasse(password);
        utilisateur.setRole(role);
        utilisateur.setActive(true);
        return utilisateur;
    }

    private Salle buildSalle(String numSalle, String etage, TypeSalle type) {
        Salle salle = new Salle();
        salle.setNumSalle(numSalle);
        salle.setEtage(etage);
        salle.setType(type);
        return salle;
    }

    private Salle stockageDe(Batiment batiment) {
        return batiment.getSalles().stream()
                .filter(salle -> salle.getType() == TypeSalle.SalleStockage)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucune salle de stockage pour le bâtiment "
                                + batiment.getNumBatiment()));
    }

    private Batiment buildBatiment(String numBatiment) {
        Batiment batiment = new Batiment();
        batiment.setNumBatiment(numBatiment);
        return batiment;
    }
}
