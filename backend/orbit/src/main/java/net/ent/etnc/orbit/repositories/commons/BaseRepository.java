package net.ent.etnc.orbit.repositories.commons;

import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Interface de base pour tous les repositories du projet.
 *
 * <p>Centralise la contrainte de type (entités étendant {@link AbstractPersistableWithIdSetter})
 * et fixe le type de la clé primaire à {@code Long}.
 * Tous les repositories applicatifs doivent étendre cette interface plutôt que
 * {@link JpaRepository} directement.
 *
 * @param <T> le type de l'entité gérée par ce repository
 */
@NoRepositoryBean
public interface BaseRepository<T extends AbstractPersistableWithIdSetter<Long>> extends JpaRepository<T, Long> {
}
