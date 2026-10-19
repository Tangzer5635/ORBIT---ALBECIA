package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Journalisation;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.repositories.JournalisationRepository;
import net.ent.etnc.orbit.repositories.specifications.JournalisationSpecifications;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
public class JournalisationServiceImpl implements JournalisationService {

    /** Logger dédié : logback-spring.xml l'envoie dans logs/audit.log (copie de secours du journal). */
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private static final LocalDateTime DEBUT_PAR_DEFAUT = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final JournalisationRepository repository;

    public JournalisationServiceImpl(JournalisationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void journaliser(TypeAction action, String detail) throws ServiceException {
        Long auteurId = idUtilisateurCourant();
        if (auteurId == null) return; // personne de connecté (jeu de données d'Init) : rien à tracer
        journaliser(auteurId, action, detail);
    }

    @Override
    @Transactional
    public void journaliser(Long auteurId, TypeAction action, String detail) throws ServiceException {
        Journalisation journalisation = repository.save(new Journalisation(auteurId, action, majuscule(detail)));
        String ligne = (journalisation.getAuteurId() == null ? "-" : journalisation.getAuteurId())
                + " | " + journalisation.getAction() + " | " + journalisation.getDetail();

        // audit.log n'est écrit qu'une fois l'action validée en base : une action annulée n'y apparaît pas
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    AUDIT.info(ligne);
                }
            });
        } else {
            AUDIT.info(ligne); // hors transaction (ex. test unitaire) : écrit tout de suite
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Journalisation> rechercher(Specification<Journalisation> filtre, LocalDateTime debut, LocalDateTime fin,
                                           Pageable pageable) throws ServiceException {
        LocalDateTime d = debut == null ? DEBUT_PAR_DEFAUT : debut;
        LocalDateTime f = fin == null ? LocalDateTime.now() : fin;
        if (d.isAfter(f)) {
            throw new ServiceException("RG-J01", "La date de début doit être avant la date de fin");
        }
        Specification<Journalisation> periode = JournalisationSpecifications.periode(d, f);
        return repository.findAll(filtre == null ? periode : periode.and(filtre), pageable);
    }

    /** Le détail commence toujours par une majuscule ("matériel SN-1 affecté..." -> "Matériel SN-1 affecté..."). */
    private static String majuscule(String detail) {
        return detail == null || detail.isEmpty() ? detail : Character.toUpperCase(detail.charAt(0)) + detail.substring(1);
    }

    private Long idUtilisateurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Personnel courant ? courant.getId() : null;
    }
}