package net.ent.etnc.orbit.init;

import net.ent.etnc.orbit.models.entities.User;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.services.UserService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class Init implements CommandLineRunner {

    private User admin;
    private UserService userService;

    public Init(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userService.count() > 0) return;
        initUsers();
    }

    private void initUsers() throws ServiceException {
        admin = userService.create(buildUser("admin", "admin1234", Role.ADMIN));
    }

    private User buildUser(String username, String password, Role role) {

        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setRole(role);
        u.setActive(true);

        return u;
    }
}