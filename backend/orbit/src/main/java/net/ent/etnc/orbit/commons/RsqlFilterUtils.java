package net.ent.etnc.orbit.commons;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.*;
import io.github.perplexhub.rsql.RSQLJPASupport;
import io.github.perplexhub.rsql.RSQLOperators;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Construit une {@link Specification} JPA à partir d'un filtre RSQL (ex. {@code numero=='X';trainId>3}).
 *
 * Le {@code mapping} (sélecteur DTO → chemin entité, ex. {@code trainId} → {@code train.id}) sert à la
 * fois de traduction de noms ET de liste blanche : tout sélecteur absent du mapping est rejeté (400).
 */
@Component
public class RsqlFilterUtils {

    /**
     * @param filter  le filtre RSQL (peut être null/vide → aucun filtrage)
     * @param mapping sélecteur DTO → chemin entité, et liste blanche des champs filtrables
     * @return la Specification correspondante, ou {@code null} si pas de filtre (null-safe pour findAll)
     */
    public <T> Specification<T> build(String filter, Map<String, String> mapping) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        Node root;
        try {
            // les opérateurs de la lib (=ilike=, =like=, =icase=...) : le parseur par défaut ne connaît que ==, =gt=, =in=...
            root = new RSQLParser(RSQLOperators.supportedOperators()).parse(filter);
        } catch (RSQLParserException e) {
            throw new ServiceException("Filtre invalide : " + e.getMessage(), e);
        }
        Set<String> selectors = new HashSet<>();
        root.accept(new SelectorCollector(), selectors);
        selectors.stream()
                .filter(selector -> !mapping.containsKey(selector))
                .findFirst()
                .ifPresent(selector -> {
                    throw new ServiceException("Champ non filtrable : " + selector);
                });
        return RSQLJPASupport.toSpecification(filter, mapping);
    }

    /** Collecte les sélecteurs (champs) référencés par le filtre, pour la validation whitelist. */
    private static final class SelectorCollector implements RSQLVisitor<Void, Set<String>> {

        @Override
        public Void visit(AndNode node, Set<String> selectors) {
            node.getChildren().forEach(child -> child.accept(this, selectors));
            return null;
        }

        @Override
        public Void visit(OrNode node, Set<String> selectors) {
            node.getChildren().forEach(child -> child.accept(this, selectors));
            return null;
        }

        @Override
        public Void visit(ComparisonNode node, Set<String> selectors) {
            selectors.add(node.getSelector());
            return null;
        }
    }
}