package com.marakicode.financetracker.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    int count,
    int totalPages
) {

    /**
     * Creates a {@code PagedResponse} from a Spring Data {@link Page},
     * mapping each entity using the supplied mapper function.
     */
    public static <E, T> PagedResponse<T> fromPage(Page<E> page, Function<E, T> mapper) {
        List<T> content = page.getContent().stream()
                .map(mapper)
                .toList();
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.getTotalPages()
        );
    }
}