package net.ent.etnc.orbit.init;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Init implements CommandLineRunner {

    private Personnel admin;
    private PersonnelService personnelService;

    public Init(PersonnelService personnelService) {
        this.personnelService = personnelService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (personnelService.count() > 0) return;
        initUsers();
    }

    private void initUsers() throws ServiceException {
        admin = personnelService.create(buildUser("ADM001", "Admin", "Orbit", "admin", "admin1234", Role.ADMIN));
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
}