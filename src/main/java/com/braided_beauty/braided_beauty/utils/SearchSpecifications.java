package com.braided_beauty.braided_beauty.utils;

import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

@NoArgsConstructor
public final class SearchSpecifications {

    public static <T> Specification<T> codeContainsIgnoreCase(String q, String fieldName) {
        return ((root, query, cb) -> {
            if (q == null || q.trim().isBlank()) return cb.conjunction();

            String like = "%" + q.trim().toUpperCase(Locale.ROOT) + "%";

            return cb.like(cb.upper((root.get(fieldName))), like);
        });
    }

    public static <T> Specification<T> hasActive(Boolean active, String fieldName) {
        return ((root, query, cb) -> {
                if (active == null) return cb.conjunction();
                return cb.equal(root.get(fieldName), active);
            }
        );
    }
}
