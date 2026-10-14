package net.ent.etnc.orbit.dtos.responseDTO.commons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Enveloppe générique pour les réponses paginées (endpoints liste).
 * On ne sérialise jamais directement {@link Page}/{@code PageImpl} (JSON instable).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;            // numéro de page courant (0-based)
    private int size;            // taille de page
    private long totalElements;  // total d'éléments
    private int totalPages;      // total de pages
    private boolean first;
    private boolean last;

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return PageResponse.<T>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
