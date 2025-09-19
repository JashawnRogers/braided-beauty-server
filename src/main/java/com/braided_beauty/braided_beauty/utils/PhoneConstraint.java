package com.braided_beauty.braided_beauty.utils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface PhoneConstraint {
    String message() default "invalid phone number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean optional() default true; // treat null/blank as acceptable
}
