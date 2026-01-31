package com.braided_beauty.braided_beauty.utils;

import jakarta.validation.Constraint;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface PhoneConstraint {
    String message() default "invalid phone number";
    boolean optional() default true; // treat null/blank as acceptable
}
