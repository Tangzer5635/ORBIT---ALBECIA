package net.ent.etnc.orbit.services.commons;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Interface générique de service CRUD pour toutes les entités du projet.
 *
 * <p>Toutes les méthodes déclarent {@link ServiceException} pour permettre
 * aux implémentations de lever des exceptions métier sans être contraintes par
 * la hiérarchie des exceptions vérifiées.
 *
 * @param <T>  le type de l'entité
 * @param <ID> le type de l'identifiant (généralement {@code Long})
 */
public interface Service<T, ID> {

    /**
     * Crée et persiste une nouvelle entité après validation Bean Validation.
     *
     * @param entity l'entité à créer
     * @return l'entité persistée (avec ID et version générés)
     * @throws ServiceException si une règle métier est violée
     */
    T create(@Valid T entity) throws ServiceException;

    /**
     * Met à jour une entité existante après validation.
     *
     * @param entity l'entité avec les nouvelles valeurs (l'ID doit être renseigné)
     * @return l'entité mise à jour
     * @throws ServiceException si l'entité n'existe pas ou si une règle métier est violée
     */
    T update(@Valid T entity) throws ServiceException;

    /**
     * Recherche une entité par son identifiant.
     *
     * @param id l'identifiant de l'entité
     * @return l'entité correspondante, ou {@link Optional#empty()} si absente
     * @throws ServiceException en cas d'erreur technique
     */
    Optional<T> findById(ID id) throws ServiceException;

    /**
     * Retourne une page d'entités.
     * Les implémentations peuvent surcharger cette méthode pour adapter
     * le contenu retourné selon le contexte de sécurité (ex: filtrage selon l'authentification).
     *
     * @param pageable paramètres de pagination et tri
     * @return page d'entités
     * @throws ServiceException en cas d'erreur
     */
    Page<T> findAll(Pageable pageable) throws ServiceException;

    /**
     * Supprime une entité par son identifiant.
     *
     * @param id l'identifiant de l'entité à supprimer
     * @throws ServiceException si l'entité est liée à d'autres données non supprimables
     */
    void deleteById(ID id) throws ServiceException;

    /**
     * Supprime une entité par référence.
     *
     * @param entity l'entité à supprimer
     * @throws ServiceException en cas d'erreur
     */
    void delete(T entity) throws ServiceException;

    /**
     * Supprime toutes les entités. À utiliser avec précaution.
     *
     * @throws ServiceException en cas d'erreur
     */
    void deleteAll() throws ServiceException;

    /**
     * Retourne le nombre total d'entités.
     *
     * @return le nombre d'entités persistées
     * @throws ServiceException en cas d'erreur
     */
    long count() throws ServiceException;

    /**
     * Vérifie si une entité avec cet identifiant existe.
     *
     * @param id l'identifiant à tester
     * @return {@code true} si l'entité existe
     * @throws ServiceException en cas d'erreur
     */
    boolean existsById(ID id) throws ServiceException;
}
