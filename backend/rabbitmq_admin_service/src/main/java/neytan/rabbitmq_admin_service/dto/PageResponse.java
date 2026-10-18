package neytan.rabbitmq_admin_service.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        boolean first,
        boolean last,
        boolean empty
) {

    public static <T> PageResponse<T> of(
            List<T> content,
            long totalElements,
            int totalPages,
            int number,
            int size) {

        List<T> items = content == null ? List.of() : content;

        return new PageResponse<>(
                items,
                totalElements,
                totalPages,
                number,
                size,
                number <= 0,
                totalPages == 0 || number >= totalPages - 1,
                items.isEmpty());
    }
}
