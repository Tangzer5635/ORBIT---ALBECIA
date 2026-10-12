package net.ent.etnc.orbit.controllers.handlers;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.orbit.services.commons.NotFoundException;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.ErrorResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions HTTP pour toute l'application.
 *
 * <p>Stratégie de mapping :
 * <ul>
 *   <li>{@link AccessDeniedException} → 401 si non authentifié, 403 si authentifié sans les droits requis.</li>
 *   <li>{@link NotFoundException} → 404, {@link ServiceException} → 400.</li>
 *   <li>Exceptions Spring MVC ({@link ErrorResponse}) → leur propre statut.</li>
 *   <li>{@link Exception} → 500 Internal Server Error.</li>
 * </ul>
 *
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Login refusé (mauvais identifiants, compte inactif) ou user supprimé → 401. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants invalides");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié.");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès interdit : droits insuffisants.");
    }

    /** Validation des DTO (@Valid dans les controllers) → 400 avec le détail par champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        f -> String.valueOf(f.getDefaultMessage()), (a, b) -> a));
        return ResponseEntity.badRequest().body(errors);
    }

    /** Validation des entités dans les services → 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /** Règle métier violée → 400. */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<String> handleServiceException(ServiceException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /** Ressource introuvable → 404 (plus spécifique que ServiceException, donc prioritaire). */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    /** Paramètre ou id du mauvais type (ex. /materiels/abc/) → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body("Paramètre invalide : " + e.getName());
    }

    /** Corps JSON absent ou mal formé → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body("Corps de requête invalide");
    }

    /**
     * Erreurs inattendues : on logge, mais on n'expose pas le détail au client.
     * Les exceptions de Spring MVC qui portent leur propre statut (405 méthode non autorisée,
     * 404 route inconnue, 400 paramètre manquant, 415…) gardent ce statut au lieu de devenir des 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse.getBody().getDetail());
        }
        log.error("Erreur inattendue", e);
        return ResponseEntity.internalServerError().body("Erreur interne");
    }

    /** Compte désactivé → 403. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<String> handleDisabled(DisabledException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Compte désactivé");
    }

    /** Compte verrouillé → 423. */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<String> handleLocked(LockedException e) {
        return ResponseEntity.status(HttpStatus.LOCKED).body("Compte verrouillé");
    }
}