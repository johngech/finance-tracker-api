package com.marakicode.financetracker.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

@Schema(description = "Paginated response wrapper")
public record PagedResponse<T>(
    @Schema(description = "List of items on the current page")
    List<T> content,

    @Schema(description = "Current page number (zero-based)", example = "0")
    int page,

    @Schema(description = "Page size", example = "10")
    int size,

    @Schema(description = "Number of items on this page", example = "5")
    int count,

    @Schema(description = "Total number of pages", example = "3")
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