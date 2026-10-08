package net.ent.etnc.orbit.services.commons;

import jakarta.validation.groups.Default;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

/**
 * Implémentation générique de {@link Service} fournissant le comportement CRUD de base.
 *
 * <p>Les services applicatifs étendent cette classe et surchargent les méthodes
 * pour y ajouter les vérifications de règles métier avant de déléguer à {@code super}.
 *
 * @param <T> le type de l'entité, étendant {@link AbstractPersistableWithIdSetter}
 * @param <R> le type du repository, étendant {@link BaseRepository}
 */
@Validated
public class AbstractService<T extends AbstractPersistableWithIdSetter<Long>, R extends BaseRepository<T>> implements Service<T, Long> {

    /** Repository sous-jacent, accessible aux sous-classes pour les requêtes métier. */
    protected final R repository;

    /**
     * @param repository le repository JPA de l'entité gérée
     */
    public AbstractService(R repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public T create(T entity) throws ServiceException {
        try {
            return repository.saveAndFlush(entity);
        } catch (Exception e) {
            throw new ServiceException("Erreur lors de la sauvegarde", e);
        }
    }

    @Override
    @Transactional
    public T update(T entity) throws ServiceException {
        if (entity.getId() == null) {
            throw new ServiceException("L'ID de l'entité ne doit pas être nul pour une mise à jour");
        }
        T existing = repository.findById(entity.getId())
                .orElseThrow(() -> new ServiceException("Entité avec l'ID " + entity.getId() + " introuvable"));
        entity.setVersion(existing.getVersion());

        try {
            return repository.saveAndFlush(entity);
        } catch (Exception e) {
            throw new ServiceException("Erreur lors de la mise à jour", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(Long id) throws ServiceException {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) throws ServiceException {
        return repository.findAll(pageable);
    }

    @Override
    @Transactional
    public void deleteById(Long id) throws ServiceException {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public void delete(T entity) throws ServiceException {
        repository.delete(entity);
    }

    @Override
    @Transactional
    public void deleteAll() throws ServiceException {
        repository.deleteAll();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() throws ServiceException {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) throws ServiceException {
        return repository.existsById(id);
    }
}
