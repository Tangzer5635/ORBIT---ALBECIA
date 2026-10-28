package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.repositories.TypeMaterielRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.TypeMaterielService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TypeMaterielServiceImpl extends AbstractService<TypeMateriel, TypeMaterielRepository> implements TypeMaterielService {

    /** Codes et libellés français des 4 types de base, en minuscules : un type ajouté ne peut pas les reprendre. */
    private static final Set<String> NOMS_RESERVES = Stream.concat(
                    TypeMateriel.DE_BASE.stream(),
                    Stream.of("unité centrale", "écran", "vidéoprojecteur", "tableau interactif", "TBI", "VP"))
            .map(nom -> nom.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());

    /** @Lazy : MaterielService dépend déjà de TypeMaterielService (dépendance circulaire sinon). */
    private final MaterielService materielService;
    private final JournalisationService journalisationService;

    @Autowired
    public TypeMaterielServiceImpl(TypeMaterielRepository typeMaterielRepository, @Lazy MaterielService materielService,
                                   JournalisationService journalisationService) {
        super(typeMaterielRepository);
        this.materielService = materielService;
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional
    public TypeMateriel create(TypeMateriel type) throws ServiceException {
        String libelle = type.getLibelle();
        if (libelle != null && NOMS_RESERVES.contains(libelle.toLowerCase(Locale.ROOT)))
            throw new ServiceException("« " + libelle + " » est un type de base, il existe déjà");
        if (repository.existsByLibelleIgnoreCase(libelle))
            throw new ServiceException("Le type de matériel « " + libelle + " » existe déjà");
        TypeMateriel cree = super.create(type);
        journalisationService.journaliser(TypeAction.CREATION, "Type de matériel « " + libelle + " » créé");
        return cree;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeMateriel> findAllTries() throws ServiceException {
        return repository.findAllByOrderByLibelleAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public TypeMateriel trouverParLibelle(String libelle) throws ServiceException {
        return repository.findByLibelleIgnoreCase(libelle == null ? "" : libelle.trim())
                .orElseThrow(() -> new ServiceException("Type de matériel inconnu : " + libelle));
    }

    /** Refusé tant qu'un matériel (archivé compris) est de ce type. */
    @Override
    @Transactional
    public void deleteById(Long id) throws ServiceException {
        TypeMateriel type = repository.findById(id)
                .orElseThrow(() -> new ServiceException("Type de matériel " + id + " introuvable"));
        long nombre = materielService.nombreDeType(id);
        if (nombre > 0)
            throw new ServiceException("Le type « " + type.getLibelle() + " » est utilisé par " + nombre
                    + " matériel(s) (archivés compris) : il ne peut pas être supprimé");
        repository.delete(type);
        journalisationService.journaliser(TypeAction.SUPPRESSION, "Type de matériel « " + type.getLibelle() + " » supprimé");
    }
}