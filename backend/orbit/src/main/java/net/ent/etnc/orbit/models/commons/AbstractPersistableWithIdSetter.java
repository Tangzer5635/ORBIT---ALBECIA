package net.ent.etnc.orbit.models.commons;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.io.Serializable;

/**
 * Classe de base pour toutes les entités JPA du projet.
 *
 * <p>Étend {@link AbstractPersistable} de Spring Data pour bénéficier de la gestion
 * automatique de l'identifiant, tout en exposant un {@code setId} public (nécessaire
 * pour les assemblers lors des mises à jour) et en ajoutant un champ {@code version}
 * pour le contrôle de concurrence optimiste via {@link Version}.
 *
 * @param <PK> le type de la clé primaire (généralement {@code Long})
 */
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class AbstractPersistableWithIdSetter<PK extends Serializable> extends AbstractPersistable<PK> {

    /**
     * Rend publique la méthode {@code setId} de {@link AbstractPersistable},
     * protégée par défaut, pour permettre aux assemblers de positionner l'ID
     * lors de la reconstruction d'une entité depuis un DTO de mise à jour.
     */
    @Override
    public void setId(PK id) {
        super.setId(id);
    }

    /** Numéro de version pour le contrôle de concurrence optimiste (JPA {@link Version}). */
    @Version
    @Column(name = "version", nullable = false)
    @Getter
    @Setter
    private Long version;
}
