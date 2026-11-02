package com.lendshelf.domain;

public enum CopyStatus {
    AVAILABLE("On shelf"),
    ON_LOAN("On loan"),
    ON_HOLD("On hold");

    private final String label;

    CopyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
