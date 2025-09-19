package com.braided_beauty.braided_beauty.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<PhoneConstraint, String> {
    private boolean optional;

    @Override public void initialize(PhoneConstraint ann) {this.optional = ann.optional();}

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.trim().isEmpty()) return optional;
        return PhoneNormalizer.toE164(value) != null;
    }
}
