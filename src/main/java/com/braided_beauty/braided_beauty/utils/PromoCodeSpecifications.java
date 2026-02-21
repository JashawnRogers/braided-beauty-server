package com.braided_beauty.braided_beauty.utils;

import com.braided_beauty.braided_beauty.models.PromoCode;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

@NoArgsConstructor
public final class PromoCodeSpecifications {

    public static Specification<PromoCode> codeContainsIgnoreCase(String q) {
        return ((root, query, cb) -> {
            if (q == null || q.trim().isBlank()) return cb.conjunction();

            String like = "%" + q.trim().toUpperCase(Locale.ROOT) + "%";

            return cb.like(cb.upper((root.get("code"))), like);
        });
    }

    public static Specification<PromoCode> hasActive(Boolean active) {
        return ((root, query, cb) -> {
                if (active == null) return cb.conjunction();
                return cb.equal(root.get("active"), active);
            }
        );
    }
}
