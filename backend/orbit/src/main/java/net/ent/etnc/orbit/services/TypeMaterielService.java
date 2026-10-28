package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.TypeMateriel;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.List;

public interface TypeMaterielService extends Service<TypeMateriel, Long> {

    /** Types ajoutés par l'administrateur, par ordre alphabétique (sans les 4 types de base). */
    List<TypeMateriel> findAllTries() throws ServiceException;

    /** Type ajouté portant ce libellé (casse ignorée), sinon ServiceException. */
    TypeMateriel trouverParLibelle(String libelle) throws ServiceException;
}