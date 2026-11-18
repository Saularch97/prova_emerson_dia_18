package crudmanager.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> errors;

    public ValidationException(Map<String, String> errors) {
        super(errors.toString());
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
