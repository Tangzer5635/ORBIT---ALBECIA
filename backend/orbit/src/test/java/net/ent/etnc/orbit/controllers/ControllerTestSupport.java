package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.jwt.JwtUtils;
import net.ent.etnc.orbit.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Base commune des tests de controllers.
 *
 * <p>Chaque classe de test porte {@code @WebMvcTest(XxxController.class)} et
 * {@code @Import(WebSecurityConfig.class)} : la vraie configuration de sécurité est chargée,
 * donc les {@code @PreAuthorize}, le 401 sans authentification et le {@code GlobalExceptionHandler}
 * sont testés tels qu'ils tournent en production. Seuls le filtre JWT et le chargement des
 * utilisateurs sont mockés : l'utilisateur connecté est injecté avec {@link #connecte(Role)}.
 */
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    /** Dépendances de AuthJwtFilter et de WebSecurityConfig. */
    @MockitoBean
    protected JwtUtils jwtUtils;

    @MockitoBean
    protected UserDetailsServiceImpl userDetailsService;

    /** Personnel de test : ses autorités suivent la hiérarchie de l'enum Role. */
    protected static Personnel personnel(Role role) {
        Personnel p = new Personnel();
        p.setId(1L);
        p.setNid("1000000001");
        p.setNom("Test");
        p.setPrenom(role.getLibelle());
        p.setLogin(role.name().toLowerCase());
        p.setMotDePasse("secret");
        p.setRole(role);
        p.setActive(true);
        return p;
    }

    /** Requête faite par un utilisateur connecté avec ce rôle (et les rôles qu'il hérite). */
    protected static RequestPostProcessor connecte(Role role) {
        return user(personnel(role));
    }
}
