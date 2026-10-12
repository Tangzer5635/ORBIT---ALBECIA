package net.ent.etnc.orbit.controllers.security;

import jakarta.servlet.http.Cookie;
import net.ent.etnc.orbit.controllers.ControllerTestSupport;
import net.ent.etnc.orbit.dtos.assemblers.UserAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.UserResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.RefreshTokenService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
class AuthControllerTest extends ControllerTestSupport {

    private static final String COOKIE = "orbit_refresh";
    private static final String JSON_LOGIN = "{\"username\":\"t.lebuhe\",\"password\":\"1595\"}";

    /** Remplace l'AuthenticationManager réel : on teste le controller, pas Spring Security. */
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private UserAssembler userAssembler;

    private Personnel stagiaire() {
        return personnel(Role.STAGIAIRE);
    }

    // ---------- login ----------

    @Test
    void login_doitRenvoyer200_avecTokenEtCookie() throws Exception {
        Personnel user = stagiaire();
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(jwtUtils.generateJwtToken(user)).thenReturn("jwt-de-test");
        when(refreshTokenService.createRefreshToken(user.getUsername())).thenReturn("refresh-brut");
        when(userAssembler.toDto(user)).thenReturn(UserResponseDto.builder().id(1L).login("stagiaire").role(Role.STAGIAIRE).build());

        mockMvc.perform(post("/api/v1/auth/login/")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-de-test"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.utilisateur.login").value("stagiaire"))
                .andExpect(cookie().value(COOKIE, "refresh-brut"))
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andExpect(cookie().path(COOKIE, "/api/v1/auth"));
    }

    @Test
    void login_doitRenvoyer401_quandMauvaisIdentifiants() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login/")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Identifiants invalides"));
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void login_doitRenvoyer403_quandCompteDesactive() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        mockMvc.perform(post("/api/v1/auth/login/")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_LOGIN))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Compte désactivé"));
    }

    @Test
    void login_doitRenvoyer423_quandCompteVerrouille() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        mockMvc.perform(post("/api/v1/auth/login/")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_LOGIN))
                .andExpect(status().isLocked())
                .andExpect(content().string("Compte verrouillé"));
    }

    @Test
    void login_doitRenvoyer400_quandMotDePasseManquant() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login/")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"t.lebuhe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
        verify(authenticationManager, never()).authenticate(any());
    }

    // ---------- refresh ----------

    @Test
    void refresh_doitRenvoyer200_etFaireTournerLeCookie() throws Exception {
        Personnel user = stagiaire();
        when(refreshTokenService.rotateRefreshToken("ancien"))
                .thenReturn(new RefreshTokenService.RotateResult("nouveau", user.getUsername()));
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtUtils.generateJwtToken(user)).thenReturn("jwt-rafraichi");

        mockMvc.perform(post("/api/v1/auth/refresh/").cookie(new Cookie(COOKIE, "ancien")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-rafraichi"))
                .andExpect(cookie().value(COOKIE, "nouveau"));
    }

    @Test
    void refresh_doitRenvoyer400_quandCookieAbsent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh/"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cookie de refresh manquant"));
        verify(refreshTokenService, never()).rotateRefreshToken(any());
    }

    @Test
    void refresh_doitRenvoyer400_quandTokenRevoque() throws Exception {
        when(refreshTokenService.rotateRefreshToken("vole"))
                .thenThrow(new ServiceException("RG-SEC02", "Refresh token révoqué — toutes vos sessions ont été fermées"));

        mockMvc.perform(post("/api/v1/auth/refresh/").cookie(new Cookie(COOKIE, "vole")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_doitRenvoyer400_etRevoquer_quandCompteDesactive() throws Exception {
        Personnel user = stagiaire();
        user.setActive(false);
        when(refreshTokenService.rotateRefreshToken("ancien"))
                .thenReturn(new RefreshTokenService.RotateResult("nouveau", user.getUsername()));
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/refresh/").cookie(new Cookie(COOKIE, "ancien")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Compte désactivé"));
        verify(refreshTokenService).revokeAllForUser(user.getUsername());
    }

    @Test
    void refresh_doitRenvoyer400_etRevoquer_quandCompteVerrouille() throws Exception {
        Personnel user = stagiaire();
        user.setVerrouille(true);
        when(refreshTokenService.rotateRefreshToken("ancien"))
                .thenReturn(new RefreshTokenService.RotateResult("nouveau", user.getUsername()));
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/refresh/").cookie(new Cookie(COOKIE, "ancien")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Compte verrouillé"));
        verify(refreshTokenService).revokeAllForUser(user.getUsername());
    }

    // ---------- logout ----------

    @Test
    void logout_doitRenvoyer204_revoquerEtEffacerLeCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout/").cookie(new Cookie(COOKIE, "courant")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE, 0));
        verify(refreshTokenService).revokeToken("courant");
    }

    @Test
    void logout_doitRenvoyer204_sansCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout/"))
                .andExpect(status().isNoContent());
        verify(refreshTokenService, never()).revokeToken(any());
    }
}
