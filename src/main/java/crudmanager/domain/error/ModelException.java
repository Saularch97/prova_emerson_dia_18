package crudmanager.domain.error;

/** Falha de acesso a dados (banco inacessível, SQL inesperado). */
public class ModelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ModelException(String message) {
        super(message);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
