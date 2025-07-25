package com.braided_beauty.braided_beauty.exceptions;

public class DuplicateEntityException extends RuntimeException {
    public DuplicateEntityException(String message){
        super(message);
    }
}
