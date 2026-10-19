package net.ent.etnc.orbit.controllers.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.requestDTO.LoginRequest;
import net.ent.etnc.orbit.dtos.responseDTO.TokenResponse;
import net.ent.etnc.orbit.dtos.assemblers.UserAssembler;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.security.jwt.JwtUtils;
import net.ent.etnc.orbit.security.services.UserDetailsServiceImpl;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.RefreshTokenService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * Endpoint d'authentification ({@code /api/v1/auth/}).
 *
 * <p>Stratégie token :
 * <ul>
 *   <li><strong>Access token</strong> — JWT renvoyé en JSON, à envoyer dans {@code Authorization: Bearer}.
 *       Durée configurée par {@code app.jwt.expiration-ms}.</li>
 *   <li><strong>Refresh token</strong> — valeur brute (SHA-256 stocké en base) transmise
 *       via un cookie HttpOnly ({@code orbit_refresh}), jamais exposée en JSON.</li>
 * </ul>
 *
 * <p>Endpoints :
 * <ul>
 *   <li>{@code POST /login/} — authentification par identifiant (login) / mot de passe.</li>
 *   <li>{@code POST /refresh/} — rotation du refresh token et nouvel access token.</li>
 *   <li>{@code POST /logout/} — suppression du cookie de refresh.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String COOKIE_NAME = "orbit_refresh";
    private final long refreshExpirationMs;
    private final long jwtExpirationMs;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final UserAssembler userAssembler;
    private final UserDetailsServiceImpl userDetailsService;
    private final JournalisationService journalisationService;

    public AuthController(
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            RefreshTokenService refreshTokenService,
            UserAssembler userAssembler,
            UserDetailsServiceImpl userDetailsService,
            JournalisationService journalisationService) {
        this.refreshExpirationMs = refreshExpirationMs;
        this.jwtExpirationMs = jwtExpirationMs;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.userAssembler = userAssembler;
        this.userDetailsService = userDetailsService;
        this.journalisationService = journalisationService;
    }

    @PostMapping("/login/")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException e) {
            journalisationService.journaliser(null, TypeAction.ECHEC_CONNEXION,
                    "Échec de connexion pour l'identifiant « " + request.getUsername() + " » : " + motif(e));
            throw e; // toujours traité par le GlobalExceptionHandler
        }

        Personnel user = (Personnel) authentication.getPrincipal();
        journalisationService.journaliser(user.getId(), TypeAction.CONNEXION, "Connexion de " + Libelles.personne(user));
        String accessToken = jwtUtils.generateJwtToken(user);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        addRefreshCookie(response, rawRefreshToken);
        TokenResponse body = new TokenResponse(accessToken, jwtExpirationMs / 1000);
        body.setUtilisateur(userAssembler.toDto(user));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh/")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = extractRefreshCookie(request);
        if (rawToken == null) {
            throw new ServiceException("RG-SEC10", "Cookie de refresh manquant");
        }
        RefreshTokenService.RotateResult result = refreshTokenService.rotateRefreshToken(rawToken);
        Personnel user = (Personnel) userDetailsService.loadUserByUsername(result.username());
        if (!user.isEnabled()) {
            refreshTokenService.revokeAllForUser(user.getUsername());
            throw new ServiceException("RG-SEC04", "Compte désactivé");
        }
        if (!user.isAccountNonLocked()) {
            refreshTokenService.revokeAllForUser(user.getUsername());
            throw new ServiceException("RG-SEC05", "Compte verrouillé");
        }
        addRefreshCookie(response, result.rawToken());
        return ResponseEntity.ok(new TokenResponse(jwtUtils.generateJwtToken(user), jwtExpirationMs / 1000));
    }

    @PostMapping("/logout/")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = extractRefreshCookie(request);
        if (rawToken != null) {
            refreshTokenService.revokeToken(rawToken);
        }
        clearRefreshCookie(response);
        journalisationService.journaliser(TypeAction.DECONNEXION, "Déconnexion");
        return ResponseEntity.noContent().build();
    }

    private String motif(AuthenticationException e) {
        if (e instanceof DisabledException) return "compte désactivé";
        if (e instanceof LockedException) return "compte verrouillé";
        return "identifiant ou mot de passe incorrect";
    }

    private void addRefreshCookie(HttpServletResponse response, String rawToken) {
        Cookie cookie = new Cookie(COOKIE_NAME, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) (refreshExpirationMs / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}