package net.ent.etnc.orbit.services.commons;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception métier levée par la couche service lors d'une violation de règle métier.
 *
 * <p>Chaque exception peut porter un {@link #code} identifiant la règle métier violée
 * (ex: {@code "RG-U01"}) et expose {@link #causesMessage} pour lister récursivement
 * les messages de la chaîne de causes.
 *
 * <p>Interceptée par {@link net.ent.etnc.certificationcenter.controllers.handlers.GlobalExceptionHandler}
 * qui retourne une réponse HTTP 400 avec le message comme corps.
 */
public class ServiceException extends RuntimeException {

    /** Liste des messages de toutes les causes de l'exception (chaîne récursive). */
    @Getter
    private List<String> causesMessage = new ArrayList<>();

    /** Code identifiant la règle métier violée (ex: {@code "RG-U01"}). Peut être {@code null}. */
    @Getter
    private String code;

    /**
     * Crée une exception avec un message simple (sans code de règle).
     *
     * @param message le message décrivant l'erreur
     */
    public ServiceException(String message) {
        super(message);
        this.causesMessage = List.of(message);
    }

    /**
     * Crée une exception avec un code de règle métier et un message.
     *
     * @param code    l'identifiant de la règle violée (ex: {@code "RG-U01"})
     * @param message le message décrivant l'erreur
     */
    public ServiceException(String code, String message) {
        super(message);
        this.code = code;
        this.causesMessage = List.of(message);
    }

    /**
     * Crée une exception encapsulant une cause technique.
     * Les messages de toute la chaîne de causes sont extraits récursivement.
     *
     * @param message le message de contexte
     * @param cause   la cause technique sous-jacente
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.causesMessage = extractCausesMessages(cause);
    }

    private List<String> extractCausesMessages(Throwable cause) {
        if (cause == null) return new ArrayList<>();
        List<String> messages = extractCausesMessages(cause.getCause());
        messages.add(cause.getMessage());
        return messages;
    }
}
