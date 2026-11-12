package crudmanager.domain.error;

/** Registro duplicado (violação de unicidade). */
public class DuplicateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateException(String message) {
        super(message);
    }
}
