package edu.cit.abella.diladila.web;

import java.util.Map;

/** A business-rule validation failure tied to specific form fields (returned as HTTP 400). */
public class FieldValidationException extends RuntimeException {

    private final Map<String, String> fields;

    public FieldValidationException(String field, String message) {
        super(message);
        this.fields = Map.of(field, message);
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
