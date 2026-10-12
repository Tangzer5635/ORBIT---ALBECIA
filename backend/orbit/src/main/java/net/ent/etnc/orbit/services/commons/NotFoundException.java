package net.ent.etnc.orbit.services.commons;

/**
 * Ressource introuvable (entité absente en base).
 *
 * <p>Interceptée par {@link net.ent.etnc.orbit.controllers.handlers.GlobalExceptionHandler}
 * qui retourne une réponse HTTP 404 avec le message comme corps.
 */
public class NotFoundException extends ServiceException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
