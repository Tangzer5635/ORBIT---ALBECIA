package net.ent.etnc.orbit.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.ent.etnc.orbit.security.services.UserDetailsServiceImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT exécuté une seule fois par requête ({@link OncePerRequestFilter}).
 *
 * <p>Lit le header {@code Authorization: Bearer <token>}, valide la signature via {@link JwtUtils},
 * charge l'utilisateur depuis la base, et injecte l'authentification dans le {@link SecurityContextHolder}.
 * Si le token est absent ou invalide, la requête continue sans authentification — Spring Security
 * appliquera ensuite les règles d'accès configurées dans {@code WebSecurityConfig}.
 */
@Component
public class AuthJwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthJwtFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = parseJwt(request);
        if (token != null && jwtUtils.validateJwtToken(token)) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(jwtUtils.getUsernameFromToken(token));
                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (UsernameNotFoundException ignored) {
                // user supprimé : la requête reste non authentifiée
            }
        }
        filterChain.doFilter(request, response);
    }

    /** Extrait le token brut du header {@code Authorization: Bearer <token>}, ou {@code null} si absent. */
    private String parseJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
