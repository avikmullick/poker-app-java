package com.sap.ase.poker.model;

public class InactivePlayerException extends RuntimeException {
    private static final long serialVersionUID = 5095496162899845835L;

    public InactivePlayerException(String message) {
        super(message);
    }
}
